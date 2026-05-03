/*
 * WaitlistRepository.java
 * Role: Single point of access for all Firestore operations on the 'waitlist' collection.
 *       Supports the full Sprint-8 waitlist lifecycle: preference-based join, ordered
 *       counselor queue, atomic resolution, and student history view.
 *
 * Design pattern: Repository pattern.
 * Part of the BetterCAPS counseling platform.
 */
package com.example.moogerscouncil;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository for the Firestore 'waitlist' collection.
 * All reads and writes to waitlist documents flow through this class.
 *
 * <p>The Sprint-8 resolution model is <em>instant</em>: when a counselor creates
 * a slot, the system automatically books it for the first matching waitlisted student
 * (FIFO). There is no intermediate "offer-and-accept" step.</p>
 */
public class WaitlistRepository {

    private final CollectionReference waitlistCollection;

    /** Initialises a repository backed by the 'waitlist' Firestore collection. */
    public WaitlistRepository() {
        waitlistCollection = FirebaseFirestore.getInstance().collection("waitlist");
    }

    // -------------------------------------------------------------------------
    // Callback interfaces
    // -------------------------------------------------------------------------

    /**
     * Three-way callback for join operations that may detect duplicate active entries.
     */
    public interface OnWaitlistActionCallback {
        void onSuccess();
        void onAlreadyWaitlisted();
        void onFailure(Exception e);
    }

    /** Callback for operations that load a list of waitlist entries. */
    public interface OnWaitlistLoadedCallback {
        /**
         * @param entries The loaded waitlist entries.
         */
        void onSuccess(List<WaitlistEntry> entries);
        void onFailure(Exception e);
    }

    /** Callback for operations that return a count. */
    public interface OnWaitlistCountCallback {
        void onSuccess(int count);
        void onFailure(Exception e);
    }

    /** Simple success/failure callback for single-document mutations. */
    public interface OnWaitlistSimpleCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    // -------------------------------------------------------------------------
    // Write operations
    // -------------------------------------------------------------------------

    /**
     * Adds a student to a counselor's waitlist unless an active duplicate exists.
     *
     * <p>A duplicate is defined as an entry with the same studentId, counselorId,
     * and status == ACTIVE. Students are allowed to re-join after cancellation.</p>
     *
     * @param entry    The waitlist entry to persist (should have preference fields set).
     * @param callback Three-way result: success, already-waitlisted, or failure.
     */
    public void joinWaitlist(WaitlistEntry entry, OnWaitlistActionCallback callback) {
        waitlistCollection
                .whereEqualTo("studentId", entry.getStudentId())
                .whereEqualTo("counselorId", entry.getCounselorId())
                .whereEqualTo("status", WaitlistEntry.STATUS_ACTIVE)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        callback.onAlreadyWaitlisted();
                        return;
                    }
                    String id = waitlistCollection.document().getId();
                    entry.setId(id);
                    waitlistCollection.document(id)
                            .set(entry)
                            .addOnSuccessListener(unused -> callback.onSuccess())
                            .addOnFailureListener(callback::onFailure);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Atomically resolves a waitlist entry — marks it RESOLVED and records the
     * slot and appointment that fulfilled it.
     *
     * @param entryId       The waitlist entry to resolve.
     * @param slotId        The slot ID that matched the entry's preferences.
     * @param appointmentId The appointment booked for the student.
     * @param callback      Success/failure callback.
     */
    public void resolveEntry(String entryId, String slotId, String appointmentId,
                             OnWaitlistSimpleCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", WaitlistEntry.STATUS_RESOLVED);
        updates.put("resolvedSlotId", slotId);
        updates.put("resolvedAppointmentId", appointmentId);
        updates.put("resolvedAt", Timestamp.now());

        waitlistCollection.document(entryId)
                .update(updates)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Cancels an active waitlist entry. Called when a student withdraws their request.
     *
     * @param entryId  The Firestore document ID of the entry to cancel.
     * @param callback Success/failure callback.
     */
    public void cancelEntry(String entryId, OnWaitlistSimpleCallback callback) {
        waitlistCollection.document(entryId)
                .update("status", WaitlistEntry.STATUS_CANCELLED)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    // -------------------------------------------------------------------------
    // Read operations — student side
    // -------------------------------------------------------------------------

    /**
     * Loads all waitlist entries (active, resolved, and cancelled) for a student.
     * Used by {@link StudentWaitlistActivity} to display the full request history.
     *
     * @param studentId The student's Firebase Auth UID.
     * @param callback  Receives all entries on success.
     */
    public void getAllWaitlistForStudent(String studentId, OnWaitlistLoadedCallback callback) {
        waitlistCollection
                .whereEqualTo("studentId", studentId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<WaitlistEntry> entries = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                        WaitlistEntry entry = doc.toObject(WaitlistEntry.class);
                        if (entry != null) {
                            entry.setId(doc.getId());
                            entries.add(entry);
                        }
                    }
                    // Sort newest-first for display
                    Collections.sort(entries, (a, b) -> {
                        String ta = a.getRequestedAt() != null ? a.getRequestedAt().toString() : "";
                        String tb = b.getRequestedAt() != null ? b.getRequestedAt().toString() : "";
                        return tb.compareTo(ta);
                    });
                    callback.onSuccess(entries);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Loads only ACTIVE waitlist entries for a student.
     * Used by {@link StudentHomeActivity} to show the waitlist status card count.
     *
     * @param studentId The student's Firebase Auth UID.
     * @param callback  Receives active entries on success.
     */
    public void getActiveWaitlistForStudent(String studentId, OnWaitlistLoadedCallback callback) {
        waitlistCollection
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("status", WaitlistEntry.STATUS_ACTIVE)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<WaitlistEntry> entries = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                        WaitlistEntry entry = doc.toObject(WaitlistEntry.class);
                        if (entry != null) {
                            entry.setId(doc.getId());
                            entries.add(entry);
                        }
                    }
                    callback.onSuccess(entries);
                })
                .addOnFailureListener(callback::onFailure);
    }

    // -------------------------------------------------------------------------
    // Read operations — counselor side
    // -------------------------------------------------------------------------

    /**
     * Loads active waitlist entries for one counselor, sorted by {@code requestedAt}
     * ascending (FIFO order). Used by {@link CounselorWaitlistActivity} and the
     * auto-resolution hook in {@link AvailabilitySetupActivity}.
     *
     * @param counselorId The counselor's Firebase Auth UID.
     * @param callback    Receives FIFO-sorted active entries on success.
     */
    public void getActiveWaitlistForCounselorOrdered(String counselorId,
                                                     OnWaitlistLoadedCallback callback) {
        waitlistCollection
                .whereEqualTo("counselorId", counselorId)
                .whereEqualTo("status", WaitlistEntry.STATUS_ACTIVE)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<WaitlistEntry> entries = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                        WaitlistEntry entry = doc.toObject(WaitlistEntry.class);
                        if (entry != null) {
                            entry.setId(doc.getId());
                            entries.add(entry);
                        }
                    }
                    // Sort FIFO: earliest requestedAt first
                    Collections.sort(entries, (a, b) -> {
                        String ta = a.getRequestedAt() != null ? a.getRequestedAt().toString() : "";
                        String tb = b.getRequestedAt() != null ? b.getRequestedAt().toString() : "";
                        return ta.compareTo(tb);
                    });
                    callback.onSuccess(entries);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Loads active waitlist entries for one counselor (unordered).
     * Kept for backward compatibility with callers that only need the list, not FIFO order.
     *
     * @param counselorId The counselor's Firebase Auth UID.
     * @param callback    Receives active entries on success.
     */
    public void getActiveWaitlistForCounselor(String counselorId,
                                              OnWaitlistLoadedCallback callback) {
        waitlistCollection
                .whereEqualTo("counselorId", counselorId)
                .whereEqualTo("status", WaitlistEntry.STATUS_ACTIVE)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<WaitlistEntry> entries = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                        WaitlistEntry entry = doc.toObject(WaitlistEntry.class);
                        if (entry != null) {
                            entry.setId(doc.getId());
                            entries.add(entry);
                        }
                    }
                    callback.onSuccess(entries);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Returns the count of ACTIVE waitlist entries for a counselor.
     * Used by {@link CounselorDashboardActivity} to populate the stat card.
     *
     * @param counselorId The counselor's Firebase Auth UID.
     * @param callback    Receives the count on success.
     */
    public void getActiveWaitlistCountForCounselor(String counselorId,
                                                   OnWaitlistCountCallback callback) {
        getActiveWaitlistForCounselor(counselorId, new OnWaitlistLoadedCallback() {
            @Override
            public void onSuccess(List<WaitlistEntry> entries) {
                callback.onSuccess(entries.size());
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }
}
