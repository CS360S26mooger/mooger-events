/*
 * CrisisEscalationRepository.java
 * Role: Repository for counselor-triggered crisis escalation records.
 *       Writes new escalations with an appointment back-link, reads unresolved
 *       escalations for the admin Crisis Alerts tab, and marks them resolved.
 *
 * Design pattern: Repository (data layer).
 * Part of the BetterCAPS counseling platform.
 */
package com.example.moogerscouncil;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

/** Repository for counselor-triggered crisis escalation records. */
public class CrisisEscalationRepository {

    private final CollectionReference crisisCollection;
    private final FirebaseFirestore db;

    /** Initialises crisis escalation Firestore references. */
    public CrisisEscalationRepository() {
        db = FirebaseFirestore.getInstance();
        crisisCollection = db.collection("crisisEscalations");
    }

    // ── Callbacks ──────────────────────────────────────────────────────────────

    public interface OnCrisisActionCallback {
        void onSuccess(String escalationId);
        void onFailure(Exception e);
    }

    public interface OnEscalationsLoadedCallback {
        void onSuccess(List<CrisisEscalation> escalations);
        void onFailure(Exception e);
    }

    public interface OnResolveCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    /**
     * Creates an escalation record and links it back to the appointment.
     *
     * @param escalation Populated escalation model (id will be set here).
     * @param callback   Returns the new Firestore document ID on success.
     */
    public void createEscalation(CrisisEscalation escalation,
                                 OnCrisisActionCallback callback) {
        String id = crisisCollection.document().getId();
        escalation.setId(id);
        db.runBatch(batch -> {
            batch.set(crisisCollection.document(id), escalation);
            if (escalation.getAppointmentId() != null) {
                batch.update(db.collection("appointments")
                        .document(escalation.getAppointmentId()),
                        "crisisEscalationId", id);
            }
        }).addOnSuccessListener(unused -> callback.onSuccess(id))
          .addOnFailureListener(callback::onFailure);
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    /**
     * Fetches all unresolved escalations ordered by createdAt descending.
     * Used by the admin Crisis Alerts tab.
     *
     * @param callback Delivers the escalation list or error.
     */
    public void getUnresolvedEscalations(OnEscalationsLoadedCallback callback) {
        crisisCollection
                .whereEqualTo("resolved", false)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<CrisisEscalation> list = new ArrayList<>();
                    snapshot.getDocuments().forEach(doc -> {
                        CrisisEscalation e = doc.toObject(CrisisEscalation.class);
                        if (e != null) {
                            e.setId(doc.getId());
                            list.add(e);
                        }
                    });
                    callback.onSuccess(list);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Marks a single crisis escalation as resolved.
     *
     * @param escalationId Firestore document ID of the escalation.
     * @param callback     Called on success or failure.
     */
    public void markResolved(String escalationId, OnResolveCallback callback) {
        crisisCollection.document(escalationId)
                .update("resolved", true)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }
}
