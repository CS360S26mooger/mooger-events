package com.example.moogerscouncil;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/** Repository for Firestore intake assessment operations. */
public class IntakeAssessmentRepository {

    private final CollectionReference intakeCollection;

    /** Initialises a repository for the intakeAssessments collection. */
    public IntakeAssessmentRepository() {
        intakeCollection = FirebaseFirestore.getInstance().collection("intakeAssessments");
    }

    public interface OnAssessmentSavedCallback {
        void onSuccess(String assessmentId);
        void onFailure(Exception e);
    }

    public interface OnAssessmentLoadedCallback {
        void onSuccess(IntakeAssessment assessment);
        void onFailure(Exception e);
    }

    public interface OnAssessmentsLoadedCallback {
        void onSuccess(List<IntakeAssessment> assessments);
        void onFailure(Exception e);
    }

    /**
     * Creates a new intake assessment document.
     *
     * @param assessment Assessment to persist.
     * @param callback Save callback.
     */
    public void saveAssessment(IntakeAssessment assessment,
                               OnAssessmentSavedCallback callback) {
        String id = intakeCollection.document().getId();
        assessment.setId(id);
        intakeCollection.document(id)
                .set(assessment)
                .addOnSuccessListener(unused -> callback.onSuccess(id))
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Stores the counselor selected by the matcher on an existing assessment.
     */
    public void updateMatchedCounselor(String assessmentId, String counselorId,
                                       String counselorName,
                                       OnAssessmentSavedCallback callback) {
        intakeCollection.document(assessmentId)
                .update("matchedCounselorId", counselorId,
                        "matchedCounselorName", counselorName)
                .addOnSuccessListener(unused -> callback.onSuccess(assessmentId))
                .addOnFailureListener(callback::onFailure);
    }

    /** Loads one assessment by ID. */
    public void getAssessment(String assessmentId, OnAssessmentLoadedCallback callback) {
        intakeCollection.document(assessmentId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        callback.onFailure(new IllegalStateException("Assessment not found"));
                        return;
                    }
                    IntakeAssessment assessment = doc.toObject(IntakeAssessment.class);
                    if (assessment == null) {
                        callback.onFailure(new IllegalStateException("Could not parse assessment"));
                        return;
                    }
                    assessment.setId(doc.getId());
                    callback.onSuccess(assessment);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Loads all active assessments for a student and returns the newest by createdAt.
     */
    public void getLatestForStudent(String studentId, OnAssessmentLoadedCallback callback) {
        intakeCollection.whereEqualTo("studentId", studentId)
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(snapshot -> {
                    IntakeAssessment latest = null;
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                        IntakeAssessment item = doc.toObject(IntakeAssessment.class);
                        if (item == null) continue;
                        item.setId(doc.getId());
                        if (latest == null || isNewer(item, latest)) {
                            latest = item;
                        }
                    }
                    if (latest == null) {
                        callback.onFailure(new IllegalStateException("No intake assessment found"));
                    } else {
                        callback.onSuccess(latest);
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    /** Loads active assessments for a student. */
    public void getActiveForStudent(String studentId, OnAssessmentsLoadedCallback callback) {
        intakeCollection.whereEqualTo("studentId", studentId)
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<IntakeAssessment> assessments = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                        IntakeAssessment item = doc.toObject(IntakeAssessment.class);
                        if (item != null) {
                            item.setId(doc.getId());
                            assessments.add(item);
                        }
                    }
                    callback.onSuccess(assessments);
                })
                .addOnFailureListener(callback::onFailure);
    }

    private boolean isNewer(IntakeAssessment item, IntakeAssessment latest) {
        if (item.getCreatedAt() == null) return false;
        if (latest.getCreatedAt() == null) return true;
        return item.getCreatedAt().compareTo(latest.getCreatedAt()) > 0;
    }
}
