/*
 * AvailabilitySchedule.java
 * Role: Local (non-Firestore) model that groups TimeSlot objects by date for a
 *       single counselor. Used by BookingActivity to efficiently highlight dates
 *       on the CalendarView and show date-specific slots without re-querying Firestore.
 *
 * This class is populated from AvailabilityRepository results and is never
 * persisted to Firestore.
 *
 * Design pattern: Value object built via factory method (fromSlots).
 * Part of the BetterCAPS counseling platform.
 */
package com.example.moogerscouncil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Local (non-Firestore) model that groups {@link TimeSlot} objects by date
 * for a single counselor.
 *
 * <p>Used by {@link BookingActivity} to efficiently render calendar highlights
 * and date-specific slot lists without re-querying Firestore on every date tap.</p>
 */
public class AvailabilitySchedule {

    private final String counselorId;
    private final Map<String, List<TimeSlot>> slotsByDate; // key = "yyyy-MM-dd"

    /**
     * Constructs an empty schedule for the given counselor.
     *
     * @param counselorId The UID of the counselor this schedule belongs to.
     */
    public AvailabilitySchedule(String counselorId) {
        this.counselorId = counselorId;
        this.slotsByDate = new HashMap<>();
    }

    /**
     * Builds a schedule from a flat list of {@link TimeSlot} objects.
     * Only slots where {@code available == true} are included — already-booked
     * slots are silently excluded.
     *
     * @param counselorId The counselor this schedule belongs to.
     * @param slots       All fetched TimeSlots (available and unavailable).
     * @return A new {@link AvailabilitySchedule} containing only available slots,
     *         grouped by date string.
     */
    public static AvailabilitySchedule fromSlots(String counselorId, List<TimeSlot> slots) {
        AvailabilitySchedule schedule = new AvailabilitySchedule(counselorId);
        for (TimeSlot slot : slots) {
            if (slot.isAvailable()) {
                schedule.slotsByDate
                        .computeIfAbsent(slot.getDate(), k -> new ArrayList<>())
                        .add(slot);
            }
        }
        return schedule;
    }

    /**
     * Returns all available slots for a specific date.
     *
     * @param date The date in "yyyy-MM-dd" format.
     * @return List of available {@link TimeSlot} objects for that date,
     *         or an empty list if none exist. Never returns null.
     */
    public List<TimeSlot> getSlotsForDate(String date) {
        return slotsByDate.getOrDefault(date, new ArrayList<>());
    }

    /**
     * Returns the set of dates that have at least one available slot.
     * Used by {@link BookingActivity} to inform the user which dates are bookable.
     *
     * @return Set of date strings in "yyyy-MM-dd" format.
     */
    public Set<String> getDatesWithAvailability() {
        return slotsByDate.keySet();
    }

    /**
     * Returns the counselor UID this schedule belongs to.
     *
     * @return The counselor's UID string.
     */
    public String getCounselorId() {
        return counselorId;
    }
}
