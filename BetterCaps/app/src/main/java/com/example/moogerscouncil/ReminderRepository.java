package com.example.moogerscouncil;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository for admin reminder configuration and generated reminder records.
 */
public class ReminderRepository {
    private static final String SETTINGS_DOC_ID = "default";

    private final CollectionReference settingsCollection;
    private final CollectionReference recordsCollection;

    public ReminderRepository() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        settingsCollection = db.collection("reminderSettings");
        recordsCollection = db.collection("reminderRecords");
    }

    public interface OnSettingsLoadedCallback {
        void onSuccess(ReminderSettings settings);
        void onFailure(Exception e);
    }

    public interface OnReminderActionCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface OnReminderRecordCallback {
        void onCreated(boolean created);
        void onFailure(Exception e);
    }

    public interface OnRemindersLoadedCallback {
        void onSuccess(List<ReminderRecord> records);
        void onFailure(Exception e);
    }

    public interface OnCountCallback {
        void onSuccess(int count);
        void onFailure(Exception e);
    }

    public void getSettings(OnSettingsLoadedCallback callback) {
        settingsCollection.document(SETTINGS_DOC_ID)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        callback.onSuccess(ReminderSettings.defaultSettings());
                        return;
                    }
                    ReminderSettings settings = doc.toObject(ReminderSettings.class);
                    callback.onSuccess(settings != null ? settings : ReminderSettings.defaultSettings());
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void saveSettings(ReminderSettings settings, OnReminderActionCallback callback) {
        settings.setUpdatedAt(Timestamp.now());
        settingsCollection.document(SETTINGS_DOC_ID)
                .set(settings)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Returns reminder records due for delivery for this student.
     * Queries by studentId only (no composite index required) and filters
     * delivered==false and scheduledFor<=now in memory.
     * Batch-marks all returned records as delivered so they fire at most once.
     */
    public void getDueRemindersForStudent(String studentId,
                                          OnRemindersLoadedCallback callback) {
        if (studentId == null || studentId.isEmpty()) {
            callback.onSuccess(new ArrayList<>());
            return;
        }
        long nowMs = System.currentTimeMillis();
        recordsCollection
                .whereEqualTo("studentId", studentId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<ReminderRecord> due = new ArrayList<>();
                    List<DocumentReference> toMark = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Boolean delivered = doc.getBoolean("delivered");
                        if (Boolean.TRUE.equals(delivered)) continue;
                        Timestamp sched = doc.getTimestamp("scheduledFor");
                        if (sched == null || sched.toDate().getTime() > nowMs) continue;
                        ReminderRecord r = new ReminderRecord();
                        r.setId(doc.getId());
                        r.setAppointmentId(doc.getString("appointmentId"));
                        r.setStudentId(doc.getString("studentId"));
                        r.setCounselorId(doc.getString("counselorId"));
                        r.setReminderType(doc.getString("reminderType"));
                        r.setMessageText(doc.getString("messageText"));
                        r.setScheduledFor(sched);
                        r.setDelivered(false);
                        due.add(r);
                        toMark.add(doc.getReference());
                    }
                    if (!toMark.isEmpty()) {
                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                        WriteBatch batch = db.batch();
                        for (DocumentReference ref : toMark) {
                            batch.update(ref, "delivered", true);
                        }
                        batch.commit(); // fire-and-forget; delivery is best-effort
                    }
                    callback.onSuccess(due);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Returns the count of reminder records not yet delivered.
     * Used by AdminDashboardActivity to show a pending queue size.
     */
    public void getPendingReminderCount(OnCountCallback callback) {
        recordsCollection
                .whereEqualTo("delivered", false)
                .get()
                .addOnSuccessListener(snapshot -> callback.onSuccess(snapshot.size()))
                .addOnFailureListener(callback::onFailure);
    }

    public void createReminderRecordIfMissing(Appointment appointment, String reminderType,
                                              String messageText, Timestamp scheduledFor,
                                              OnReminderRecordCallback callback) {
        if (appointment == null || appointment.getId() == null || appointment.getId().isEmpty()) {
            callback.onCreated(false);
            return;
        }
        String recordId = appointment.getId() + "_" + reminderType;
        DocumentReference recordRef = recordsCollection.document(recordId);
        recordRef.get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        callback.onCreated(false);
                        return;
                    }
                    Map<String, Object> data = new HashMap<>();
                    data.put("appointmentId", appointment.getId());
                    data.put("studentId", appointment.getStudentId());
                    data.put("counselorId", appointment.getCounselorId());
                    data.put("reminderType", reminderType);
                    data.put("messageText", messageText);
                    data.put("scheduledFor", scheduledFor);
                    data.put("createdAt", Timestamp.now());
                    data.put("delivered", false);
                    recordRef.set(data)
                            .addOnSuccessListener(unused -> callback.onCreated(true))
                            .addOnFailureListener(callback::onFailure);
                })
                .addOnFailureListener(callback::onFailure);
    }
}
