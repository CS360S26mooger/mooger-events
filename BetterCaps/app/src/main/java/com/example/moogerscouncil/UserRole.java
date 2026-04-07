/*
 * UserRole.java
 * Role: String constants for user roles stored in Firestore.
 * Pattern: Constants class (prevents enum-to-string conversion overhead).
 *
 * Part of the BetterCAPS counseling platform.
 */
package com.example.moogerscouncil;

/**
 * String constants for user roles stored in Firestore.
 * Used for role-based routing after login and for the
 * 'role' field in the users collection.
 */
public final class UserRole {
    /** Role assigned to students registering for counseling services. */
    public static final String STUDENT = "student";
    /** Role assigned to counselors providing services. */
    public static final String COUNSELOR = "counselor";
    /** Role assigned to platform administrators. */
    public static final String ADMIN = "admin";

    /** Prevent instantiation. */
    private UserRole() {}
}
