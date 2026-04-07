/*
 * AppointmentTest.java
 * Role: Unit tests for the Appointment model class.
 *       Verifies constructor behaviour, Firestore no-arg constructor,
 *       status field correctness, and setter overrides.
 *
 * Part of the BetterCAPS counseling platform.
 */
package com.example.moogerscouncil;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Unit tests for {@link Appointment}.
 * Runs on the JVM without an Android runtime.
 */
public class AppointmentTest {

    /**
     * The no-arg constructor required by Firestore deserialization must not throw
     * and must leave all fields null.
     */
    @Test
    public void testEmptyConstructorForFirestore() {
        Appointment apt = new Appointment();
        assertNull(apt.getId());
        assertNull(apt.getStudentId());
        assertNull(apt.getCounselorId());
        assertNull(apt.getSlotId());
        assertNull(apt.getDate());
        assertNull(apt.getTime());
        assertNull(apt.getStatus());
    }

    /**
     * All setters must correctly update their respective fields.
     */
    @Test
    public void testSettersOverrideFields() {
        Appointment apt = new Appointment();
        apt.setId("appt-001");
        apt.setStudentId("student-uid");
        apt.setCounselorId("counselor-uid");
        apt.setSlotId("slot-uid");
        apt.setDate("2026-04-10");
        apt.setTime("09:00");
        apt.setStatus("CONFIRMED");

        assertEquals("appt-001", apt.getId());
        assertEquals("student-uid", apt.getStudentId());
        assertEquals("counselor-uid", apt.getCounselorId());
        assertEquals("slot-uid", apt.getSlotId());
        assertEquals("2026-04-10", apt.getDate());
        assertEquals("09:00", apt.getTime());
        assertEquals("CONFIRMED", apt.getStatus());
    }

    /**
     * Status field must accept and return each of the three valid status strings
     * without transformation.
     */
    @Test
    public void testStatusValues() {
        Appointment apt = new Appointment();

        apt.setStatus("CONFIRMED");
        assertEquals("CONFIRMED", apt.getStatus());

        apt.setStatus("COMPLETED");
        assertEquals("COMPLETED", apt.getStatus());

        apt.setStatus("CANCELLED");
        assertEquals("CANCELLED", apt.getStatus());

        apt.setStatus("NO_SHOW");
        assertEquals("NO_SHOW", apt.getStatus());
    }

    /**
     * Calling setStatus multiple times must update the field to the last value.
     */
    @Test
    public void testStatusTransition() {
        Appointment apt = new Appointment();
        apt.setStatus("CONFIRMED");
        apt.setStatus("COMPLETED");
        assertEquals("COMPLETED", apt.getStatus());
    }
}
