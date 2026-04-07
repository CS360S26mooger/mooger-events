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

        DocumentReference slotRef = db.collection("slots").document(slot.getId());
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
                .orderBy("date")
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
                .orderBy("date")
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
}
