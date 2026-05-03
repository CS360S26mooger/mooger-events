/*
 * AppointmentRepository.java
 * Role: Single point of access for all Firestore operations on the 'appointments'
 *       collection. The critical operation is bookAppointment(), which uses a
 *       Firestore transaction to atomically prevent double-booking.
 *
 * Design pattern: Repository pattern, consistent with UserRepository,
 *                 CounselorRepository, and AvailabilityRepository.
 * Part of the BetterCAPS counseling platform.
 */
package com.example.moogerscouncil;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.Timestamp;

import java.util.Collections;
import java.util.List;

/**
 * Repository for the Firestore 'appointments' collection.
 * All reads and writes to appointment documents flow through this class.
 *
 * <p>The critical method is {@link #bookAppointment}, which uses a Firestore
 * transaction to atomically mark a slot as unavailable and create the appointment
 * document — preventing double-booking under concurrent access.</p>
 */
public class AppointmentRepository {

    private final FirebaseFirestore db;
    private final CollectionReference appointmentsCollection;

    /** Initialises the repository with a reference to the 'appointments' collection. */
    public AppointmentRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.appointmentsCollection = db.collection("appointments");
    }

    // -------------------------------------------------------------------------
    // Callback interfaces
    // -------------------------------------------------------------------------

    /**
     * Three-way callback for the booking transaction.
     * Distinguishes slot-taken races from general failures.
     */
    public interface OnBookingCallback {
        /** Called when the transaction committed successfully. */
        void onSuccess();

        /**
         * Called when the slot was already booked (race condition).
         * The Activity should remove the slot from the UI.
         */
        void onSlotTaken();

        /**
         * Called when the transaction failed for a non-race-condition reason.
         *
         * @param e The exception describing the failure.
         */
        void onFailure(Exception e);
    }

    /**
     * Callback for operations that return a list of appointments.
     */
    public interface OnAppointmentsLoadedCallback {
        /**
         * Called when appointments are successfully fetched.
         *
         * @param appointments The list of {@link Appointment} objects.
         */
        void onSuccess(List<Appointment> appointments);

        /**
         * Called when the fetch fails.
         *
         * @param e The exception describing the failure.
         */
        void onFailure(Exception e);
    }

    /**
     * Callback for appointment status update operations.
     */
    public interface OnStatusUpdateCallback {
        /** Called when the status update succeeds. */
        void onSuccess();

        /**
         * Called when the status update fails.
         *
         * @param e The exception describing the failure.
         */
        void onFailure(Exception e);
    }

    /**
     * Callback for waitlist-triggered booking. Returns the new appointment ID so the
     * caller can pass it to {@link WaitlistRepository#resolveEntry}.
     */
    public interface OnWaitlistBookingCallback {
        /**
         * Called when the booking transaction committed successfully.
         *
         * @param appointmentId The Firestore document ID of the created appointment.
         */
        void onSuccess(String appointmentId);

        /** Called when the slot was already taken (race condition). */
        void onSlotTaken();

        /**
         * Called when the transaction failed for a non-race-condition reason.
         *
         * @param e The exception describing the failure.
         */
        void onFailure(Exception e);
    }

    public interface OnDateCheckCallback {
        void onResult(boolean hasBooking);
        void onFailure(Exception e);
    }

    /**
     * Checks whether a student already has a CONFIRMED appointment on the given date.
     * Uses a two-field query (studentId + date) to avoid needing a composite index,
     * then filters for CONFIRMED status client-side.
     */
    public void hasConfirmedBookingOnDate(String studentId, String date,
                                          OnDateCheckCallback callback) {
        appointmentsCollection
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("date", date)
                .get()
                .addOnSuccessListener(snapshot -> {
                    boolean found = false;
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String status = doc.getString("status");
                        if ("CONFIRMED".equals(status)) {
                            found = true;
                            break;
                        }
                    }
                    callback.onResult(found);
                })
                .addOnFailureListener(callback::onFailure);
    }

    // -------------------------------------------------------------------------
    // Write operations
    // -------------------------------------------------------------------------

    /**
     * Atomically books a time slot for a student.
     *
     * <p>Runs a Firestore transaction that:
     * <ol>
     *   <li>Reads the slot document to verify it is still available.</li>
     *   <li>Sets {@code slot.available = false}.</li>
     *   <li>Creates a new appointment document with status {@code "CONFIRMED"}.</li>
     * </ol>
     * If the slot was already taken, the transaction aborts and
     * {@link OnBookingCallback#onSlotTaken()} is called instead of
     * {@link OnBookingCallback#onFailure(Exception)}.</p>
     *
     * @param studentId   The booking student's UID.
     * @param counselorId The counselor being booked.
     * @param slot        The {@link TimeSlot} to book (must have a valid ID).
     * @param callback    Three-way callback: success, slot-taken, or general failure.
     */
    public void bookAppointment(String studentId, String counselorId,
                                TimeSlot slot, OnBookingCallback callback) {

        DocumentReference slotRef = db.collection("Slots")
                .document(counselorId)
                .collection("slots")
                .document(slot.getId());
        DocumentReference appointmentRef = appointmentsCollection.document();

        db.runTransaction(transaction -> {
            DocumentSnapshot slotSnap = transaction.get(slotRef);
            Boolean available = slotSnap.getBoolean("available");
            if (available == null || !available) {
                throw new RuntimeException("SLOT_TAKEN");
            }

            transaction.update(slotRef, "available", false);

            Appointment appointment = new Appointment();
            appointment.setId(appointmentRef.getId());
            appointment.setStudentId(studentId);
            appointment.setCounselorId(counselorId);
            appointment.setSlotId(slot.getId());
            appointment.setDate(slot.getDate());
            appointment.setTime(slot.getTime());
            appointment.setStatus("CONFIRMED");
            transaction.set(appointmentRef, appointment);

            return null;

        }).addOnSuccessListener(unused -> callback.onSuccess())
          .addOnFailureListener(e -> {
              if (e.getMessage() != null && e.getMessage().contains("SLOT_TAKEN")) {
                  callback.onSlotTaken();
              } else {
                  callback.onFailure(e);
              }
          });
    }

    /**
     * Atomically books a time slot on behalf of a waitlisted student.
     *
     * <p>Identical transaction logic to {@link #bookAppointment} but accepts the
     * student UID directly (no Firebase Auth context required) and returns the
     * generated appointment ID so the caller can call
     * {@link WaitlistRepository#resolveEntry} atomically.</p>
     *
     * @param studentId   The waitlisted student's UID.
     * @param counselorId The counselor being booked.
     * @param slot        The {@link TimeSlot} to book (must have a valid ID).
     * @param callback    Three-way callback: success with appointmentId, slot-taken, or failure.
     */
    public void bookAppointmentForWaitlist(String studentId, String counselorId,
                                           TimeSlot slot, OnWaitlistBookingCallback callback) {
        DocumentReference slotRef = db.collection("Slots")
                .document(counselorId)
                .collection("slots")
                .document(slot.getId());
        DocumentReference appointmentRef = appointmentsCollection.document();

        db.runTransaction(transaction -> {
            DocumentSnapshot slotSnap = transaction.get(slotRef);
            Boolean available = slotSnap.getBoolean("available");
            if (available == null || !available) {
                throw new RuntimeException("SLOT_TAKEN");
            }

            transaction.update(slotRef, "available", false);

            Appointment appointment = new Appointment();
            appointment.setId(appointmentRef.getId());
            appointment.setStudentId(studentId);
            appointment.setCounselorId(counselorId);
            appointment.setSlotId(slot.getId());
            appointment.setDate(slot.getDate());
            appointment.setTime(slot.getTime());
            appointment.setStatus("CONFIRMED");
            transaction.set(appointmentRef, appointment);

            return appointmentRef.getId();

        }).addOnSuccessListener(appointmentId -> callback.onSuccess(appointmentId))
          .addOnFailureListener(e -> {
              if (e.getMessage() != null && e.getMessage().contains("SLOT_TAKEN")) {
                  callback.onSlotTaken();
              } else {
                  callback.onFailure(e);
              }
          });
    }

    /**
     * Updates the status of an existing appointment.
     * Used for marking appointments as COMPLETED, CANCELLED, or NO_SHOW.
     *
     * @param appointmentId The Firestore document ID.
     * @param newStatus     The new status string (e.g. "NO_SHOW", "COMPLETED").
     * @param callback      Success/failure callback.
     */
    public void updateAppointmentStatus(String appointmentId, String newStatus,
                                        OnStatusUpdateCallback callback) {
        appointmentsCollection.document(appointmentId)
                .update("status", newStatus)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Marks an appointment as no-show and creates a pending follow-up flag.
     *
     * @param appointmentId The Firestore appointment document ID.
     * @param callback Success/failure callback.
     */
    public void markNoShowWithFollowUp(String appointmentId,
                                       OnStatusUpdateCallback callback) {
        appointmentsCollection.document(appointmentId)
                .update("status", "NO_SHOW",
                        "noShowFollowUpRequired", true,
                        "noShowFollowUpStatus", "PENDING",
                        "noShowMarkedAt", Timestamp.now())
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Cancels an appointment by setting its status to CANCELLED and restoring
     * the linked slot's availability so another student can book it.
     *
     * <p>The slot restore is best-effort — if the slot document does not exist
     * (e.g. demo data) the appointment is still marked CANCELLED and
     * {@link OnStatusUpdateCallback#onSuccess()} is called.</p>
     *
     * @param appointmentId The Firestore document ID of the appointment.
     * @param counselorId   The counselor's Auth UID (needed to locate the slot path).
     * @param slotId        The Firestore document ID of the linked slot.
     * @param callback      Success/failure callback.
     */
    public void cancelAppointment(String appointmentId, String counselorId, String slotId,
                                  OnStatusUpdateCallback callback) {
        appointmentsCollection.document(appointmentId)
                .update("status", "CANCELLED")
                .addOnSuccessListener(unused -> {
                    if (counselorId != null && slotId != null && !slotId.isEmpty()) {
                        db.collection("Slots")
                                .document(counselorId)
                                .collection("slots")
                                .document(slotId)
                                .update("available", true)
                                .addOnSuccessListener(v -> callback.onSuccess())
                                .addOnFailureListener(e -> callback.onSuccess());
                    } else {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    // -------------------------------------------------------------------------
    // Read operations
    // -------------------------------------------------------------------------

    /**
     * Fetches all appointments for a given counselor, ordered by date ascending.
     * Used by {@link CounselorDashboardActivity} for the tabbed appointment view.
     *
     * @param counselorId The counselor's UID.
     * @param callback    Receives the appointment list on success.
     */
    public void getAppointmentsForCounselor(String counselorId,
                                            OnAppointmentsLoadedCallback callback) {
        appointmentsCollection
                .whereEqualTo("counselorId", counselorId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Appointment> appointments = querySnapshot.toObjects(Appointment.class);
                    for (int i = 0; i < appointments.size(); i++) {
                        appointments.get(i).setId(
                                querySnapshot.getDocuments().get(i).getId());
                    }
                    // Sort client-side — avoids requiring a composite Firestore index
                    Collections.sort(appointments, (a, b) ->
                            String.valueOf(a.getDate()).compareTo(String.valueOf(b.getDate())));
                    callback.onSuccess(appointments);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Subscribes to real-time updates for a counselor's appointments.
     * Firestore pushes changes automatically — no polling needed.
     *
     * @param counselorId The counselor's Auth UID.
     * @param callback    Called on every change (initial load + subsequent mutations).
     * @return A ListenerRegistration — call .remove() in onDestroy to unsubscribe.
     */
    public ListenerRegistration listenForCounselorAppointments(
            String counselorId, OnAppointmentsLoadedCallback callback) {
        return appointmentsCollection
                .whereEqualTo("counselorId", counselorId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        callback.onFailure(error);
                        return;
                    }
                    if (snapshots == null) return;
                    List<Appointment> appointments = new java.util.ArrayList<>();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Appointment a = doc.toObject(Appointment.class);
                        if (a != null) {
                            a.setId(doc.getId());
                            appointments.add(a);
                        }
                    }
                    Collections.sort(appointments, (a, b) ->
                            String.valueOf(a.getDate()).compareTo(String.valueOf(b.getDate())));
                    callback.onSuccess(appointments);
                });
    }

    /**
     * Subscribes to real-time updates for a student's appointments.
     *
     * @param studentId The student's Auth UID.
     * @param callback  Called on every change.
     * @return A ListenerRegistration — call .remove() in onDestroy to unsubscribe.
     */
    public ListenerRegistration listenForStudentAppointments(
            String studentId, OnAppointmentsLoadedCallback callback) {
        return appointmentsCollection
                .whereEqualTo("studentId", studentId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        callback.onFailure(error);
                        return;
                    }
                    if (snapshots == null) return;
                    List<Appointment> appointments = new java.util.ArrayList<>();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Appointment a = doc.toObject(Appointment.class);
                        if (a != null) {
                            a.setId(doc.getId());
                            appointments.add(a);
                        }
                    }
                    Collections.sort(appointments, (a, b) ->
                            String.valueOf(a.getDate()).compareTo(String.valueOf(b.getDate())));
                    callback.onSuccess(appointments);
                });
    }

    /**
     * Fetches all appointments for a given student, ordered by date ascending.
     * Used by {@link HistoryActivity}.
     *
     * @param studentId The student's UID.
     * @param callback  Receives the appointment list on success.
     */
    public void getAppointmentsForStudent(String studentId,
                                          OnAppointmentsLoadedCallback callback) {
        appointmentsCollection
                .whereEqualTo("studentId", studentId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Appointment> appointments = querySnapshot.toObjects(Appointment.class);
                    for (int i = 0; i < appointments.size(); i++) {
                        appointments.get(i).setId(
                                querySnapshot.getDocuments().get(i).getId());
                    }
                    // Sort client-side — avoids requiring a composite Firestore index
                    Collections.sort(appointments, (a, b) ->
                            String.valueOf(a.getDate()).compareTo(String.valueOf(b.getDate())));
                    callback.onSuccess(appointments);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Fetches all appointments for a student, newest first, for counselor history view.
     */
    public void getAppointmentsForStudentHistory(String studentId,
                                                 OnAppointmentsLoadedCallback callback) {
        appointmentsCollection
                .whereEqualTo("studentId", studentId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Appointment> appointments = querySnapshot.toObjects(Appointment.class);
                    for (int i = 0; i < appointments.size(); i++) {
                        appointments.get(i).setId(
                                querySnapshot.getDocuments().get(i).getId());
                    }
                    Collections.sort(appointments, (a, b) ->
                            String.valueOf(b.getDate()).compareTo(String.valueOf(a.getDate())));
                    callback.onSuccess(appointments);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Fetches appointments for a student on a specific date.
     * Used by {@link CalendarActivity} when a date is tapped.
     *
     * @param studentId The student's UID.
     * @param date      Date in "yyyy-MM-dd" format.
     * @param callback  Receives the matching appointments on success.
     */
    public void getAppointmentsForStudentOnDate(String studentId, String date,
                                                OnAppointmentsLoadedCallback callback) {
        appointmentsCollection
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("date", date)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Appointment> appointments = querySnapshot.toObjects(Appointment.class);
                    for (int i = 0; i < appointments.size(); i++) {
                        appointments.get(i).setId(
                                querySnapshot.getDocuments().get(i).getId());
                    }
                    callback.onSuccess(appointments);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Fetches completed appointments for a student.
     * Used by {@link StudentHomeActivity} to find appointments that need
     * a feedback prompt. The caller is responsible for filtering out those
     * that already have feedback submitted (via {@link FeedbackRepository}).
     *
     * <p>This two-step approach (fetch COMPLETED, then check feedback per-appointment)
     * is necessary because Firestore does not support cross-collection joins.</p>
     *
     * @param studentId The student's UID.
     * @param callback  Receives the list of COMPLETED appointments on success.
     */
    /**
     * Fetches completed appointments for a student that don't yet have
     * feedback submitted. Filters by status in memory after a single
     * studentId query — avoids requiring a composite Firestore index.
     */
    public void getCompletedAppointmentsNeedingFeedback(String studentId,
                                                        OnAppointmentsLoadedCallback callback) {
        appointmentsCollection
                .whereEqualTo("studentId", studentId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Appointment> all = querySnapshot.toObjects(Appointment.class);
                    for (int i = 0; i < all.size(); i++) {
                        all.get(i).setId(querySnapshot.getDocuments().get(i).getId());
                    }
                    // Filter for COMPLETED in memory — no composite index needed
                    List<Appointment> completed = new java.util.ArrayList<>();
                    for (Appointment a : all) {
                        if ("COMPLETED".equals(a.getStatus())) {
                            completed.add(a);
                        }
                    }
                    callback.onSuccess(completed);
                })
                .addOnFailureListener(callback::onFailure);
    }
}
