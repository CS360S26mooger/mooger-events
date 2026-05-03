package com.example.moogerscouncil;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/** Repository for Firestore waitlist operations. */
public class WaitlistRepository {

    private final CollectionReference waitlistCollection;
    private final CollectionReference offersCollection;

    /** Initialises a repository for the waitlist collection. */
    public WaitlistRepository() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        waitlistCollection = db.collection("waitlist");
        offersCollection = db.collection("waitlistOffers");
    }

    public interface OnWaitlistActionCallback {
        void onSuccess();
        void onAlreadyWaitlisted();
        void onFailure(Exception e);
    }

    public interface OnWaitlistLoadedCallback {
        void onSuccess(List<WaitlistEntry> entries);
        void onFailure(Exception e);
    }

    public interface OnWaitlistCountCallback {
        void onSuccess(int count);
        void onFailure(Exception e);
    }

    public interface OnNextWaitlistCallback {
        void onSuccess(WaitlistEntry entry);
        void onEmpty();
        void onFailure(Exception e);
    }

    public interface OnWaitlistSimpleCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    /**
     * Adds a student to a counselor waitlist unless an active duplicate exists.
     */
    public void joinWaitlist(WaitlistEntry entry, OnWaitlistActionCallback callback) {
        waitlistCollection
                .whereEqualTo("studentId", entry.getStudentId())
                .whereEqualTo("counselorId", entry.getCounselorId())
                .whereEqualTo("status", WaitlistEntry.STATUS_ACTIVE)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        callback.onAlreadyWaitlisted();
                        return;
                    }
                    String id = waitlistCollection.document().getId();
                    entry.setId(id);
                    waitlistCollection.document(id)
                            .set(entry)
                            .addOnSuccessListener(unused -> callback.onSuccess())
                            .addOnFailureListener(callback::onFailure);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /** Loads active waitlist entries for one counselor. */
    public void getActiveWaitlistForCounselor(String counselorId,
                                              OnWaitlistLoadedCallback callback) {
        waitlistCollection
                .whereEqualTo("counselorId", counselorId)
                .whereEqualTo("status", WaitlistEntry.STATUS_ACTIVE)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<WaitlistEntry> entries = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                        WaitlistEntry entry = doc.toObject(WaitlistEntry.class);
                        if (entry != null) {
                            entry.setId(doc.getId());
                            entries.add(entry);
                        }
                    }
                    callback.onSuccess(entries);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /** Counts active waitlist entries for one counselor. */
    public void getActiveWaitlistCountForCounselor(String counselorId,
                                                   OnWaitlistCountCallback callback) {
        getActiveWaitlistForCounselor(counselorId, new OnWaitlistLoadedCallback() {
            @Override
            public void onSuccess(List<WaitlistEntry> entries) {
                callback.onSuccess(entries.size());
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    /** Loads active waitlist entries for a student for dashboard display. */
    public void getActiveWaitlistForStudent(String studentId,
                                            OnWaitlistLoadedCallback callback) {
        waitlistCollection
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("status", WaitlistEntry.STATUS_ACTIVE)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<WaitlistEntry> entries = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                        WaitlistEntry entry = doc.toObject(WaitlistEntry.class);
                        if (entry != null) {
                            entry.setId(doc.getId());
                            entries.add(entry);
                        }
                    }
                    callback.onSuccess(entries);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /** Returns the oldest active entry for a counselor. */
    public void getNextActiveEntry(String counselorId, OnNextWaitlistCallback callback) {
        getActiveWaitlistForCounselor(counselorId, new OnWaitlistLoadedCallback() {
            @Override
            public void onSuccess(List<WaitlistEntry> entries) {
                if (entries.isEmpty()) {
                    callback.onEmpty();
                    return;
                }
                WaitlistEntry next = entries.get(0);
                for (WaitlistEntry entry : entries) {
                    if (String.valueOf(entry.getRequestedAt())
                            .compareTo(String.valueOf(next.getRequestedAt())) < 0) {
                        next = entry;
                    }
                }
                callback.onSuccess(next);
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    /** Marks a waitlist entry as offered and creates a waitlistOffers record. */
    public void markOffered(WaitlistEntry entry, TimeSlot slot,
                            OnWaitlistSimpleCallback callback) {
        String offerId = offersCollection.document().getId();
        WaitlistOffer offer = new WaitlistOffer(entry, slot);
        offer.setId(offerId);
        FirebaseFirestore.getInstance().runBatch(batch -> {
            batch.set(offersCollection.document(offerId), offer);
            batch.update(waitlistCollection.document(entry.getId()),
                    "status", WaitlistEntry.STATUS_OFFERED,
                    "offeredSlotId", slot.getId(),
                    "offeredAt", com.google.firebase.Timestamp.now());
        }).addOnSuccessListener(unused -> callback.onSuccess())
          .addOnFailureListener(callback::onFailure);
    }

    /** Marks a waitlist entry as booked after a student accepts an offered slot. */
    public void markBooked(String entryId, OnWaitlistSimpleCallback callback) {
        waitlistCollection.document(entryId)
                .update("status", WaitlistEntry.STATUS_BOOKED)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }
}
