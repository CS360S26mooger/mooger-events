package com.example.moogerscouncil;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Unit tests for the bulk slot generation algorithm in {@link GenerateSlotsActivity}. */
public class SlotGenerationTest {

    @Test
    public void simpleWorkday_30MinSlots_noBreaks() {
        // 09:00 - 12:00, 30 min slots, no breaks = 6 slots
        List<String> slots = GenerateSlotsActivity.computeSlotTimesForDate(
                540, 720, 30, Collections.emptyList(), Collections.emptyList(), 0);
        assertEquals(6, slots.size());
        assertEquals("09:00", slots.get(0));
        assertEquals("09:30", slots.get(1));
        assertEquals("11:30", slots.get(5));
    }

    @Test
    public void workday_withOneBreak() {
        // 09:00 - 13:00, 30 min slots, break 11:00-11:30 = 7 slots (8 minus 1 break slot)
        List<int[]> breaks = Arrays.asList(new int[]{660, 690}); // 11:00-11:30
        List<String> slots = GenerateSlotsActivity.computeSlotTimesForDate(
                540, 780, 30, breaks, Collections.emptyList(), 0);
        assertTrue(slots.contains("10:30"));
        assertTrue(slots.contains("11:30"));
        assertTrue(!slots.contains("11:00")); // break slot skipped
    }

    @Test
    public void workday_withMultipleBreaks() {
        // 08:00 - 17:00, 60 min slots, breaks 12:00-13:00 and 15:00-15:30
        List<int[]> breaks = Arrays.asList(
                new int[]{720, 780},  // 12:00-13:00
                new int[]{900, 930}   // 15:00-15:30
        );
        List<String> slots = GenerateSlotsActivity.computeSlotTimesForDate(
                480, 1020, 60, breaks, Collections.emptyList(), 0);
        assertTrue(!slots.contains("12:00")); // lunch break
        assertTrue(slots.contains("13:00")); // after lunch
        assertTrue(!slots.contains("15:00")); // break overlaps this hour slot
        assertTrue(slots.contains("08:00"));
    }

    @Test
    public void slotDurationDoesNotFitRemainingTime() {
        // 09:00 - 09:45, 30 min slots = only 1 slot (09:00-09:30, 09:30 doesn't fit)
        List<String> slots = GenerateSlotsActivity.computeSlotTimesForDate(
                540, 585, 30, Collections.emptyList(), Collections.emptyList(), 0);
        assertEquals(1, slots.size());
        assertEquals("09:00", slots.get(0));
    }

    @Test
    public void emptyRange_producesNoSlots() {
        // start == end
        List<String> slots = GenerateSlotsActivity.computeSlotTimesForDate(
                540, 540, 30, Collections.emptyList(), Collections.emptyList(), 0);
        assertEquals(0, slots.size());
    }

    @Test
    public void existingSlots_skippedWithBuffer() {
        // 09:00 - 11:00, 30 min, existing slot at 10:00, buffer 10 min
        TimeSlot existing = new TimeSlot();
        existing.setTime("10:00");
        List<String> slots = GenerateSlotsActivity.computeSlotTimesForDate(
                540, 660, 30, Collections.emptyList(), Arrays.asList(existing), 10);
        assertTrue(!slots.contains("10:00")); // existing slot
        assertTrue(slots.contains("09:00"));
        assertTrue(slots.contains("09:30"));
    }

    @Test
    public void breakAtStart_skipsToBreakEnd() {
        // 09:00 - 11:00, break 09:00-09:30, 30 min slots
        List<int[]> breaks = Arrays.asList(new int[]{540, 570});
        List<String> slots = GenerateSlotsActivity.computeSlotTimesForDate(
                540, 660, 30, breaks, Collections.emptyList(), 0);
        assertEquals("09:30", slots.get(0)); // starts after break
        assertTrue(!slots.contains("09:00"));
    }

    @Test
    public void fortyFiveMinuteSlots() {
        // 09:00 - 12:00 (180 min), 45 min slots = 4 slots
        List<String> slots = GenerateSlotsActivity.computeSlotTimesForDate(
                540, 720, 45, Collections.emptyList(), Collections.emptyList(), 0);
        assertEquals(4, slots.size());
        assertEquals("09:00", slots.get(0));
        assertEquals("09:45", slots.get(1));
        assertEquals("10:30", slots.get(2));
        assertEquals("11:15", slots.get(3));
    }

    @Test
    public void sixtyMinuteSlots() {
        // 09:00 - 12:00, 60 min slots = 3 slots
        List<String> slots = GenerateSlotsActivity.computeSlotTimesForDate(
                540, 720, 60, Collections.emptyList(), Collections.emptyList(), 0);
        assertEquals(3, slots.size());
        assertEquals("09:00", slots.get(0));
        assertEquals("10:00", slots.get(1));
        assertEquals("11:00", slots.get(2));
    }
}
