package com.example.moogerscouncil;

import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link FeedbackService}.
 *
 * <p>Key invariant verified: the class has no studentId field, enforcing
 * the anonymity guarantee of US-21 at the schema level.</p>
 */
public class FeedbackServiceTest {

    /**
     * Verifies via reflection that FeedbackService has no field named "studentId".
     * This ensures anonymity cannot be accidentally introduced by adding such a field.
     */
    @Test
    public void testNoStudentIdField() {
        boolean hasStudentId = false;
        for (Field field : FeedbackService.class.getDeclaredFields()) {
            if (field.getName().equalsIgnoreCase("studentId")) {
                hasStudentId = true;
                break;
            }
        }
        assertFalse("FeedbackService must not have a studentId field (anonymity invariant)", hasStudentId);
    }

    /**
     * Verifies that the parameterized constructor correctly sets all fields
     * and that submittedAt is populated (non-null).
     */
    @Test
    public void testConstructorSetsFields() {
        FeedbackService feedback = new FeedbackService("appt123", 4, "Great session!");

        assertEquals("appt123", feedback.getAppointmentId());
        assertEquals(4, feedback.getRating());
        assertEquals("Great session!", feedback.getComment());
        assertNotNull("submittedAt must be set by constructor", feedback.getSubmittedAt());
    }

    /**
     * Verifies that the no-argument constructor creates an instance with
     * null/zero field values, as required for Firestore deserialization.
     */
    @Test
    public void testEmptyConstructorForFirestore() {
        FeedbackService feedback = new FeedbackService();

        assertNull(feedback.getId());
        assertNull(feedback.getAppointmentId());
        assertEquals(0, feedback.getRating());
        assertNull(feedback.getComment());
    }

    /**
     * Verifies that rating can be set to the boundary values 1 and 5.
     */
    @Test
    public void testRatingBounds() {
        FeedbackService feedback = new FeedbackService();

        feedback.setRating(1);
        assertEquals(1, feedback.getRating());

        feedback.setRating(5);
        assertEquals(5, feedback.getRating());
    }

    /**
     * Verifies that a null comment is accepted (feedback text is optional per US-21).
     */
    @Test
    public void testNullCommentAllowed() {
        FeedbackService feedback = new FeedbackService("appt456", 3, null);
        assertNull(feedback.getComment());
    }
}
