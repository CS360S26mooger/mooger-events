package com.example.moogerscouncil;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for Appointment state management.
 * Follows TDD (Green phase).
 */
public class AppointmentTest {

    @Test
    public void testAppointmentInitialization() {
        Appointment apt = new Appointment();
        apt.setStatus("CONFIRMED");
        assertEquals("CONFIRMED", apt.getStatus());
    }

    @Test
    public void testSetAndGetDetails() {
        Appointment apt = new Appointment();
        apt.setStudentId("student123");
        apt.setCounselorId("counselor456");
        apt.setDate("2026-04-10");
        
        assertEquals("student123", apt.getStudentId());
        assertEquals("counselor456", apt.getCounselorId());
        assertEquals("2026-04-10", apt.getDate());
    }
}