package com.example.moogerscouncil;

import com.google.firebase.Timestamp;

/**
 * Represents a confirmed appointment between a student and counselor.
 */
public class Appointment {
    private String id;
    private String studentId;
    private String counselorId;
    private String slotId;
    private String date;
    private String time;
    private String status; // "CONFIRMED", "COMPLETED", "CANCELLED"
    private boolean noShowFollowUpRequired;
    private String noShowFollowUpStatus;
    private Timestamp noShowMarkedAt;
    private boolean returningStudent;
    private String crisisEscalationId;

    public Appointment() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getCounselorId() { return counselorId; }
    public void setCounselorId(String counselorId) { this.counselorId = counselorId; }

    public String getSlotId() { return slotId; }
    public void setSlotId(String slotId) { this.slotId = slotId; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isNoShowFollowUpRequired() { return noShowFollowUpRequired; }
    public void setNoShowFollowUpRequired(boolean noShowFollowUpRequired) {
        this.noShowFollowUpRequired = noShowFollowUpRequired;
    }

    public String getNoShowFollowUpStatus() { return noShowFollowUpStatus; }
    public void setNoShowFollowUpStatus(String noShowFollowUpStatus) {
        this.noShowFollowUpStatus = noShowFollowUpStatus;
    }

    public Timestamp getNoShowMarkedAt() { return noShowMarkedAt; }
    public void setNoShowMarkedAt(Timestamp noShowMarkedAt) {
        this.noShowMarkedAt = noShowMarkedAt;
    }

    public boolean isReturningStudent() { return returningStudent; }
    public void setReturningStudent(boolean returningStudent) {
        this.returningStudent = returningStudent;
    }

    public String getCrisisEscalationId() { return crisisEscalationId; }
    public void setCrisisEscalationId(String crisisEscalationId) {
        this.crisisEscalationId = crisisEscalationId;
    }
}
