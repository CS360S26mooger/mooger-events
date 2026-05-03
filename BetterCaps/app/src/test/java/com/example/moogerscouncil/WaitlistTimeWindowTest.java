package com.example.moogerscouncil;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the time-window building logic shared by
 * WaitlistRequestActivity.buildEndOptions() and
 * CounselorWaitlistActivity.buildTimesInRange().
 */
public class WaitlistTimeWindowTest {

    // ------------------------------------------------------------------
    // buildTimesInRange (CounselorWaitlistActivity)
    // ------------------------------------------------------------------

    @Test
    public void buildTimesInRange_producesCorrectSteps() {
        List<String> times = CounselorWaitlistActivity.buildTimesInRange("09:00", "11:00");
        assertEquals(4, times.size());
        assertEquals("09:00", times.get(0));
        assertEquals("09:30", times.get(1));
        assertEquals("10:00", times.get(2));
        assertEquals("10:30", times.get(3));
    }

    @Test
    public void buildTimesInRange_endTimeIsExclusive() {
        List<String> times = CounselorWaitlistActivity.buildTimesInRange("09:00", "09:30");
        assertEquals(1, times.size());
        assertEquals("09:00", times.get(0));
        assertFalse(times.contains("09:30"));
    }

    @Test
    public void buildTimesInRange_singleSlotWindow() {
        List<String> times = CounselorWaitlistActivity.buildTimesInRange("14:00", "14:30");
        assertEquals(1, times.size());
        assertEquals("14:00", times.get(0));
    }

    @Test
    public void buildTimesInRange_startEqualsEnd_returnsEmpty() {
        List<String> times = CounselorWaitlistActivity.buildTimesInRange("10:00", "10:00");
        assertTrue(times.isEmpty());
    }

    @Test
    public void buildTimesInRange_nullInputs_returnsEmpty() {
        assertTrue(CounselorWaitlistActivity.buildTimesInRange(null, "11:00").isEmpty());
        assertTrue(CounselorWaitlistActivity.buildTimesInRange("09:00", null).isEmpty());
    }

    @Test
    public void buildTimesInRange_fullDay_containsOnlyHalfHourSteps() {
        List<String> times = CounselorWaitlistActivity.buildTimesInRange("08:00", "17:00");
        assertEquals(18, times.size());
        for (int i = 0; i < times.size() - 1; i++) {
            int curr = CounselorWaitlistActivity.toMinutes(times.get(i));
            int next = CounselorWaitlistActivity.toMinutes(times.get(i + 1));
            assertEquals(30, next - curr);
        }
    }

    // ------------------------------------------------------------------
    // buildEndOptions (WaitlistRequestActivity) — requires Activity instance
    // Tested via a thin helper shim that mirrors the same algorithm.
    // ------------------------------------------------------------------

    @Test
    public void endOptionsAlwaysAtLeast30MinAfterStart() {
        String[] starts = {"08:00", "09:00", "10:30", "15:30", "16:00", "16:30"};
        for (String start : starts) {
            List<String> ends = buildEndOptionsFor(start);
            for (String end : ends) {
                int gap = CounselorWaitlistActivity.toMinutes(end)
                        - CounselorWaitlistActivity.toMinutes(start);
                assertTrue("gap < 30 for start=" + start + " end=" + end, gap >= 30);
            }
        }
    }

    @Test
    public void endOptionsFor0800_excludes0800and0830() {
        List<String> ends = buildEndOptionsFor("08:00");
        assertFalse(ends.contains("08:00"));
        assertFalse(ends.contains("08:30"));
        assertTrue(ends.contains("09:00"));
    }

    @Test
    public void endOptionsFor1630_hasOnly1700() {
        List<String> ends = buildEndOptionsFor("16:30");
        assertEquals(1, ends.size());
        assertEquals("17:00", ends.get(0));
    }

    @Test
    public void endOptionsFor1700_isEmpty_noPossibleEnd() {
        // 17:00 is the last entry in ALL_TIMES, so nothing is 30 min after it.
        List<String> ends = buildEndOptionsFor("17:00");
        assertTrue(ends.isEmpty());
    }

    // ------------------------------------------------------------------
    // toMinutes (shared helper)
    // ------------------------------------------------------------------

    @Test
    public void toMinutes_correctForMidnight() {
        assertEquals(0, CounselorWaitlistActivity.toMinutes("00:00"));
    }

    @Test
    public void toMinutes_correctForNoon() {
        assertEquals(720, CounselorWaitlistActivity.toMinutes("12:00"));
    }

    @Test
    public void toMinutes_correctForHalfPast() {
        assertEquals(570, CounselorWaitlistActivity.toMinutes("09:30"));
    }

    // ------------------------------------------------------------------
    // Helper — mirrors WaitlistRequestActivity.buildEndOptions() algorithm
    // ------------------------------------------------------------------

    private static final String[] ALL_TIMES = {
            "08:00", "08:30", "09:00", "09:30", "10:00", "10:30",
            "11:00", "11:30", "12:00", "12:30", "13:00", "13:30",
            "14:00", "14:30", "15:00", "15:30", "16:00", "16:30", "17:00"
    };

    private static List<String> buildEndOptionsFor(String startTime) {
        java.util.List<String> opts = new java.util.ArrayList<>();
        int startMins = CounselorWaitlistActivity.toMinutes(startTime);
        for (String t : ALL_TIMES) {
            if (CounselorWaitlistActivity.toMinutes(t) - startMins >= 30) {
                opts.add(t);
            }
        }
        return opts;
    }
}
