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

        // Step 1: check users collection (students / admins)
        usersCollection.document(uid)
                .get()
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
                        return;
                    }

                    // Step 2: not in users — check counselors collection
                    FirebaseFirestore.getInstance().collection("counselors")
                            .document(uid)
                            .get()
                            .addOnSuccessListener(counselorDoc -> {
                                if (counselorDoc.exists()) {
                                    callback.onSuccess(UserRole.COUNSELOR);
                                } else {
                                    callback.onFailure(new IllegalArgumentException(
                                            "Account not found. Please contact your administrator."));
                                }
                            })
                            .addOnFailureListener(callback::onFailure);
                })
                .addOnFailureListener(callback::onFailure);
    }
}
