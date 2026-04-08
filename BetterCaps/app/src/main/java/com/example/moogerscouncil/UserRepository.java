/*
 * UserRepository.java
 * Role: Single point of access for all Firestore operations on the 'users' collection.
 * Pattern: Repository — Activities depend on this class, never on FirebaseFirestore directly.
 *          This isolates the data layer and makes it testable without the Android runtime.
 *
 * Part of the BetterCAPS counseling platform.
 */
package com.example.moogerscouncil;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Repository for the Firestore 'users' collection.
 * All reads and writes to user documents flow through this class.
 * Activities depend on this class, never on FirebaseFirestore directly.
 *
 * <p>Follows the Repository design pattern to isolate the data layer
 * from the UI layer and keep Activities testable.
 */
public class UserRepository {

    private final CollectionReference usersCollection;
    private final FirebaseAuth auth;

    /**
     * Constructs a UserRepository backed by the live Firestore instance.
     */
    public UserRepository() {
        this.usersCollection = FirebaseFirestore.getInstance().collection("users");
        this.auth = FirebaseAuth.getInstance();
    }

    // --- Callback interfaces ---

    /**
     * Callback for user creation operations.
     */
    public interface OnUserCreatedCallback {
        /** Called when the Firestore document was written successfully. */
        void onSuccess();

        /**
         * Called when the write failed.
         *
         * @param e the exception describing the failure.
         */
        void onFailure(Exception e);
    }

    /**
     * Callback for user fetch operations that return a full {@link Student} object.
     */
    public interface OnUserFetchedCallback {
        /**
         * Called when the document was fetched and deserialized successfully.
         *
         * @param student the deserialized Student object.
         */
        void onSuccess(Student student);

        /**
         * Called when the fetch failed.
         *
         * @param e the exception describing the failure.
         */
        void onFailure(Exception e);
    }

    /**
     * Callback for role-only fetch operations used for post-login routing.
     */
    public interface OnRoleFetchedCallback {
        /**
         * Called when the role field was retrieved successfully.
         *
         * @param role one of the constants from {@link UserRole}.
         */
        void onSuccess(String role);

        /**
         * Called when the fetch failed.
         *
         * @param e the exception describing the failure.
         */
        void onFailure(Exception e);
    }

    // --- Write operations ---

    /**
     * Creates a new user document in Firestore under {@code users/{uid}}.
     * Uses the Student's UID as the document ID so it matches
     * the Firebase Auth UID for easy lookup.
     *
     * @param student  The Student object to persist.
     * @param callback Success/failure callback.
     */
    public void createUser(Student student, OnUserCreatedCallback callback) {
        usersCollection.document(student.getUid())
                .set(student)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    // --- Read operations ---

    /**
     * Fetches the Firestore document for the currently authenticated user
     * and deserializes it into a {@link Student} object.
     *
     * @param callback Receives the Student on success, or an Exception on failure.
     */
    public void getCurrentUser(OnUserFetchedCallback callback) {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) {
            callback.onFailure(new IllegalStateException("No authenticated user"));
            return;
        }
        usersCollection.document(firebaseUser.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Student student = doc.toObject(Student.class);
                        callback.onSuccess(student);
                    } else {
                        callback.onFailure(new IllegalStateException("User document not found"));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Callback for name-only user lookups.
     * On failure, "Unknown" is returned so appointment cards degrade gracefully.
     */
    public interface OnUserNameCallback {
        /**
         * Called with the user's name, or "Unknown" if the document is missing
         * or the lookup fails.
         *
         * @param name The user's display name.
         */
        void onSuccess(String name);
    }

    /**
     * Fetches only the {@code name} field for a given user UID.
     * Used by {@link AppointmentAdapter} to display student names on
     * counselor dashboard appointment cards.
     *
     * <p>On Firestore failure, the callback still receives "Unknown" rather than
     * propagating an error — student name is supplementary display data and should
     * never block rendering an appointment card.</p>
     *
     * @param uid      The Firebase Auth UID of the user to look up.
     * @param callback Receives the name string on success, or "Unknown" on failure.
     */
    public void getUserName(String uid, OnUserNameCallback callback) {
        usersCollection.document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        callback.onSuccess(name != null ? name : "Unknown");
                    } else {
                        callback.onSuccess("Unknown");
                    }
                })
                .addOnFailureListener(e -> callback.onSuccess("Unknown"));
    }

    /**
     * Finds the "real" counselor document (the one with actual profile data like
     * name, bio, specializations) and writes the Auth UID into its {@code uid} field.
     * This ensures that student-side code (CounselorAdapter) can read
     * {@code counselor.getUid()} and pass it to BookingActivity for slot queries.
     *
     * <p>Called on every counselor login so the mapping is always current.
     * Best-effort — failures are logged but do not block login.</p>
     *
     * @param counselorsRef Reference to the counselors collection.
     * @param authUid       The counselor's Firebase Auth UID.
     */
    private void stampUidOnRealCounselorDoc(CollectionReference counselorsRef,
                                             String authUid) {
        counselorsRef.get()
                .addOnSuccessListener(snapshot -> {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                        // Skip the ghost doc (the one whose ID == Auth UID with no real data)
                        if (doc.getId().equals(authUid)) continue;
                        // Found the real counselor doc — stamp the Auth UID onto it
                        String existingUid = doc.getString("uid");
                        if (!authUid.equals(existingUid)) {
                            doc.getReference().update("uid", authUid)
                                    .addOnFailureListener(e ->
                                            android.util.Log.w("UserRepository",
                                                    "uid stamp failed: " + e.getMessage()));
                        }
                        break; // only one real counselor doc expected
                    }
                });
    }

    /**
     * Resolves the role of the currently authenticated user by checking both
     * Firestore collections:
     * <ol>
     *   <li>{@code users/{uid}} — students and admins; role is stored in the {@code role} field.</li>
     *   <li>{@code counselors/{uid}} — counselors provisioned directly in this collection
     *       (no {@code role} field required; presence in the collection implies counselor role).</li>
     * </ol>
     *
     * <p>If the UID is found in neither collection, {@code onFailure} is called with an
     * {@link IllegalArgumentException} so the caller can display a user-readable error.</p>
     *
     * @param callback Receives the role string on success, or an Exception on failure.
     */
    public void getCurrentUserRole(OnRoleFetchedCallback callback) {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) {
            callback.onFailure(new IllegalStateException("No authenticated user"));
            return;
        }
        String uid = firebaseUser.getUid();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference counselorsRef = db.collection("counselors");

        // Step 1: check counselors collection first — a counselor may also have a
        // ghost document at counselors/{authUID} (created by a previous login) OR
        // a real document whose Auth UID differs from the Firestore doc ID.
        // We check both: direct doc lookup AND a query on the uid field.
        counselorsRef.document(uid).get()
                .addOnSuccessListener(counselorDoc -> {
                    if (counselorDoc.exists()) {
                        // Found via direct doc ID lookup (ghost doc or matching doc).
                        // Stamp uid onto the real counselor doc so student-side
                        // slot queries always have the Auth UID available.
                        stampUidOnRealCounselorDoc(counselorsRef, uid);
                        callback.onSuccess(UserRole.COUNSELOR);
                        return;
                    }

                    // Step 2: no doc at counselors/{authUID} — scan for a doc
                    // that already has this uid field (in case ghost doc was cleaned up)
                    counselorsRef.whereEqualTo("uid", uid).limit(1).get()
                            .addOnSuccessListener(uidQuery -> {
                                if (!uidQuery.isEmpty()) {
                                    callback.onSuccess(UserRole.COUNSELOR);
                                    return;
                                }

                                // Step 3: not a counselor — check users collection
                                usersCollection.document(uid).get()
                                        .addOnSuccessListener(userDoc -> {
                                            if (userDoc.exists()) {
                                                String role = userDoc.getString("role");
                                                if (role != null && !role.isEmpty()) {
                                                    callback.onSuccess(role);
                                                } else {
                                                    callback.onFailure(new IllegalArgumentException(
                                                            "Account has no role assigned. "
                                                                    + "Please contact your administrator."));
                                                }
                                            } else {
                                                callback.onFailure(new IllegalArgumentException(
                                                        "Account not found. Please contact your administrator."));
                                            }
                                        })
                                        .addOnFailureListener(callback::onFailure);
                            })
                            .addOnFailureListener(e -> {
                                // uid query failed — fall through to users check
                                usersCollection.document(uid).get()
                                        .addOnSuccessListener(userDoc -> {
                                            if (userDoc.exists()) {
                                                String role = userDoc.getString("role");
                                                callback.onSuccess(role != null ? role : "student");
                                            } else {
                                                callback.onFailure(new IllegalArgumentException(
                                                        "Account not found."));
                                            }
                                        })
                                        .addOnFailureListener(callback::onFailure);
                            });
                })
                .addOnFailureListener(callback::onFailure);
    }
}
