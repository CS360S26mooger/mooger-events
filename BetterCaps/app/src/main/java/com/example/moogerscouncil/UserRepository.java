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
     * Fetches only the {@code role} field for the currently authenticated user.
     * Used for post-login routing to the correct home screen.
     *
     * @param callback Receives the role string ("student", "counselor", "admin")
     *                 on success, or an Exception on failure.
     */
    public void getCurrentUserRole(OnRoleFetchedCallback callback) {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) {
            callback.onFailure(new IllegalStateException("No authenticated user"));
            return;
        }
        usersCollection.document(firebaseUser.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String role = doc.getString("role");
                        callback.onSuccess(role);
                    } else {
                        callback.onFailure(new IllegalStateException("User document not found"));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }
}
