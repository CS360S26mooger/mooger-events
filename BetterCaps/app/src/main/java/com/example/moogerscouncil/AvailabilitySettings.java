package com.example.moogerscouncil;

import com.google.firebase.Timestamp;

/** Stores counselor-level scheduling preferences such as buffer time. */
public class AvailabilitySettings {

    public static final String PROVIDER_NONE = "NONE";
    public static final String PROVIDER_GOOGLE = "GOOGLE";
    public static final String PROVIDER_OUTLOOK = "OUTLOOK";
    public static final String PROVIDER_DEVICE = "DEVICE";

    private String counselorId;
    private int bufferMinutes;
    private boolean externalCalendarEnabled;
    private String calendarProvider;
    private boolean exportIcsEnabled;
    private Timestamp updatedAt;

    /** Required empty constructor for Firestore. */
    public AvailabilitySettings() {
        bufferMinutes = 0;
        calendarProvider = PROVIDER_NONE;
        externalCalendarEnabled = false;
        exportIcsEnabled = false;
    }

    /**
     * Creates scheduling settings for one counselor.
     *
     * @param counselorId Counselor Auth UID.
     * @param bufferMinutes Minutes to keep between 60-minute sessions.
     */
    public AvailabilitySettings(String counselorId, int bufferMinutes) {
        this.counselorId = counselorId;
        this.bufferMinutes = bufferMinutes;
        this.calendarProvider = PROVIDER_DEVICE;
        this.externalCalendarEnabled = false;
        this.exportIcsEnabled = true;
        this.updatedAt = Timestamp.now();
    }

    public String getCounselorId() { return counselorId; }
    public int getBufferMinutes() { return bufferMinutes; }
    public boolean isExternalCalendarEnabled() { return externalCalendarEnabled; }
    public String getCalendarProvider() { return calendarProvider; }
    public boolean isExportIcsEnabled() { return exportIcsEnabled; }
    public Timestamp getUpdatedAt() { return updatedAt; }

    public void setCounselorId(String counselorId) { this.counselorId = counselorId; }
    public void setBufferMinutes(int bufferMinutes) { this.bufferMinutes = bufferMinutes; }
    public void setExternalCalendarEnabled(boolean externalCalendarEnabled) {
        this.externalCalendarEnabled = externalCalendarEnabled;
    }
    public void setCalendarProvider(String calendarProvider) { this.calendarProvider = calendarProvider; }
    public void setExportIcsEnabled(boolean exportIcsEnabled) { this.exportIcsEnabled = exportIcsEnabled; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}
