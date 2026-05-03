package com.example.moogerscouncil;

import com.google.firebase.Timestamp;

/**
 * Records a counselor-triggered crisis escalation workflow.
 * Stored in the Firestore crisisEscalations collection.
 */
public class CrisisEscalation {

    public static final String SEVERITY_MODERATE = "MODERATE";
    public static final String SEVERITY_HIGH = "HIGH";
    public static final String SEVERITY_IMMEDIATE = "IMMEDIATE";

    public static final String ACTION_CALLED_SECURITY = "CALLED_SECURITY";
    public static final String ACTION_REFERRED_CAPS = "REFERRED_CAPS";
    public static final String ACTION_SAFETY_PLAN = "SAFETY_PLAN";
    public static final String ACTION_OTHER = "OTHER";

    private String id;
    private String appointmentId;
    private String counselorId;
    private String studentId;
    private String severity;
    private String actionTaken;
    private String notes;
    private Timestamp createdAt;
    private boolean resolved;

    /** Required empty constructor for Firestore. */
    public CrisisEscalation() {}

    /**
     * Creates an unresolved crisis escalation record.
     */
    public CrisisEscalation(String appointmentId, String counselorId, String studentId,
                            String severity, String actionTaken, String notes) {
        this.appointmentId = appointmentId;
        this.counselorId = counselorId;
        this.studentId = studentId;
        this.severity = severity;
        this.actionTaken = actionTaken;
        this.notes = notes;
        this.createdAt = Timestamp.now();
        this.resolved = false;
    }

    public String getId() { return id; }
    public String getAppointmentId() { return appointmentId; }
    public String getCounselorId() { return counselorId; }
    public String getStudentId() { return studentId; }
    public String getSeverity() { return severity; }
    public String getActionTaken() { return actionTaken; }
    public String getNotes() { return notes; }
    public Timestamp getCreatedAt() { return createdAt; }
    public boolean isResolved() { return resolved; }

    public void setId(String id) { this.id = id; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }
    public void setCounselorId(String counselorId) { this.counselorId = counselorId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public void setSeverity(String severity) { this.severity = severity; }
    public void setActionTaken(String actionTaken) { this.actionTaken = actionTaken; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public void setResolved(boolean resolved) { this.resolved = resolved; }
}
