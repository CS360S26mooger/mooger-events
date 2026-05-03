package com.example.moogerscouncil;

import com.google.firebase.Timestamp;

/**
 * Represents a counselor's private note for one appointment/session.
 * Stored in the Firestore sessionNotes collection.
 */
public class SessionNote {

    private String id;
    private String appointmentId;
    private String counselorId;
    private String studentId;
    private String templateKey;
    private String noteText;
    private boolean privateToCounselor;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    /** Date of the appointment this note belongs to (yyyy-MM-dd). Stored at creation so the
     *  history list shows session date rather than note-save date. */
    private String appointmentDate;
    /** Time of the appointment this note belongs to (HH:mm). */
    private String appointmentTime;

    /** Required empty constructor for Firestore. */
    public SessionNote() {}

    /**
     * Creates a private counselor note for a session.
     *
     * @param appointmentId Firestore appointment document ID.
     * @param counselorId Counselor UID on the appointment.
     * @param studentId Student UID on the appointment.
     * @param templateKey Optional quick-template key.
     * @param noteText Note body.
     */
    public SessionNote(String appointmentId, String counselorId, String studentId,
                       String templateKey, String noteText) {
        this.appointmentId = appointmentId;
        this.counselorId = counselorId;
        this.studentId = studentId;
        this.templateKey = templateKey;
        this.noteText = noteText;
        this.privateToCounselor = true;
        this.createdAt = Timestamp.now();
        this.updatedAt = Timestamp.now();
    }

    public String getId() { return id; }
    public String getAppointmentId() { return appointmentId; }
    public String getCounselorId() { return counselorId; }
    public String getStudentId() { return studentId; }
    public String getTemplateKey() { return templateKey; }
    public String getNoteText() { return noteText; }
    public boolean isPrivateToCounselor() { return privateToCounselor; }
    public Timestamp getCreatedAt() { return createdAt; }
    public Timestamp getUpdatedAt() { return updatedAt; }

    public void setId(String id) { this.id = id; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }
    public void setCounselorId(String counselorId) { this.counselorId = counselorId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public void setTemplateKey(String templateKey) { this.templateKey = templateKey; }
    public void setNoteText(String noteText) { this.noteText = noteText; }
    public void setPrivateToCounselor(boolean privateToCounselor) {
        this.privateToCounselor = privateToCounselor;
    }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public String getAppointmentDate() { return appointmentDate; }
    public void setAppointmentDate(String appointmentDate) { this.appointmentDate = appointmentDate; }

    public String getAppointmentTime() { return appointmentTime; }
    public void setAppointmentTime(String appointmentTime) { this.appointmentTime = appointmentTime; }
}
