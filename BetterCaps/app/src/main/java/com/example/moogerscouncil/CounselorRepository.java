/*
 * CounselorRepository.java
 * Role: Single point of access for all Firestore operations on the 'counselors'
 *       collection. Activities and adapters depend on this class and never call
 *       FirebaseFirestore directly, keeping the data layer testable and swappable.
 *
 * Design pattern: Repository pattern.
 * Part of the BetterCAPS counseling platform.
 */
package com.example.moogerscouncil;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

/**
 * Repository for the Firestore 'counselors' collection.
 * All reads and writes to counselor documents flow through this class.
 * Activities depend on this class, never on {@link FirebaseFirestore} directly.
 *
 * <p>Follows the Repository design pattern to isolate the data layer
 * and maintain consistency with {@link UserRepository} from Sprint 1.</p>
 */
public class CounselorRepository {

    private final CollectionReference counselorsCollection;

    /** Initialises the repository with a reference to the 'counselors' collection. */
    public CounselorRepository() {
        this.counselorsCollection = FirebaseFirestore.getInstance()
                .collection("counselors");
    }

    // -------------------------------------------------------------------------
    // Callback interfaces
    // -------------------------------------------------------------------------

    /**
     * Callback for operations that return a list of counselors.
     */
    public interface OnCounselorsLoadedCallback {
        /**
         * Called when counselors are successfully fetched.
         *
         * @param counselors The full list of counselors from Firestore.
         */
        void onSuccess(List<Counselor> counselors);

        /**
         * Called when the Firestore fetch fails.
         *
         * @param e The exception describing the failure.
         */
        void onFailure(Exception e);
    }

    /**
     * Callback for operations that return a single counselor.
     */
    public interface OnCounselorFetchedCallback {
        /**
         * Called when the counselor document is successfully fetched.
         *
         * @param counselor The fetched {@link Counselor} object.
         */
        void onSuccess(Counselor counselor);

        /**
         * Called when the fetch fails or the document does not exist.
         *
         * @param e The exception describing the failure.
         */
        void onFailure(Exception e);
    }

    /**
     * Callback for write operations (update / set).
     */
    public interface OnUpdateCallback {
        /** Called when the write operation succeeds. */
        void onSuccess();

        /**
         * Called when the write operation fails.
         *
         * @param e The exception describing the failure.
         */
        void onFailure(Exception e);
    }

    // -------------------------------------------------------------------------
    // Read operations
    // -------------------------------------------------------------------------

    /**
     * Fetches all counselor documents from Firestore.
     * The full list is returned — filtering happens client-side
     * in the Activity for responsiveness.
     *
     * @param callback Receives the list on success, or an Exception on failure.
     */
    public void getAllCounselors(OnCounselorsLoadedCallback callback) {
        counselorsCollection.get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Counselor> counselors = querySnapshot.toObjects(Counselor.class);
                    // Attach document IDs (not set by toObjects)
                    for (int i = 0; i < querySnapshot.size(); i++) {
                        counselors.get(i).setId(querySnapshot.getDocuments().get(i).getId());
                    }
                    callback.onSuccess(counselors);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Fetches a single counselor by their Firestore document ID.
     *
     * @param counselorId The Firestore document ID.
     * @param callback    Receives the {@link Counselor} on success, or an Exception on failure.
     */
    public void getCounselor(String counselorId, OnCounselorFetchedCallback callback) {
        counselorsCollection.document(counselorId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Counselor counselor = doc.toObject(Counselor.class);
                        if (counselor != null) {
                            counselor.setId(doc.getId());
                        }
                        callback.onSuccess(counselor);
                    } else {
                        callback.onFailure(
                                new IllegalStateException("Counselor document not found: " + counselorId));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    // -------------------------------------------------------------------------
    // Write operations
    // -------------------------------------------------------------------------

    /**
     * Updates the specialization tags for a counselor.
     *
     * @param counselorId     The Firestore document ID.
     * @param specializations The updated list of specialization tag strings.
     * @param callback        Success/failure callback.
     */
    public void updateSpecializations(String counselorId,
                                      List<String> specializations,
                                      OnUpdateCallback callback) {
        counselorsCollection.document(counselorId)
                .update("specializations", specializations)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Overwrites all profile fields for a counselor.
     * Used by {@code CounselorProfileEditActivity} when saving bio,
     * language, gender, and specializations together.
     *
     * <p>Uses {@code set()} so this also creates the document if it does
     * not yet exist (first-time profile setup).</p>
     *
     * @param counselorId The Firestore document ID.
     * @param counselor   The updated {@link Counselor} object — all fields overwritten.
     * @param callback    Success/failure callback.
     */
    public void updateCounselorProfile(String counselorId,
                                       Counselor counselor,
                                       OnUpdateCallback callback) {
        counselorsCollection.document(counselorId)
                .set(counselor)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }
}
