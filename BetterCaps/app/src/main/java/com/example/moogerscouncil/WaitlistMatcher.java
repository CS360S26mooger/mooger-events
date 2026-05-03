/*
 * WaitlistMatcher.java
 * Role: Pure helper that determines which waitlist entries match a newly-created slot.
 *       Used by AvailabilitySetupActivity after slot creation to auto-resolve entries.
 *
 * Design pattern: Helper / Utility (no state, all static methods).
 * Part of the BetterCAPS counseling platform.
 */
package com.example.moogerscouncil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Determines which waitlist entries match a newly-created slot.
 *
 * <p>Matching rule: a slot matches an entry when
 * {@code slot.date ∈ entry.preferredDates}
 * AND {@code slot.time >= entry.preferredStartTime}
 * AND {@code slot.time < entry.preferredEndTime}.
 * String comparison is safe for "HH:mm" and "yyyy-MM-dd" formats because
 * lexicographic order equals chronological order for zero-padded values.</p>
 *
 * <p>When multiple entries match a single slot, resolution order is
 * strictly by {@code requestedAt} ascending (FIFO) — first-come, first-served.
 * Only the first matching entry is resolved per slot.</p>
 */
public final class WaitlistMatcher {

    private WaitlistMatcher() {}

    /**
     * Returns true if the given slot falls within an entry's scheduling preferences.
     *
     * <p>Returns false when any required preference field is null or empty,
     * which gracefully handles legacy entries that have no preference data.</p>
     *
     * @param entry    The waitlist entry to test.
     * @param slotDate The date of the slot being tested ("yyyy-MM-dd").
     * @param slotTime The time of the slot being tested ("HH:mm").
     * @return true if the slot satisfies the entry's preferred dates and time window.
     */
    public static boolean matches(WaitlistEntry entry, String slotDate, String slotTime) {
        if (entry == null || slotDate == null || slotTime == null) return false;
        if (entry.getPreferredDates() == null || entry.getPreferredDates().isEmpty()) return false;
        if (!entry.getPreferredDates().contains(slotDate)) return false;

        String startTime = entry.getPreferredStartTime();
        String endTime = entry.getPreferredEndTime();
        if (startTime == null || endTime == null) return false;

        return slotTime.compareTo(startTime) >= 0 && slotTime.compareTo(endTime) < 0;
    }

    /**
     * Returns the earliest-requested matching entry from a list, or null if none match.
     *
     * <p>Sorts by {@code requestedAt} ascending before scanning so the FIFO invariant
     * is respected even if the list arrives from Firestore in arbitrary order.</p>
     *
     * @param entries  Active waitlist entries for one counselor.
     * @param slotDate The date of the newly-created slot ("yyyy-MM-dd").
     * @param slotTime The time of the newly-created slot ("HH:mm").
     * @return The FIFO-first entry whose preferences include this slot, or null.
     */
    public static WaitlistEntry findFirstMatch(List<WaitlistEntry> entries,
                                               String slotDate, String slotTime) {
        if (entries == null || entries.isEmpty()) return null;

        List<WaitlistEntry> sorted = new ArrayList<>(entries);
        Collections.sort(sorted, (a, b) -> {
            String ta = a.getRequestedAt() != null ? a.getRequestedAt().toString() : "";
            String tb = b.getRequestedAt() != null ? b.getRequestedAt().toString() : "";
            return ta.compareTo(tb);
        });

        for (WaitlistEntry entry : sorted) {
            if (WaitlistEntry.STATUS_ACTIVE.equals(entry.getStatus())
                    && matches(entry, slotDate, slotTime)) {
                return entry;
            }
        }
        return null;
    }

    /**
     * Returns true if any available slot already covers the student's preferred dates
     * and time window. Used to block waitlist submission when direct booking is possible.
     *
     * @param availableSlots All available slots for the counselor.
     * @param preferredDates The dates the student is available on.
     * @param startTime      Start of the preferred time window ("HH:mm").
     * @param endTime        End of the preferred time window ("HH:mm").
     * @return true if at least one existing available slot falls within the preferences.
     */
    public static boolean existingSlotsMatchPreferences(List<TimeSlot> availableSlots,
                                                        List<String> preferredDates,
                                                        String startTime, String endTime) {
        return findFirstMatchingSlot(availableSlots, preferredDates, startTime, endTime) != null;
    }

    /**
     * Returns the first available slot that falls within the student's preferred dates and
     * time window, or null if none exists.
     *
     * @param availableSlots All available slots for the counselor.
     * @param preferredDates The dates the student is available on.
     * @param startTime      Start of the preferred time window ("HH:mm").
     * @param endTime        End of the preferred time window ("HH:mm").
     * @return The first matching available slot, or null.
     */
    public static TimeSlot findFirstMatchingSlot(List<TimeSlot> availableSlots,
                                                  List<String> preferredDates,
                                                  String startTime, String endTime) {
        if (availableSlots == null || preferredDates == null
                || startTime == null || endTime == null) return null;
        for (TimeSlot slot : availableSlots) {
            if (!slot.isAvailable()) continue;
            if (!preferredDates.contains(slot.getDate())) continue;
            if (slot.getTime().compareTo(startTime) >= 0
                    && slot.getTime().compareTo(endTime) < 0) {
                return slot;
            }
        }
        return null;
    }
}
