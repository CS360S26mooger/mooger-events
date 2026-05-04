package com.example.moogerscouncil;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
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
