package com.example.moogerscouncil;

import java.util.List;

/**
 * Validates counselor slot spacing using a 60-minute default session length
 * plus the counselor's configured buffer.
 */
public final class BufferTimeValidator {

    public static final int DEFAULT_SESSION_MINUTES = 60;

    private BufferTimeValidator() {}

    /** Returns true when the proposed slot conflicts with an existing same-day slot. */
    public static boolean hasConflict(List<TimeSlot> slots, String date, String time,
                                      int bufferMinutes) {
        if (slots == null) return false;
        for (TimeSlot slot : slots) {
            if (slot == null || !safeEquals(date, slot.getDate())) continue;
            if (isWithinBlockedWindow(slot.getTime(), time, bufferMinutes)) {
                return true;
            }
        }
        return false;
    }

    /** Returns true when two same-day start times are too close. */
    public static boolean isWithinBlockedWindow(String existingTime, String proposedTime,
                                                int bufferMinutes) {
        int existing = parseMinutes(existingTime);
        int proposed = parseMinutes(proposedTime);
        int blockedMinutes = DEFAULT_SESSION_MINUTES + Math.max(0, bufferMinutes);
        return Math.abs(existing - proposed) < blockedMinutes;
    }

    /** Parses HH:mm into minutes after midnight. */
    public static int parseMinutes(String hhmm) {
        if (hhmm == null || !hhmm.matches("\\d{1,2}:\\d{2}")) {
            throw new IllegalArgumentException("Time must be HH:mm");
        }
        String[] parts = hhmm.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }

    private static boolean safeEquals(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }
}
