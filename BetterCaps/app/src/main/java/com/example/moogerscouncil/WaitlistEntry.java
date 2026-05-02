package com.example.moogerscouncil;

import com.google.firebase.Timestamp;

/**
 * Represents one student's place on a counselor's waitlist.
 * Stored in the Firestore waitlist collection.
 */
public class WaitlistEntry {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_OFFERED = "OFFERED";
    public static final String STATUS_BOOKED = "BOOKED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_EXPIRED = "EXPIRED";

    private String id;
    private String studentId;
    private String counselorId;
    private String assessmentId;
    private String reason;
    private String status;
    private Timestamp requestedAt;
    private String offeredSlotId;
    private Timestamp offeredAt;

    /** Required empty constructor for Firestore. */
    public WaitlistEntry() {
        status = STATUS_ACTIVE;
    }

    /**
     * Creates an active waitlist entry.
     *
     * @param studentId Firebase Auth UID of the student.
     * @param counselorId Counselor identifier being waited on.
     * @param assessmentId Optional intake assessment ID that led to the request.
     * @param reason Human-readable reason for operational review.
     */
    public WaitlistEntry(String studentId, String counselorId,
                         String assessmentId, String reason) {
        this.studentId = studentId;
        this.counselorId = counselorId;
        this.assessmentId = assessmentId;
        this.reason = reason;
        this.status = STATUS_ACTIVE;
        this.requestedAt = Timestamp.now();
    }

    public String getId() { return id; }
    public String getStudentId() { return studentId; }
    public String getCounselorId() { return counselorId; }
    public String getAssessmentId() { return assessmentId; }
    public String getReason() { return reason; }
    public String getStatus() { return status; }
    public Timestamp getRequestedAt() { return requestedAt; }
    public String getOfferedSlotId() { return offeredSlotId; }
    public Timestamp getOfferedAt() { return offeredAt; }

    public void setId(String id) { this.id = id; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public void setCounselorId(String counselorId) { this.counselorId = counselorId; }
    public void setAssessmentId(String assessmentId) { this.assessmentId = assessmentId; }
    public void setReason(String reason) { this.reason = reason; }
    public void setStatus(String status) { this.status = status; }
    public void setRequestedAt(Timestamp requestedAt) { this.requestedAt = requestedAt; }
    public void setOfferedSlotId(String offeredSlotId) { this.offeredSlotId = offeredSlotId; }
    public void setOfferedAt(Timestamp offeredAt) { this.offeredAt = offeredAt; }
}
