/*
 * FeedbackRepository.java
 * Role: Single point of access for all Firestore operations on the 'feedback'
 *       collection. Enforces anonymity by only accepting FeedbackService objects
 *       (which have no studentId field) for writes.
 *
 * Design pattern: Repository pattern, consistent with UserRepository,
 *                 CounselorRepository, AppointmentRepository,
 *                 and AvailabilityRepository.
 * Part of the BetterCAPS counseling platform.
 */
package com.example.moogerscouncil;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Repository for the Firestore 'feedback' collection.
 * All reads and writes to feedback documents flow through this class.
 *
 * <p>Key design constraint: feedback documents never contain a studentId.
 * This repository enforces anonymity by only accepting {@link FeedbackService}
 * objects (which have no studentId field) for writes.</p>
 *
 * <p>Follows the Repository design pattern consistent with
 * {@link UserRepository}, {@link CounselorRepository},
 * {@link AppointmentRepository}, and {@link AvailabilityRepository}.</p>
 */
public class FeedbackRepository {

    private final CollectionReference feedbackCollection;

    /** Initialises the repository with a reference to the 'feedback' collection. */
    public FeedbackRepository() {
        this.feedbackCollection = FirebaseFirestore.getInstance()
                .collection("feedback");
    }

    // -------------------------------------------------------------------------
    // Callback interfaces
    // -------------------------------------------------------------------------

    /**
     * Callback for feedback submission operations.
     */
    public interface OnFeedbackSubmittedCallback {
        /** Called when the feedback was successfully persisted. */
        void onSuccess();

        /**
         * Called when the submission failed.
         *
         * @param e The exception describing the failure.
         */
        void onFailure(Exception e);
    }

    /**
     * Callback for checking whether feedback already exists for an appointment.
     */
    public interface OnFeedbackCheckCallback {
        /**
         * Called with the result of the existence check.
         *
         * @param feedbackExists True if at least one feedback document exists
         *                       for the queried appointment.
         */
        void onResult(boolean feedbackExists);
    }

    // -------------------------------------------------------------------------
    // Write operations
    // -------------------------------------------------------------------------

    /**
     * Submits anonymous feedback for a completed appointment.
     * The {@link FeedbackService} object must not contain a studentId —
     * anonymity is enforced at the model level.
     *
     * @param feedback The feedback to persist.
     * @param callback Success/failure callback.
     */
    public void submitFeedback(FeedbackService feedback,
                               OnFeedbackSubmittedCallback callback) {
        feedbackCollection.add(feedback)
                .addOnSuccessListener(docRef -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    // -------------------------------------------------------------------------
    // Read operations
    // -------------------------------------------------------------------------

    /**
     * Checks whether feedback has already been submitted for a given appointment.
     * Used to determine whether to show the feedback prompt on StudentHomeActivity.
     *
     * <p>Uses {@code limit(1)} because we only need to know if at least one
     * document exists — fetching the full set would be wasteful.</p>
     *
     * <p>On Firestore failure, returns {@code false} (assumes no feedback) so the
     * prompt is shown rather than silently suppressed. Worst case: the student
     * sees the prompt twice.</p>
     *
     * @param appointmentId The appointment to check.
     * @param callback      Receives true if feedback exists, false otherwise.
     */
    public void hasFeedbackForAppointment(String appointmentId,
                                          OnFeedbackCheckCallback callback) {
        feedbackCollection
                .whereEqualTo("appointmentId", appointmentId)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot ->
                        callback.onResult(!querySnapshot.isEmpty()))
                .addOnFailureListener(e ->
                        callback.onResult(false)); // On failure, assume no feedback — don't block UI
    }
}
