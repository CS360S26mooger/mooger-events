/*
 * Student.java
 * Role: Firestore-mapped data model for a registered student user.
 * Pattern: Plain model (data container). No business logic beyond field access.
 *
 * Maps to the 'users' Firestore collection where role == "student".
 * Part of the BetterCAPS counseling platform.
 */
package com.example.moogerscouncil;

import com.google.firebase.Timestamp;

/**
 * Represents a registered student user in the BetterCAPS system.
 * Maps directly to a document in the Firestore 'users' collection
 * where role == "student".
 *
 * <p>This class follows the Firestore model convention: no-argument
 * constructor, private fields, public getters and setters.
 */
public class Student {

    private String uid;
    private String name;
    private String email;
    private String preferredName;
    private String pronouns;
    private String role;
    private Timestamp createdAt;

    /** Required empty constructor for Firestore deserialization. */
    public Student() {}

    /**
     * Constructs a Student with all required registration fields.
     * Sets {@code role} to {@link UserRole#STUDENT} and {@code createdAt}
     * to the current server timestamp.
     *
     * @param uid           Firebase Auth UID, also the Firestore document ID.
     * @param name          Full legal name.
     * @param email         University email address (must end in @lums.edu.pk).
     * @param preferredName The name the student prefers to be called.
     * @param pronouns      Pronoun preference (e.g. "he/him", "she/her", "they/them").
     */
    public Student(String uid, String name, String email,
                   String preferredName, String pronouns) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.preferredName = preferredName;
        this.pronouns = pronouns;
        this.role = UserRole.STUDENT;
        this.createdAt = Timestamp.now();
    }

    // --- Getters ---

    /**
     * Returns the Firebase Auth UID, which also serves as the Firestore document ID.
     *
     * @return the student's UID.
     */
    public String getUid() { return uid; }

    /**
     * Returns the student's full legal name.
     *
     * @return the full name.
     */
    public String getName() { return name; }

    /**
     * Returns the student's university email address.
     *
     * @return the email.
     */
    public String getEmail() { return email; }

    /**
     * Returns the student's preferred name (what they want to be called).
     *
     * @return the preferred name.
     */
    public String getPreferredName() { return preferredName; }

    /**
     * Returns the student's pronoun preference.
     *
     * @return the pronouns string (e.g. "they/them").
     */
    public String getPronouns() { return pronouns; }

    /**
     * Returns the user's role string, always {@link UserRole#STUDENT} for this class.
     *
     * @return the role string.
     */
    public String getRole() { return role; }

    /**
     * Returns the timestamp when this account was created.
     *
     * @return a Firebase {@link Timestamp} representing account creation time.
     */
    public Timestamp getCreatedAt() { return createdAt; }

    // --- Setters (required by Firestore deserialization) ---

    /**
     * Sets the UID.
     *
     * @param uid Firebase Auth UID.
     */
    public void setUid(String uid) { this.uid = uid; }

    /**
     * Sets the full name.
     *
     * @param name full legal name.
     */
    public void setName(String name) { this.name = name; }

    /**
     * Sets the email address.
     *
     * @param email university email.
     */
    public void setEmail(String email) { this.email = email; }

    /**
     * Sets the preferred name.
     *
     * @param preferredName name to display in the UI.
     */
    public void setPreferredName(String preferredName) { this.preferredName = preferredName; }

    /**
     * Sets the pronouns.
     *
     * @param pronouns pronoun string.
     */
    public void setPronouns(String pronouns) { this.pronouns = pronouns; }

    /**
     * Sets the role field (used during Firestore deserialization).
     *
     * @param role one of the constants in {@link UserRole}.
     */
    public void setRole(String role) { this.role = role; }

    /**
     * Sets the creation timestamp (used during Firestore deserialization).
     *
     * @param createdAt a Firebase Timestamp.
     */
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
