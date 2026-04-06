package com.example.moogerscouncil;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for TimeSlot booking logic.
 * Written BEFORE implementation (TDD Red phase).
 */
public class TimeSlotTest {

    @Test
    public void testNewSlotIsAvailable() {
        TimeSlot slot = new TimeSlot();
        slot.setAvailable(true);
        assertTrue(slot.isAvailable());
    }

    @Test
    public void testBookingMakesSlotUnavailable() {
        TimeSlot slot = new TimeSlot();
        slot.setAvailable(true);
        slot.book();
        assertFalse(slot.isAvailable());
    }

    @Test(expected = IllegalStateException.class)
    public void testCannotBookAlreadyBookedSlot() {
        TimeSlot slot = new TimeSlot();
        slot.setAvailable(false);
        slot.book(); // Should throw IllegalStateException
    }
}