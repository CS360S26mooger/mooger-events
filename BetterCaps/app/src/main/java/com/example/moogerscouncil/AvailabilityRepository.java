/*
 * AvailabilityRepository.java
 * Role: Single point of access for all Firestore operations on the 'slots' collection.
 *       Replaces direct db.collection("slots") calls scattered across BookingActivity
 *       and CounselorDashboardActivity.
 *
 * Design pattern: Repository pattern, consistent with UserRepository and CounselorRepository.
 * Part of the BetterCAPS counseling platform.
 */
package com.example.moogerscouncil;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository for the Firestore 'slots' collection.
 * All reads and writes to time slot documents flow through this class.
 *
 * <p>Used by {@link BookingActivity} (fetch available slots),
 * {@link CounselorDashboardActivity} (add slots), and
 * {@link AvailabilitySetupActivity} (manage slots).</p>
 */
public class AvailabilityRepository {

    private final CollectionReference slotsCollection;

    /** Initialises the repository with a reference to the 'slots' collection. */
    public AvailabilityRepository() {
        this.slotsCollection = FirebaseFirestore.getInstance().collection("slots");
    }

    // -------------------------------------------------------------------------
    // Callback interfaces
    // -------------------------------------------------------------------------

    /**
     * Callback for operations that return a list of time slots.
     */
    public interface OnSlotsLoadedCallback {
        /**
         * Called when time slots are successfully fetched.
         *
         * @param slots The list of {@link TimeSlot} objects.
         */
        void onSuccess(List<TimeSlot> slots);

        /**
         * Called when the fetch fails.
         *
         * @param e The exception describing the failure.
         */
        void onFailure(Exception e);
    }

    /**
     * Callback for slot write operations (add / remove).
     */
    public interface OnSlotActionCallback {
        /** Called when the action succeeds. */
        void onSuccess();

        /**
         * Called when the action fails.
         *
         * @param e The exception describing the failure.
         */
        void onFailure(Exception e);
    }

    // -------------------------------------------------------------------------
    // Read operations
    // -------------------------------------------------------------------------

    /**
     * Fetches all time slots (available and unavailable) for a given counselor.
     * Used by {@link AvailabilitySetupActivity} to display the full slot list
     * and by {@link AvailabilitySchedule#fromSlots} which filters internally.
     *
     * @param counselorId The counselor whose slots to fetch.
     * @param callback    Receives the full slot list on success.
     */
    public void getSlotsForCounselor(String counselorId, OnSlotsLoadedCallback callback) {
        slotsCollection
                .whereEqualTo("counselorId", counselorId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<TimeSlot> slots = querySnapshot.toObjects(TimeSlot.class);
                    for (int i = 0; i < slots.size(); i++) {
                        slots.get(i).setId(querySnapshot.getDocuments().get(i).getId());
                    }
                    callback.onSuccess(slots);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Fetches only available slots for a given counselor.
     * Used by {@link BookingActivity} where unavailable slots must never be shown.
     *
     * <p>Filters by counselorId only (single-field index, no composite index required),
     * then removes booked slots in memory — consistent with how other repositories
     * in this project avoid composite Firestore index requirements.</p>
     *
     * @param counselorId The counselor whose available slots to fetch.
     * @param callback    Receives the available slot list on success.
     */
    public void getAvailableSlotsForCounselor(String counselorId,
                                               OnSlotsLoadedCallback callback) {
        slotsCollection
                .whereEqualTo("counselorId", counselorId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<TimeSlot> slots = querySnapshot.toObjects(TimeSlot.class);
                    for (int i = 0; i < slots.size(); i++) {
                        slots.get(i).setId(querySnapshot.getDocuments().get(i).getId());
                    }
                    // Filter available-only in memory — avoids requiring a composite index
                    List<TimeSlot> available = new java.util.ArrayList<>();
                    for (TimeSlot slot : slots) {
                        if (slot.isAvailable()) available.add(slot);
                    }
                    callback.onSuccess(available);
                })
                .addOnFailureListener(callback::onFailure);
    }

    // -------------------------------------------------------------------------
    // Write operations
    // -------------------------------------------------------------------------

    /**
     * Adds a new time slot for a counselor with {@code available = true}.
     *
     * @param counselorId The counselor this slot belongs to.
     * @param date        Date in "yyyy-MM-dd" format.
     * @param time        Time in "HH:mm" format.
     * @param callback    Success/failure callback.
     */
    public void addSlot(String counselorId, String date, String time,
                        OnSlotActionCallback callback) {
        Map<String, Object> slotData = new HashMap<>();
        slotData.put("counselorId", counselorId);
        slotData.put("date", date);
        slotData.put("time", time);
        slotData.put("available", true);

        slotsCollection.add(slotData)
                .addOnSuccessListener(docRef -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Removes a time slot document from Firestore.
     * Used by {@link AvailabilitySetupActivity} when a counselor swipes to delete.
     *
     * @param slotId   The Firestore document ID of the slot to delete.
     * @param callback Success/failure callback.
     */
    public void removeSlot(String slotId, OnSlotActionCallback callback) {
        slotsCollection.document(slotId)
                .delete()
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }
}
