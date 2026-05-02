package com.example.moogerscouncil;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/** Repository for Firestore waitlist operations. */
public class WaitlistRepository {

    private final CollectionReference waitlistCollection;

    /** Initialises a repository for the waitlist collection. */
    public WaitlistRepository() {
        waitlistCollection = FirebaseFirestore.getInstance().collection("waitlist");
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
}
