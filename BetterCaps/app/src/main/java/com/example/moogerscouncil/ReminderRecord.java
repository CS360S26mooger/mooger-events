package com.example.moogerscouncil;

import com.google.firebase.Timestamp;

/** In-app reminder record stored at reminderRecords/{appointmentId}_{reminderType}. */
public class ReminderRecord {

    private String id;
    private String appointmentId;
    private String studentId;
    private String counselorId;
    private String reminderType;
    private String messageText;
    private Timestamp scheduledFor;
    private Timestamp createdAt;
    private boolean delivered;

    public ReminderRecord() {}

    public String getId() { return id; }
    public String getAppointmentId() { return appointmentId; }
    public String getStudentId() { return studentId; }
    public String getCounselorId() { return counselorId; }
    public String getReminderType() { return reminderType; }
    public String getMessageText() { return messageText; }
    public Timestamp getScheduledFor() { return scheduledFor; }
    public Timestamp getCreatedAt() { return createdAt; }
    public boolean isDelivered() { return delivered; }

    public void setId(String id) { this.id = id; }
    public void setAppointmentId(String v) { this.appointmentId = v; }
    public void setStudentId(String v) { this.studentId = v; }
    public void setCounselorId(String v) { this.counselorId = v; }
    public void setReminderType(String v) { this.reminderType = v; }
    public void setMessageText(String v) { this.messageText = v; }
    public void setScheduledFor(Timestamp v) { this.scheduledFor = v; }
    public void setCreatedAt(Timestamp v) { this.createdAt = v; }
    public void setDelivered(boolean v) { this.delivered = v; }
}
