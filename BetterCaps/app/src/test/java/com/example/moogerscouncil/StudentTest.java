/*
 * StudentTest.java
 * Role: Unit tests for the Student model class.
 *       Verifies constructor field assignment, default role, Firestore no-arg
 *       constructor, setter overrides, and non-null timestamp.
 *
 * Part of the BetterCAPS counseling platform.
 */
package com.example.moogerscouncil;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Unit tests for {@link Student}.
 * These tests run on the JVM without an Android runtime (no Firestore connection needed).
 */
public class StudentTest {

    private static final String TEST_UID           = "uid-abc-123";
    private static final String TEST_NAME          = "Ali Hassan";
    private static final String TEST_EMAIL         = "ali.hassan@lums.edu.pk";
    private static final String TEST_PREFERRED     = "Ali";
    private static final String TEST_PRONOUNS      = "he/him";

    /** Creates a Student using the full constructor with standard test data. */
    private Student makeStudent() {
        return new Student(TEST_UID, TEST_NAME, TEST_EMAIL, TEST_PREFERRED, TEST_PRONOUNS);
    }

    /**
     * A Student created with the full constructor must have role set to "student"
     * (as defined in {@link UserRole#STUDENT}).
     */
    @Test
    public void testDefaultRoleIsStudent() {
        Student student = makeStudent();
        assertEquals(UserRole.STUDENT, student.getRole());
    }

    /**
     * The full constructor must set uid, name, email, preferredName, and pronouns
     * exactly as supplied.
     */
    @Test
    public void testConstructorSetsAllFields() {
        Student student = makeStudent();
        assertEquals(TEST_UID,       student.getUid());
        assertEquals(TEST_NAME,      student.getName());
        assertEquals(TEST_EMAIL,     student.getEmail());
        assertEquals(TEST_PREFERRED, student.getPreferredName());
        assertEquals(TEST_PRONOUNS,  student.getPronouns());
    }

    /**
     * The no-arg constructor required by Firestore deserialization must not throw
     * and must leave all fields null.
     */
    @Test
    public void testEmptyConstructorForFirestore() {
        Student student = new Student();
        assertNull(student.getUid());
        assertNull(student.getName());
        assertNull(student.getEmail());
        assertNull(student.getPreferredName());
        assertNull(student.getPronouns());
        assertNull(student.getRole());
        assertNull(student.getCreatedAt());
    }

    /**
     * Setters must override values previously set by the constructor.
     */
    @Test
    public void testSettersOverrideConstructorValues() {
        Student student = makeStudent();
        student.setPreferredName("Hassan");
        assertEquals("Hassan", student.getPreferredName());

        student.setPronouns("they/them");
        assertEquals("they/them", student.getPronouns());

        student.setRole(UserRole.COUNSELOR);
        assertEquals(UserRole.COUNSELOR, student.getRole());
    }

    /**
     * The full constructor must set {@code createdAt} to a non-null timestamp.
     * Note: Timestamp.now() requires Firebase SDK on classpath; this test validates
     * the field is populated, not the exact value.
     */
    @Test
    public void testCreatedAtIsNotNull() {
        Student student = makeStudent();
        assertNotNull(student.getCreatedAt());
    }
}
