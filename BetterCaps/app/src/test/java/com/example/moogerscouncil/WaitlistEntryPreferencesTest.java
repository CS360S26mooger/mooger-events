/*
 * WaitlistEntryPreferencesTest.java
 * Role: Unit tests for the Sprint-8 preference fields added to WaitlistEntry:
 *       preferredDates, preferredStartTime, preferredEndTime, note, and the
 *       new preference-based constructor.
 *
 * Part of the BetterCAPS counseling platform.
 */
package com.example.moogerscouncil;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Unit tests for the preference fields of {@link WaitlistEntry}. */
public class WaitlistEntryPreferencesTest {

    @Test
    public void preferenceConstructorSetsAllFields() {
        List<String> dates = Arrays.asList("2026-06-10", "2026-06-11");
        WaitlistEntry entry = new WaitlistEntry(
                "student1", "counselor1", dates, "09:00", "12:00",
                "I prefer mornings", "assessment99");

        assertEquals("student1", entry.getStudentId());
        assertEquals("counselor1", entry.getCounselorId());
        assertEquals(dates, entry.getPreferredDates());
        assertEquals("09:00", entry.getPreferredStartTime());
        assertEquals("12:00", entry.getPreferredEndTime());
        assertEquals("I prefer mornings", entry.getNote());
        assertEquals("assessment99", entry.getAssessmentId());
        assertEquals(WaitlistEntry.STATUS_ACTIVE, entry.getStatus());
        assertNotNull(entry.getRequestedAt());
    }

    @Test
    public void preferenceConstructorHandlesNullDates() {
        WaitlistEntry entry = new WaitlistEntry(
                "s", "c", null, "09:00", "12:00", null, null);

        assertNotNull(entry.getPreferredDates());
        assertTrue(entry.getPreferredDates().isEmpty());
    }

    @Test
    public void emptyConstructorReturnsEmptyPreferredDatesList() {
        WaitlistEntry entry = new WaitlistEntry();

        assertNotNull(entry.getPreferredDates());
        assertTrue(entry.getPreferredDates().isEmpty());
    }

    @Test
    public void resolutionFieldsNullByDefault() {
        WaitlistEntry entry = new WaitlistEntry();

        assertNull(entry.getResolvedSlotId());
        assertNull(entry.getResolvedAppointmentId());
        assertNull(entry.getResolvedAt());
    }

    @Test
    public void resolutionSettersWork() {
        WaitlistEntry entry = new WaitlistEntry();
        entry.setResolvedSlotId("slot42");
        entry.setResolvedAppointmentId("appt99");

        assertEquals("slot42", entry.getResolvedSlotId());
        assertEquals("appt99", entry.getResolvedAppointmentId());
    }
}
