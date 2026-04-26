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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                    List<Counselor> counselors = new java.util.ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc
                            : querySnapshot.getDocuments()) {
                        try {
                            Counselor c = doc.toObject(Counselor.class);
                            if (c != null) {
                                c.setId(doc.getId());
                                counselors.add(c);
                            }
                        } catch (RuntimeException e) {
                            android.util.Log.e("CounselorRepo",
                                    "Skipping doc " + doc.getId() + ": " + e.getMessage());
                        }
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
                        if (counselor == null) {
                            callback.onFailure(new IllegalStateException(
                                    "Counselor deserialization returned null: " + counselorId));
                            return;
                        }
                        counselor.setId(doc.getId());
                        callback.onSuccess(counselor);
                    } else {
                        // Direct doc lookup failed — the counselorId is an Auth UID but the
                        // Firestore document was manually created with a different doc ID.
                        // Fall back to querying by the uid field.
                        counselorsCollection.whereEqualTo("uid", counselorId).limit(1).get()
                                .addOnSuccessListener(query -> {
                                    if (!query.isEmpty()) {
                                        com.google.firebase.firestore.DocumentSnapshot match =
                                                query.getDocuments().get(0);
                                        Counselor counselor = match.toObject(Counselor.class);
                                        if (counselor == null) {
                                            callback.onFailure(new IllegalStateException(
                                                    "Counselor deserialization returned null (uid query): " + counselorId));
                                            return;
                                        }
                                        counselor.setId(match.getId());
                                        callback.onSuccess(counselor);
                                    } else {
                                        callback.onFailure(new IllegalStateException(
                                                "Counselor not found: " + counselorId));
                                    }
                                })
                                .addOnFailureListener(callback::onFailure);
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
     * Creates a new counselor document at {@code counselors/{authUid}}.
     * Called by {@link RegisterActivity} when a counselor account is created so that
     * the Firestore document ID always equals the Firebase Auth UID — no manual
     * Firebase console provisioning needed.
     *
     * <p>Only writes the immutable fields ({@code uid}, {@code name}). All other
     * fields (bio, specializations, etc.) are empty and editable via the profile screen.</p>
     *
     * @param authUid  Firebase Auth UID — used as both the document ID and the {@code uid} field.
     * @param name     The counselor's display name from the registration form.
     * @param callback Success/failure callback.
     */
    public void createCounselorProfile(String authUid, String name, OnUpdateCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("uid", authUid);
        data.put("name", name);
        data.put("bio", "");
        data.put("language", "");
        data.put("gender", "");
        data.put("onLeave", false);
        data.put("onLeaveMessage", "");
        data.put("referralCounselorId", "");
        data.put("specializations", new java.util.ArrayList<>());

        counselorsCollection.document(authUid)
                .set(data)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Updates editable profile fields for a counselor without touching other fields
     * (e.g. {@code name}) that are not exposed in the profile edit screen.
     * Uses {@code update()} so the document must already exist.
     *
     * @param counselorId The Firestore document ID.
     * @param counselor   The updated {@link Counselor} object — only editable fields written.
     * @param callback    Success/failure callback.
     */
    public void updateCounselorProfile(String counselorId,
                                       Counselor counselor,
                                       OnUpdateCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("uid", counselor.getUid());
        updates.put("bio", counselor.getBio());
        updates.put("language", counselor.getLanguage());
        updates.put("gender", counselor.getGender());
        updates.put("specializations", counselor.getSpecializations());
        updates.put("onLeave", counselor.getOnLeave());
        updates.put("onLeaveMessage", counselor.getOnLeaveMessage());
        updates.put("referralCounselorId", counselor.getReferralCounselorId());

        counselorsCollection.document(counselorId)
                .update(updates)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Stamps the Firebase Auth UID onto a counselor's Firestore document.
     * Called by {@link CounselorDashboardActivity} on login so that the student-side
     * slot query (which uses {@code counselor.getUid()}) always has the correct value.
     *
     * @param counselorDocId The Firestore document ID.
     * @param authUid        The Firebase Auth UID of the logged-in counselor.
     * @param callback       Success/failure callback.
     */
    public void stampAuthUid(String counselorDocId, String authUid, OnUpdateCallback callback) {
        counselorsCollection.document(counselorDocId)
                .update("uid", authUid)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Updates only the on-leave status fields for a counselor.
     * Uses {@code update()} instead of {@code set()} to avoid overwriting
     * existing profile fields (bio, specializations, etc.) when only the
     * leave status is changing.
     *
     * @param counselorId The Firestore document ID.
     * @param onLeave     Whether the counselor is on leave.
     * @param leaveMessage Custom message shown to students (nullable).
     * @param referralId  Document ID of the referral counselor (nullable).
     * @param callback    Success/failure callback.
     */
    public void setOnLeaveStatus(String counselorId, boolean onLeave,
                                 String leaveMessage, String referralId,
                                 OnUpdateCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("onLeave", onLeave);
        updates.put("onLeaveMessage", leaveMessage);
        updates.put("referralCounselorId", referralId);

        counselorsCollection.document(counselorId)
                .update(updates)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }
}
