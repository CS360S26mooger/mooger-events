package com.example.moogerscouncil;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/** Unit tests for {@link CrisisEscalation}. */
public class CrisisEscalationTest {

    @Test
    public void constructorStoresAppointmentCounselorAndStudent() {
        CrisisEscalation escalation = new CrisisEscalation(
                "appointment1",
                "counselor1",
                "student1",
                CrisisEscalation.SEVERITY_HIGH,
                CrisisEscalation.ACTION_SAFETY_PLAN,
                "Notes");

        assertEquals("appointment1", escalation.getAppointmentId());
        assertEquals("counselor1", escalation.getCounselorId());
        assertEquals("student1", escalation.getStudentId());
        assertEquals(CrisisEscalation.SEVERITY_HIGH, escalation.getSeverity());
        assertEquals(CrisisEscalation.ACTION_SAFETY_PLAN, escalation.getActionTaken());
        assertEquals("Notes", escalation.getNotes());
        assertNotNull(escalation.getCreatedAt());
    }

    @Test
    public void defaultResolvedIsFalse() {
        CrisisEscalation escalation = new CrisisEscalation(
                "a", "c", "s",
                CrisisEscalation.SEVERITY_MODERATE,
                CrisisEscalation.ACTION_OTHER,
                "");

        assertFalse(escalation.isResolved());
    }

    @Test
    public void severityAndActionConstantsMatchExpectedStrings() {
        assertEquals("MODERATE", CrisisEscalation.SEVERITY_MODERATE);
        assertEquals("HIGH", CrisisEscalation.SEVERITY_HIGH);
        assertEquals("IMMEDIATE", CrisisEscalation.SEVERITY_IMMEDIATE);
        assertEquals("CALLED_SECURITY", CrisisEscalation.ACTION_CALLED_SECURITY);
        assertEquals("REFERRED_CAPS", CrisisEscalation.ACTION_REFERRED_CAPS);
        assertEquals("SAFETY_PLAN", CrisisEscalation.ACTION_SAFETY_PLAN);
        assertEquals("OTHER", CrisisEscalation.ACTION_OTHER);
    }
}
