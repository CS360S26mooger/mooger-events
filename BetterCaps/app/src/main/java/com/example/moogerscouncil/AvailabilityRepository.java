package com.example.moogerscouncil;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

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

    public interface OnBufferCheckCallback {
        void onAvailable();
        void onConflict(String reason);
        void onFailure(Exception e);
    }

    /**
     * Callback for slot creation operations that need to return the created slot.
     * Used by {@link AvailabilitySetupActivity} for the waitlist auto-resolution hook.
     */
    public interface OnSlotCreatedCallback {
        /**
         * Called when the slot document was written successfully.
         *
         * @param slot The fully-populated {@link TimeSlot} including its Firestore ID.
         */
        void onSuccess(TimeSlot slot);
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

    /** Fetches slots for one counselor on one date. */
    public void getSlotsForDate(String counselorId, String date, OnSlotsLoadedCallback callback) {
        getSlotsForCounselor(counselorId, new OnSlotsLoadedCallback() {
            @Override
            public void onSuccess(List<TimeSlot> slots) {
                List<TimeSlot> sameDate = new ArrayList<>();
                for (TimeSlot slot : slots) {
                    if (date != null && date.equals(slot.getDate())) {
                        sameDate.add(slot);
                    }
                }
                callback.onSuccess(sameDate);
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    /**
     * Checks whether a new slot would violate the counselor's buffer settings.
     * Rule: 60-minute session length plus configured buffer minutes.
     */
    public void canAddSlotWithBuffer(String counselorId, String date, String time,
                                     int bufferMinutes,
                                     OnBufferCheckCallback callback) {
        getSlotsForCounselor(counselorId, new OnSlotsLoadedCallback() {
            @Override
            public void onSuccess(List<TimeSlot> slots) {
                try {
                    if (BufferTimeValidator.hasConflict(slots, date, time, bufferMinutes)) {
                        callback.onConflict("Slot conflicts with buffer time.");
                    } else {
                        callback.onAvailable();
                    }
                } catch (IllegalArgumentException e) {
                    callback.onFailure(e);
                }
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
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
     * Adds a new slot and returns the created {@link TimeSlot} (with its Firestore ID) via
     * callback. Use this variant when the caller needs the slot object — e.g. the
     * waitlist auto-resolution hook in {@link AvailabilitySetupActivity}.
     *
     * @param counselorId Firebase Auth UID of the counselor.
     * @param date        Date in "yyyy-MM-dd" format.
     * @param time        Time in "HH:mm" format.
     * @param callback    Receives the created slot on success.
     */
    public void addSlotAndReturn(String counselorId, String date, String time,
                                 OnSlotCreatedCallback callback) {
        Map<String, Object> slotData = new HashMap<>();
        slotData.put("counselorId", counselorId);
        slotData.put("date", date);
        slotData.put("time", time);
        slotData.put("available", true);

        slotsFor(counselorId).add(slotData)
                .addOnSuccessListener(docRef -> {
                    TimeSlot slot = new TimeSlot();
                    slot.setId(docRef.getId());
                    slot.setCounselorId(counselorId);
                    slot.setDate(date);
                    slot.setTime(time);
                    slot.setAvailable(true);
                    callback.onSuccess(slot);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Creates multiple slots for one date using a single Firestore WriteBatch.
     * Used by {@link GenerateSlotsActivity} for bulk slot generation.
     *
     * @param counselorId Firebase Auth UID of the counselor.
     * @param date        Date in "yyyy-MM-dd" format.
     * @param times       List of times in "HH:mm" format.
     * @param callback    Success/failure callback.
     */
    public void addSlotsBatch(String counselorId, String date, List<String> times,
                               OnSlotActionCallback callback) {
        WriteBatch batch = db.batch();
        CollectionReference col = slotsFor(counselorId);
        for (String time : times) {
            Map<String, Object> data = new HashMap<>();
            data.put("counselorId", counselorId);
            data.put("date", date);
            data.put("time", time);
            data.put("available", true);
            batch.set(col.document(), data);
        }
        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Creates slots for multiple dates in a single Firestore WriteBatch.
     * Used by {@link GenerateSlotsActivity} for multi-day bulk generation.
     *
     * @param counselorId  Firebase Auth UID of the counselor.
     * @param slotsPerDate Map of "yyyy-MM-dd" date → list of "HH:mm" times.
     * @param callback     Success/failure callback.
     */
    public void addSlotsBatchMultiDay(String counselorId, Map<String, List<String>> slotsPerDate,
                                      OnSlotActionCallback callback) {
        WriteBatch batch = db.batch();
        CollectionReference col = slotsFor(counselorId);
        for (Map.Entry<String, List<String>> entry : slotsPerDate.entrySet()) {
            for (String time : entry.getValue()) {
                Map<String, Object> data = new HashMap<>();
                data.put("counselorId", counselorId);
                data.put("date", entry.getKey());
                data.put("time", time);
                data.put("available", true);
                batch.set(col.document(), data);
            }
        }
        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess())
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
