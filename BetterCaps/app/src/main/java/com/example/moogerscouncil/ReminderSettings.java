package com.example.moogerscouncil;

import com.google.firebase.Timestamp;

/**
 * Global reminder configuration controlled by an admin.
 * Stored at reminderSettings/default in Firestore.
 */
public class ReminderSettings {
    private boolean enabled24Hour;
    private boolean enabled1Hour;
    private String message24Hour;
    private String message1Hour;
    private String updatedBy;
    private Timestamp updatedAt;

    /** Required empty constructor for Firestore. */
    public ReminderSettings() {}

    public ReminderSettings(boolean enabled24Hour, boolean enabled1Hour,
                            String message24Hour, String message1Hour,
                            String updatedBy) {
        this.enabled24Hour = enabled24Hour;
        this.enabled1Hour = enabled1Hour;
        this.message24Hour = message24Hour;
        this.message1Hour = message1Hour;
        this.updatedBy = updatedBy;
        this.updatedAt = Timestamp.now();
    }

    public static ReminderSettings defaultSettings() {
        return new ReminderSettings(
                true,
                true,
                "Reminder: your counseling session is tomorrow. Please check BetterCaps for details.",
                "Reminder: your counseling session starts in about one hour.",
                ""
        );
    }

    public boolean isEnabled24Hour() { return enabled24Hour; }
    public boolean isEnabled1Hour() { return enabled1Hour; }
    public String getMessage24Hour() { return message24Hour; }
    public String getMessage1Hour() { return message1Hour; }
    public String getUpdatedBy() { return updatedBy; }
    public Timestamp getUpdatedAt() { return updatedAt; }

    public void setEnabled24Hour(boolean enabled24Hour) { this.enabled24Hour = enabled24Hour; }
    public void setEnabled1Hour(boolean enabled1Hour) { this.enabled1Hour = enabled1Hour; }
    public void setMessage24Hour(String message24Hour) { this.message24Hour = message24Hour; }
    public void setMessage1Hour(String message1Hour) { this.message1Hour = message1Hour; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}
