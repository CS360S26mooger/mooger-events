package com.example.moogerscouncil;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ReturningStudentHelperTest {
    @Test
    public void returnsFalseForNullList() {
        assertFalse(ReturningStudentHelper.isReturningStudent(null, appointment("current", "s1", "2026-05-02", "CONFIRMED")));
    }

    @Test
    public void returnsFalseWhenOnlyCurrentAppointmentExists() {
        Appointment current = appointment("current", "s1", "2026-05-02", "CONFIRMED");
        assertFalse(ReturningStudentHelper.isReturningStudent(Arrays.asList(current), current));
    }

    @Test
    public void returnsTrueForPreviousCompletedAppointmentForSameStudent() {
        Appointment previous = appointment("previous", "s1", "2026-04-02", "COMPLETED");
        Appointment current = appointment("current", "s1", "2026-05-02", "CONFIRMED");
        assertTrue(ReturningStudentHelper.isReturningStudent(Arrays.asList(previous, current), current));
    }

    @Test
    public void returnsFalseForDifferentStudent() {
        Appointment previous = appointment("previous", "s2", "2026-04-02", "COMPLETED");
        Appointment current = appointment("current", "s1", "2026-05-02", "CONFIRMED");
        assertFalse(ReturningStudentHelper.isReturningStudent(Arrays.asList(previous, current), current));
    }

    @Test
    public void returnsFalseForCancelledOrNoShowHistory() {
        Appointment cancelled = appointment("cancelled", "s1", "2026-04-02", "CANCELLED");
        Appointment noShow = appointment("noshow", "s1", "2026-04-03", "NO_SHOW");
        Appointment current = appointment("current", "s1", "2026-05-02", "CONFIRMED");
        assertFalse(ReturningStudentHelper.isReturningStudent(Arrays.asList(cancelled, noShow, current), current));
    }

    @Test
    public void returnsFalseForFutureAppointment() {
        Appointment future = appointment("future", "s1", "2026-06-02", "COMPLETED");
        Appointment current = appointment("current", "s1", "2026-05-02", "CONFIRMED");
        assertFalse(ReturningStudentHelper.isReturningStudent(Arrays.asList(future, current), current));
    }

    private Appointment appointment(String id, String studentId, String date, String status) {
        Appointment appointment = new Appointment();
        appointment.setId(id);
        appointment.setStudentId(studentId);
        appointment.setDate(date);
        appointment.setStatus(status);
        return appointment;
    }
}
