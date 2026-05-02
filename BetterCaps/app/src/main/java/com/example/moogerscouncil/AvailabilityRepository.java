package com.example.moogerscouncil;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository for slot documents stored under Slots/{counselorId}/slots.
 *
 * <p>Structure: {@code Slots/{counselorAuthUID}/slots/{slotDocId}}</p>
 *
 * <p>Each counselor's slots live under their own branch keyed by Auth UID.
 * Reads are a direct path lookup — no collection-wide scan or composite index needed.</p>
 */
public class AvailabilityRepository {

    private final FirebaseFirestore db;

    public AvailabilityRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    // -------------------------------------------------------------------------
    // Callback interfaces
    // -------------------------------------------------------------------------

    public interface OnSlotsLoadedCallback {
        void onSuccess(List<TimeSlot> slots);
        void onFailure(Exception e);
    }

    public interface OnSlotActionCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface OnAvailabilityCheckCallback {
        void onSuccess(boolean hasAvailableSlots);
        void onFailure(Exception e);
    }

    // -------------------------------------------------------------------------
    // Path helper
    // -------------------------------------------------------------------------

    /**
     * Returns the subcollection reference for a counselor's slots.
     * Path: {@code Slots/{counselorId}/slots}
     *
     * @param counselorId The Firebase Auth UID of the counselor.
     */
    private CollectionReference slotsFor(String counselorId) {
        return db.collection("Slots").document(counselorId).collection("slots");
    }

    // -------------------------------------------------------------------------
    // Read operations
    // -------------------------------------------------------------------------

    /**
     * Fetches all time slots (available and booked) for a counselor.
     * Used by {@link AvailabilitySetupActivity} to show the full slot list.
     *
     * @param counselorId Firebase Auth UID of the counselor.
     * @param callback    Receives the full slot list on success.
     */
    public void getSlotsForCounselor(String counselorId, OnSlotsLoadedCallback callback) {
        slotsFor(counselorId)
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
     * Fetches only available slots for a counselor.
     * Used by {@link BookingActivity}; all results are cached in
     * {@link AvailabilitySchedule} so the calendar needs no further Firestore reads.
     *
     * @param counselorId Firebase Auth UID of the counselor.
     * @param callback    Receives the available-only slot list on success.
     */
    public void getAvailableSlotsForCounselor(String counselorId,
                                               OnSlotsLoadedCallback callback) {
        slotsFor(counselorId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<TimeSlot> slots = querySnapshot.toObjects(TimeSlot.class);
                    for (int i = 0; i < slots.size(); i++) {
                        slots.get(i).setId(querySnapshot.getDocuments().get(i).getId());
                    }
                    List<TimeSlot> available = new ArrayList<>();
                    for (TimeSlot slot : slots) {
                        if (slot.isAvailable()) available.add(slot);
                    }
                    callback.onSuccess(available);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Checks whether at least one available slot exists for the counselor.
     *
     * @param counselorId Firebase Auth UID or legacy slot-path key.
     * @param callback Receives true when one or more available slots exist.
     */
    public void hasAvailableSlots(String counselorId, OnAvailabilityCheckCallback callback) {
        getAvailableSlotsForCounselor(counselorId, new OnSlotsLoadedCallback() {
            @Override
            public void onSuccess(List<TimeSlot> slots) {
                callback.onSuccess(!slots.isEmpty());
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    // -------------------------------------------------------------------------
    // Write operations
    // -------------------------------------------------------------------------

    /**
     * Adds a new slot under {@code Slots/{counselorId}/slots}.
     * Firestore creates the parent document implicitly on first write — no
     * separate initialisation step is needed.
     *
     * @param counselorId Firebase Auth UID of the counselor.
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

        slotsFor(counselorId).add(slotData)
                .addOnSuccessListener(docRef -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Deletes a slot document from {@code Slots/{counselorId}/slots/{slotId}}.
     *
     * @param counselorId Firebase Auth UID of the counselor who owns the slot.
     * @param slotId      Firestore document ID of the slot to delete.
     * @param callback    Success/failure callback.
     */
    public void removeSlot(String counselorId, String slotId, OnSlotActionCallback callback) {
        slotsFor(counselorId).document(slotId)
                .delete()
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }
}
