package com.example.moogerscouncil;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

/** Repository for counselor availability settings. */
public class AvailabilitySettingsRepository {

    private final CollectionReference settingsCollection;

    /** Initialises the availabilitySettings collection reference. */
    public AvailabilitySettingsRepository() {
        settingsCollection = FirebaseFirestore.getInstance().collection("availabilitySettings");
    }

    public interface OnSettingsLoadedCallback {
        void onSuccess(AvailabilitySettings settings);
        void onFailure(Exception e);
    }

    public interface OnSettingsSavedCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    /** Loads settings or returns defaults when no document exists. */
    public void getSettings(String counselorId, OnSettingsLoadedCallback callback) {
        settingsCollection.document(counselorId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        callback.onSuccess(new AvailabilitySettings(counselorId, 0));
                        return;
                    }
                    AvailabilitySettings settings = doc.toObject(AvailabilitySettings.class);
                    if (settings == null) settings = new AvailabilitySettings(counselorId, 0);
                    settings.setCounselorId(counselorId);
                    callback.onSuccess(settings);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /** Saves settings at availabilitySettings/{counselorId}. */
    public void saveSettings(AvailabilitySettings settings,
                             OnSettingsSavedCallback callback) {
        settings.setUpdatedAt(Timestamp.now());
        settingsCollection.document(settings.getCounselorId())
                .set(settings)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }
}
