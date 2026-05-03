package com.example.moogerscouncil;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Covers the full WaitlistEntry lifecycle: construction, preference fields,
 * status transitions, resolution fields, and legacy backward-compat.
 */
public class WaitlistEntryLifecycleTest {

    // ------------------------------------------------------------------
    // Construction
    // ------------------------------------------------------------------

    @Test
    public void preferenceConstructorSetsAllEightFields() {
        List<String> dates = Arrays.asList("2026-06-10", "2026-06-11");
        WaitlistEntry e = new WaitlistEntry(
                "s1", "c1", dates, "09:00", "12:00", "Please call first", "a1");

        assertEquals("s1", e.getStudentId());
        assertEquals("c1", e.getCounselorId());
        assertEquals(dates, e.getPreferredDates());
        assertEquals("09:00", e.getPreferredStartTime());
        assertEquals("12:00", e.getPreferredEndTime());
        assertEquals("Please call first", e.getNote());
        assertEquals("a1", e.getAssessmentId());
        assertEquals(WaitlistEntry.STATUS_ACTIVE, e.getStatus());
        assertNotNull(e.getRequestedAt());
    }

    @Test
    public void preferenceConstructorWithNullNote_noteIsNull() {
        WaitlistEntry e = new WaitlistEntry("s1", "c1",
                Arrays.asList("2026-06-10"), "09:00", "12:00", null, null);
        assertNull(e.getNote());
    }

    @Test
    public void preferenceConstructorWithNullDates_returnsEmptyList() {
        WaitlistEntry e = new WaitlistEntry("s1", "c1",
                null, "09:00", "12:00", null, null);
        assertNotNull(e.getPreferredDates());
        assertTrue(e.getPreferredDates().isEmpty());
    }

    @Test
    public void legacyConstructorSetsFields_noPreferencesPopulated() {
        WaitlistEntry e = new WaitlistEntry("s1", "c1", "a1", "No slots available");
        assertEquals("s1", e.getStudentId());
        assertEquals("c1", e.getCounselorId());
        assertEquals("a1", e.getAssessmentId());
        assertEquals("No slots available", e.getReason());
        assertEquals(WaitlistEntry.STATUS_ACTIVE, e.getStatus());
        assertNotNull(e.getRequestedAt());
        assertTrue(e.getPreferredDates().isEmpty());
        assertNull(e.getPreferredStartTime());
        assertNull(e.getPreferredEndTime());
        assertNull(e.getNote());
    }

    @Test
    public void emptyConstructor_statusActiveAndDatesEmpty() {
        WaitlistEntry e = new WaitlistEntry();
        assertEquals(WaitlistEntry.STATUS_ACTIVE, e.getStatus());
        assertNotNull(e.getPreferredDates());
        assertTrue(e.getPreferredDates().isEmpty());
    }

    // ------------------------------------------------------------------
    // Status transitions
    // ------------------------------------------------------------------

    @Test
    public void statusTransition_activeToResolved() {
        WaitlistEntry e = new WaitlistEntry();
        e.setStatus(WaitlistEntry.STATUS_RESOLVED);
        assertEquals(WaitlistEntry.STATUS_RESOLVED, e.getStatus());
    }

    @Test
    public void statusTransition_activeToCancelled() {
        WaitlistEntry e = new WaitlistEntry();
        e.setStatus(WaitlistEntry.STATUS_CANCELLED);
        assertEquals(WaitlistEntry.STATUS_CANCELLED, e.getStatus());
    }

    @Test
    public void statusTransition_resolvedBackToActive_allowedByModel() {
        WaitlistEntry e = new WaitlistEntry();
        e.setStatus(WaitlistEntry.STATUS_RESOLVED);
        e.setStatus(WaitlistEntry.STATUS_ACTIVE);
        assertEquals(WaitlistEntry.STATUS_ACTIVE, e.getStatus());
    }

    // ------------------------------------------------------------------
    // Resolution fields
    // ------------------------------------------------------------------

    @Test
    public void resolutionFields_nullByDefault() {
        WaitlistEntry e = new WaitlistEntry();
        assertNull(e.getResolvedSlotId());
        assertNull(e.getResolvedAppointmentId());
        assertNull(e.getResolvedAt());
    }

    @Test
    public void resolutionFields_setAndGetRoundtrip() {
        WaitlistEntry e = new WaitlistEntry();
        e.setResolvedSlotId("slot-123");
        e.setResolvedAppointmentId("appt-456");
        assertEquals("slot-123", e.getResolvedSlotId());
        assertEquals("appt-456", e.getResolvedAppointmentId());
    }

    @Test
    public void idField_setAndGet() {
        WaitlistEntry e = new WaitlistEntry();
        assertNull(e.getId());
        e.setId("doc-id-789");
        assertEquals("doc-id-789", e.getId());
    }

    // ------------------------------------------------------------------
    // Legacy status constants
    // ------------------------------------------------------------------

    @Test
    public void legacyConstantsHaveCorrectStringValues() {
        assertEquals("OFFERED", WaitlistEntry.STATUS_OFFERED);
        assertEquals("BOOKED", WaitlistEntry.STATUS_BOOKED);
        assertEquals("EXPIRED", WaitlistEntry.STATUS_EXPIRED);
    }

    @Test
    public void legacyOfferedAndBooked_areNotActiveOrResolved() {
        // Adapters must handle these separately — not equal to new constants.
        assertTrue(!WaitlistEntry.STATUS_OFFERED.equals(WaitlistEntry.STATUS_ACTIVE));
        assertTrue(!WaitlistEntry.STATUS_OFFERED.equals(WaitlistEntry.STATUS_RESOLVED));
        assertTrue(!WaitlistEntry.STATUS_BOOKED.equals(WaitlistEntry.STATUS_ACTIVE));
        assertTrue(!WaitlistEntry.STATUS_EXPIRED.equals(WaitlistEntry.STATUS_CANCELLED));
    }

    // ------------------------------------------------------------------
    // Preference field setters
    // ------------------------------------------------------------------

    @Test
    public void setPreferredDates_replacesExistingList() {
        WaitlistEntry e = new WaitlistEntry();
        e.setPreferredDates(Arrays.asList("2026-06-01"));
        e.setPreferredDates(Arrays.asList("2026-07-01", "2026-07-02"));
        assertEquals(2, e.getPreferredDates().size());
        assertTrue(e.getPreferredDates().contains("2026-07-01"));
    }

    @Test
    public void setPreferredDates_null_getterReturnsEmptyList() {
        WaitlistEntry e = new WaitlistEntry("s1", "c1",
                Arrays.asList("2026-06-01"), "09:00", "12:00", null, null);
        e.setPreferredDates(null);
        assertNotNull(e.getPreferredDates());
    }

    @Test
    public void timeWindowSetters_roundtrip() {
        WaitlistEntry e = new WaitlistEntry();
        e.setPreferredStartTime("10:00");
        e.setPreferredEndTime("13:30");
        assertEquals("10:00", e.getPreferredStartTime());
        assertEquals("13:30", e.getPreferredEndTime());
    }
}
