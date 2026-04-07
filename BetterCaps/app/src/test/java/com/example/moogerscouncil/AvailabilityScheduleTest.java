/*
 * AvailabilityScheduleTest.java
 * Role: Unit tests for AvailabilitySchedule — slot grouping, date queries,
 *       available-only filtering, and empty-list edge cases.
 *
 * Part of the BetterCAPS counseling platform.
 */
package com.example.moogerscouncil;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link AvailabilitySchedule}.
 * These tests run on the JVM without an Android runtime or Firestore connection.
 */
public class AvailabilityScheduleTest {

    private static final String COUNSELOR_ID = "counselor-uid-001";

    /** Helper to build a TimeSlot with given values. */
    private TimeSlot makeSlot(String date, String time, boolean available) {
        TimeSlot slot = new TimeSlot();
        slot.setId("slot-" + date + "-" + time);
        slot.setCounselorId(COUNSELOR_ID);
        slot.setDate(date);
        slot.setTime(time);
        slot.setAvailable(available);
        return slot;
    }

    /**
     * {@link AvailabilitySchedule#fromSlots} must include only available slots
     * and exclude unavailable ones from the resulting schedule.
     */
    @Test
    public void testFromSlotsFiltersUnavailable() {
        List<TimeSlot> slots = new ArrayList<>();
        slots.add(makeSlot("2026-04-10", "09:00", true));
        slots.add(makeSlot("2026-04-10", "10:00", false)); // unavailable — should be excluded
        slots.add(makeSlot("2026-04-11", "14:00", true));

        AvailabilitySchedule schedule = AvailabilitySchedule.fromSlots(COUNSELOR_ID, slots);

        // Only two available slots should be in the schedule
        assertEquals(1, schedule.getSlotsForDate("2026-04-10").size());
        assertEquals("09:00", schedule.getSlotsForDate("2026-04-10").get(0).getTime());
        assertEquals(1, schedule.getSlotsForDate("2026-04-11").size());
    }

    /**
     * {@link AvailabilitySchedule#getSlotsForDate} must return only slots
     * matching the requested date, not slots from other dates.
     */
    @Test
    public void testGetSlotsForDateReturnsCorrectSubset() {
        List<TimeSlot> slots = new ArrayList<>();
        slots.add(makeSlot("2026-04-10", "09:00", true));
        slots.add(makeSlot("2026-04-10", "11:00", true));
        slots.add(makeSlot("2026-04-11", "14:00", true));

        AvailabilitySchedule schedule = AvailabilitySchedule.fromSlots(COUNSELOR_ID, slots);

        List<TimeSlot> aprilTenSlots = schedule.getSlotsForDate("2026-04-10");
        assertEquals(2, aprilTenSlots.size());

        List<TimeSlot> aprilElevenSlots = schedule.getSlotsForDate("2026-04-11");
        assertEquals(1, aprilElevenSlots.size());
    }

    /**
     * Requesting slots for a date that has no entries must return an empty list,
     * not null — callers should never need to null-check the return value.
     */
    @Test
    public void testGetSlotsForDateReturnsEmptyForNoSlots() {
        AvailabilitySchedule schedule = AvailabilitySchedule.fromSlots(
                COUNSELOR_ID, new ArrayList<>());

        List<TimeSlot> result = schedule.getSlotsForDate("2026-04-15");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /**
     * {@link AvailabilitySchedule#getDatesWithAvailability()} must return the
     * exact set of dates that have at least one available slot.
     */
    @Test
    public void testGetDatesWithAvailability() {
        List<TimeSlot> slots = new ArrayList<>();
        slots.add(makeSlot("2026-04-10", "09:00", true));
        slots.add(makeSlot("2026-04-12", "15:00", true));
        slots.add(makeSlot("2026-04-13", "10:00", false)); // excluded

        AvailabilitySchedule schedule = AvailabilitySchedule.fromSlots(COUNSELOR_ID, slots);

        assertTrue(schedule.getDatesWithAvailability().contains("2026-04-10"));
        assertTrue(schedule.getDatesWithAvailability().contains("2026-04-12"));
        assertFalse(schedule.getDatesWithAvailability().contains("2026-04-13"));
        assertEquals(2, schedule.getDatesWithAvailability().size());
    }

    /**
     * Building a schedule from an empty slot list must produce a schedule with
     * no dates and no slots — no exceptions thrown.
     */
    @Test
    public void testEmptySlotListProducesEmptySchedule() {
        AvailabilitySchedule schedule = AvailabilitySchedule.fromSlots(
                COUNSELOR_ID, new ArrayList<>());

        assertTrue(schedule.getDatesWithAvailability().isEmpty());
        assertEquals(COUNSELOR_ID, schedule.getCounselorId());
    }
}
