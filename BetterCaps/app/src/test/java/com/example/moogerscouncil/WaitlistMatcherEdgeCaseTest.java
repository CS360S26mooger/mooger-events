package com.example.moogerscouncil;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Edge-case and boundary tests for WaitlistMatcher that complement the
 * primary coverage in WaitlistMatcherTest.
 */
public class WaitlistMatcherEdgeCaseTest {

    // ------------------------------------------------------------------
    // matches() — time boundary precision
    // ------------------------------------------------------------------

    @Test
    public void matches_timeAtExactStart_isIncluded() {
        WaitlistEntry e = entry(Arrays.asList("2026-06-10"), "09:00", "12:00");
        assertTrue(WaitlistMatcher.matches(e, "2026-06-10", "09:00"));
    }

    @Test
    public void matches_timeOneMinuteBeforeEnd_isIncluded() {
        WaitlistEntry e = entry(Arrays.asList("2026-06-10"), "09:00", "12:00");
        assertTrue(WaitlistMatcher.matches(e, "2026-06-10", "11:59"));
    }

    @Test
    public void matches_timeMidRange_isIncluded() {
        WaitlistEntry e = entry(Arrays.asList("2026-06-10"), "09:00", "17:00");
        assertTrue(WaitlistMatcher.matches(e, "2026-06-10", "13:30"));
    }

    @Test
    public void matches_slotDateAndTimeNull_returnsFalse() {
        WaitlistEntry e = entry(Arrays.asList("2026-06-10"), "09:00", "12:00");
        assertFalse(WaitlistMatcher.matches(e, null, "10:00"));
        assertFalse(WaitlistMatcher.matches(e, "2026-06-10", null));
    }

    @Test
    public void matches_emptyPreferredDatesList_returnsFalse() {
        WaitlistEntry e = entry(Collections.emptyList(), "09:00", "12:00");
        assertFalse(WaitlistMatcher.matches(e, "2026-06-10", "10:00"));
    }

    @Test
    public void matches_nullStartOrEndTime_returnsFalse() {
        WaitlistEntry e = new WaitlistEntry();
        e.setPreferredDates(Arrays.asList("2026-06-10"));
        e.setPreferredStartTime(null);
        e.setPreferredEndTime("12:00");
        assertFalse(WaitlistMatcher.matches(e, "2026-06-10", "10:00"));

        e.setPreferredStartTime("09:00");
        e.setPreferredEndTime(null);
        assertFalse(WaitlistMatcher.matches(e, "2026-06-10", "10:00"));
    }

    @Test
    public void matches_multiplePreferredDates_matchesAny() {
        WaitlistEntry e = entry(
                Arrays.asList("2026-06-10", "2026-06-12", "2026-06-14"), "10:00", "15:00");
        assertTrue(WaitlistMatcher.matches(e, "2026-06-12", "11:00"));
        assertFalse(WaitlistMatcher.matches(e, "2026-06-11", "11:00"));
    }

    // ------------------------------------------------------------------
    // findFirstMatch() — filtering and skipping behaviour
    // ------------------------------------------------------------------

    @Test
    public void findFirstMatch_skipsOnlyCancelledEntries() {
        WaitlistEntry cancelled = entry(Arrays.asList("2026-06-10"), "09:00", "12:00");
        cancelled.setStatus(WaitlistEntry.STATUS_CANCELLED);

        WaitlistEntry active = entry(Arrays.asList("2026-06-10"), "09:00", "12:00");
        active.setStudentId("should-find");

        List<WaitlistEntry> entries = Arrays.asList(cancelled, active);
        WaitlistEntry result = WaitlistMatcher.findFirstMatch(entries, "2026-06-10", "10:00");

        assertNotNull(result);
        assertEquals("should-find", result.getStudentId());
    }

    @Test
    public void findFirstMatch_noActiveMatch_returnsNull() {
        WaitlistEntry resolved = entry(Arrays.asList("2026-06-10"), "09:00", "12:00");
        resolved.setStatus(WaitlistEntry.STATUS_RESOLVED);

        WaitlistEntry cancelled = entry(Arrays.asList("2026-06-10"), "09:00", "12:00");
        cancelled.setStatus(WaitlistEntry.STATUS_CANCELLED);

        assertNull(WaitlistMatcher.findFirstMatch(
                Arrays.asList(resolved, cancelled), "2026-06-10", "10:00"));
    }

    @Test
    public void findFirstMatch_singleMatchingEntry_returnsIt() {
        WaitlistEntry e = entry(Arrays.asList("2026-06-10"), "09:00", "12:00");
        e.setStudentId("only-one");

        WaitlistEntry result = WaitlistMatcher.findFirstMatch(
                Collections.singletonList(e), "2026-06-10", "11:00");

        assertNotNull(result);
        assertEquals("only-one", result.getStudentId());
    }

    @Test
    public void findFirstMatch_mixedMatchAndNonMatch_returnsMatch() {
        WaitlistEntry nonMatch = entry(Arrays.asList("2026-07-01"), "09:00", "12:00");
        nonMatch.setStudentId("wrong-date");

        WaitlistEntry match = entry(Arrays.asList("2026-06-10"), "09:00", "12:00");
        match.setStudentId("right-date");

        // nonMatch is listed first; match should still be returned
        WaitlistEntry result = WaitlistMatcher.findFirstMatch(
                Arrays.asList(nonMatch, match), "2026-06-10", "10:00");

        assertNotNull(result);
        assertEquals("right-date", result.getStudentId());
    }

    @Test
    public void findFirstMatch_nullList_returnsNull() {
        assertNull(WaitlistMatcher.findFirstMatch(null, "2026-06-10", "10:00"));
    }

    // ------------------------------------------------------------------
    // existingSlotsMatchPreferences() — edge cases
    // ------------------------------------------------------------------

    @Test
    public void existingSlots_emptySlotList_returnsFalse() {
        assertFalse(WaitlistMatcher.existingSlotsMatchPreferences(
                Collections.emptyList(),
                Arrays.asList("2026-06-10"), "09:00", "12:00"));
    }

    @Test
    public void existingSlots_nullPreferredDates_returnsFalse() {
        TimeSlot slot = available("2026-06-10", "10:00");
        assertFalse(WaitlistMatcher.existingSlotsMatchPreferences(
                Collections.singletonList(slot), null, "09:00", "12:00"));
    }

    @Test
    public void existingSlots_slotAtExactStartTime_matches() {
        TimeSlot slot = available("2026-06-10", "09:00");
        assertTrue(WaitlistMatcher.existingSlotsMatchPreferences(
                Collections.singletonList(slot),
                Arrays.asList("2026-06-10"), "09:00", "12:00"));
    }

    @Test
    public void existingSlots_slotAtExactEndTime_doesNotMatch() {
        TimeSlot slot = available("2026-06-10", "12:00");
        assertFalse(WaitlistMatcher.existingSlotsMatchPreferences(
                Collections.singletonList(slot),
                Arrays.asList("2026-06-10"), "09:00", "12:00"));
    }

    @Test
    public void existingSlots_multipleSlots_onlyAvailableMatch_returnsTrue() {
        TimeSlot booked = new TimeSlot();
        booked.setDate("2026-06-10"); booked.setTime("10:00"); booked.setAvailable(false);

        TimeSlot available = available("2026-06-10", "11:00");

        assertTrue(WaitlistMatcher.existingSlotsMatchPreferences(
                Arrays.asList(booked, available),
                Arrays.asList("2026-06-10"), "09:00", "12:00"));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static WaitlistEntry entry(List<String> dates, String start, String end) {
        WaitlistEntry e = new WaitlistEntry();
        e.setPreferredDates(dates);
        e.setPreferredStartTime(start);
        e.setPreferredEndTime(end);
        e.setStatus(WaitlistEntry.STATUS_ACTIVE);
        return e;
    }

    private static TimeSlot available(String date, String time) {
        TimeSlot s = new TimeSlot();
        s.setDate(date); s.setTime(time); s.setAvailable(true);
        return s;
    }
}
