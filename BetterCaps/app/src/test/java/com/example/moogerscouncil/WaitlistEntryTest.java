package com.example.moogerscouncil;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/** Unit tests for {@link WaitlistEntry}. */
public class WaitlistEntryTest {

    @Test
    public void emptyConstructorDefaultsStatusToActive() {
        WaitlistEntry entry = new WaitlistEntry();

        assertEquals(WaitlistEntry.STATUS_ACTIVE, entry.getStatus());
    }

    @Test
    public void constructorSetsFields() {
        WaitlistEntry entry = new WaitlistEntry(
                "student1",
                "counselor1",
                "assessment1",
                "No slots available");

        assertEquals("student1", entry.getStudentId());
        assertEquals("counselor1", entry.getCounselorId());
        assertEquals("assessment1", entry.getAssessmentId());
        assertEquals("No slots available", entry.getReason());
        assertEquals(WaitlistEntry.STATUS_ACTIVE, entry.getStatus());
        assertNotNull(entry.getRequestedAt());
    }

    @Test
    public void statusCanMoveThroughNewOperationalStates() {
        WaitlistEntry entry = new WaitlistEntry();

        entry.setStatus(WaitlistEntry.STATUS_RESOLVED);
        assertEquals(WaitlistEntry.STATUS_RESOLVED, entry.getStatus());

        entry.setStatus(WaitlistEntry.STATUS_CANCELLED);
        assertEquals(WaitlistEntry.STATUS_CANCELLED, entry.getStatus());
    }

    @Test
    public void legacyStatusConstantsStillPresent() {
        // Backward compat: adapters map these to RESOLVED/CANCELLED for display.
        assertEquals("OFFERED", WaitlistEntry.STATUS_OFFERED);
        assertEquals("BOOKED", WaitlistEntry.STATUS_BOOKED);
        assertEquals("EXPIRED", WaitlistEntry.STATUS_EXPIRED);
    }
}
