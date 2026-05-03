package com.example.moogerscouncil;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

/** Repository for counselor-triggered crisis escalation records. */
public class CrisisEscalationRepository {

    private final CollectionReference crisisCollection;
    private final FirebaseFirestore db;

    /** Initialises crisis escalation Firestore references. */
    public CrisisEscalationRepository() {
        db = FirebaseFirestore.getInstance();
        crisisCollection = db.collection("crisisEscalations");
    }

    public interface OnCrisisActionCallback {
        void onSuccess(String escalationId);
        void onFailure(Exception e);
    }

    /**
     * Creates an escalation record and links it back to the appointment.
     */
    public void createEscalation(CrisisEscalation escalation,
                                 OnCrisisActionCallback callback) {
        String id = crisisCollection.document().getId();
        escalation.setId(id);
        db.runBatch(batch -> {
            batch.set(crisisCollection.document(id), escalation);
            if (escalation.getAppointmentId() != null) {
                batch.update(db.collection("appointments").document(escalation.getAppointmentId()),
                        "crisisEscalationId", id);
            }
        }).addOnSuccessListener(unused -> callback.onSuccess(id))
          .addOnFailureListener(callback::onFailure);
    }
}
