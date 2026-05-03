package com.example.moogerscouncil;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Unit tests for {@link BufferTimeValidator}. */
public class BufferTimeValidatorTest {

    @Test
    public void slotTenMinutesAfterExistingSessionIsRejectedWithFifteenMinuteBuffer() {
        TimeSlot existing = slot("2026-05-03", "10:00");

        assertTrue(BufferTimeValidator.hasConflict(
                Arrays.asList(existing), "2026-05-03", "10:10", 15));
    }

    @Test
    public void slotOutsideBufferIsAccepted() {
        TimeSlot existing = slot("2026-05-03", "10:00");

        assertFalse(BufferTimeValidator.hasConflict(
                Arrays.asList(existing), "2026-05-03", "11:15", 15));
    }

    @Test
    public void differentDatesDoNotConflict() {
        TimeSlot existing = slot("2026-05-03", "10:00");

        assertFalse(BufferTimeValidator.hasConflict(
                Arrays.asList(existing), "2026-05-04", "10:10", 15));
    }

    @Test
    public void zeroBufferAllowsBackToBackSixtyMinuteSessions() {
        TimeSlot existing = slot("2026-05-03", "10:00");

        assertFalse(BufferTimeValidator.hasConflict(
                Arrays.asList(existing), "2026-05-03", "11:00", 0));
    }

    private TimeSlot slot(String date, String time) {
        TimeSlot slot = new TimeSlot();
        slot.setDate(date);
        slot.setTime(time);
        slot.setAvailable(true);
        return slot;
    }
}
