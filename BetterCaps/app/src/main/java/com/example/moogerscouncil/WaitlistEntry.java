package com.example.moogerscouncil;

import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a student's waitlist request for a specific counselor.
 * Stored in the Firestore 'waitlist' collection.
 *
 * <p>A waitlist entry captures the student's preferred dates and time window.
 * Resolution happens automatically when the counselor creates a slot that
 * falls within the entry's preferences. Entries are resolved in
 * {@code requestedAt} order (FIFO) to prevent race conditions.</p>
 *
 * <p>Backward compatibility: old entries (created before Sprint 8) may have
 * a {@code reason} field and status values OFFERED / BOOKED / EXPIRED instead of
 * RESOLVED / CANCELLED. Adapters should treat OFFERED/BOOKED as RESOLVED and
 * EXPIRED as CANCELLED when displaying legacy records.</p>
 */
public class WaitlistEntry {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_RESOLVED = "RESOLVED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    // Legacy status values kept for backward-compat display in adapters only.
    public static final String STATUS_OFFERED = "OFFERED";
    public static final String STATUS_BOOKED = "BOOKED";
    public static final String STATUS_EXPIRED = "EXPIRED";

    private String id;
    private String studentId;
    private String counselorId;
    private String assessmentId;
    // Legacy field — present on pre-Sprint-8 entries. Kept for Firestore deserialization.
    private String reason;
    private String note;
    private List<String> preferredDates;
    private String preferredStartTime;
    private String preferredEndTime;
    private String status;
    private Timestamp requestedAt;
    // Legacy fields from pre-Sprint-8 offer flow. Kept for deserialization.
    private String offeredSlotId;
    private Timestamp offeredAt;
    // Resolution fields (Sprint 8+)
    private String resolvedSlotId;
    private String resolvedAppointmentId;
    private Timestamp resolvedAt;

    /** Required empty constructor for Firestore. */
    public WaitlistEntry() {
        status = STATUS_ACTIVE;
        preferredDates = new ArrayList<>();
    }

    /**
     * Creates an active waitlist entry with scheduling preferences.
     *
     * @param studentId      Firebase Auth UID of the requesting student.
     * @param counselorId    Counselor identifier the student wants to see.
     * @param preferredDates List of ISO date strings ("yyyy-MM-dd") the student is available on.
     * @param startTime      Start of preferred time window ("HH:mm").
     * @param endTime        End of preferred time window ("HH:mm").
     * @param note           Optional free-text note from the student to the counselor.
     * @param assessmentId   Optional intake assessment ID that led to this request.
     */
    public WaitlistEntry(String studentId, String counselorId,
                         List<String> preferredDates, String startTime,
                         String endTime, String note, String assessmentId) {
        this.studentId = studentId;
        this.counselorId = counselorId;
        this.preferredDates = preferredDates != null ? preferredDates : new ArrayList<>();
        this.preferredStartTime = startTime;
        this.preferredEndTime = endTime;
        this.note = note;
        this.assessmentId = assessmentId;
        this.status = STATUS_ACTIVE;
        this.requestedAt = Timestamp.now();
    }

    /**
     * Legacy constructor for simple entries without scheduling preferences.
     * Retained for callers that have not yet migrated to the preference-based form.
     *
     * @param studentId   Firebase Auth UID.
     * @param counselorId Counselor identifier.
     * @param assessmentId Optional intake assessment ID.
     * @param reason      Human-readable reason string.
     */
    public WaitlistEntry(String studentId, String counselorId,
                         String assessmentId, String reason) {
        this.studentId = studentId;
        this.counselorId = counselorId;
        this.assessmentId = assessmentId;
        this.reason = reason;
        this.preferredDates = new ArrayList<>();
        this.status = STATUS_ACTIVE;
        this.requestedAt = Timestamp.now();
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    /** @return Firestore document ID. */
    public String getId() { return id; }

    /** @return Firebase Auth UID of the student who submitted this request. */
    public String getStudentId() { return studentId; }

    /** @return Counselor document / Auth UID this request is for. */
    public String getCounselorId() { return counselorId; }

    /** @return Optional intake assessment ID that triggered this request. */
    public String getAssessmentId() { return assessmentId; }

    /** @return Legacy reason string from pre-Sprint-8 entries, or null for new entries. */
    public String getReason() { return reason; }

    /** @return Optional free-text note from the student to the counselor. */
    public String getNote() { return note; }

    /**
     * @return List of ISO date strings the student is available on,
     *         or an empty list for legacy entries that have no preference.
     */
    public List<String> getPreferredDates() {
        return preferredDates != null ? preferredDates : new ArrayList<>();
    }

    /** @return Start of preferred time window ("HH:mm"), or null for legacy entries. */
    public String getPreferredStartTime() { return preferredStartTime; }

    /** @return End of preferred time window ("HH:mm"), or null for legacy entries. */
    public String getPreferredEndTime() { return preferredEndTime; }

    /**
     * @return Entry status. One of {@link #STATUS_ACTIVE}, {@link #STATUS_RESOLVED},
     *         {@link #STATUS_CANCELLED}, or a legacy value.
     */
    public String getStatus() { return status; }

    /** @return Timestamp when the student submitted this request. Used for FIFO ordering. */
    public Timestamp getRequestedAt() { return requestedAt; }

    /** @return Slot ID that fulfilled this request on resolution, or null if not yet resolved. */
    public String getResolvedSlotId() { return resolvedSlotId; }

    /** @return Appointment ID created when this entry was resolved, or null if not yet resolved. */
    public String getResolvedAppointmentId() { return resolvedAppointmentId; }

    /** @return Timestamp when this entry was resolved, or null if not yet resolved. */
    public Timestamp getResolvedAt() { return resolvedAt; }

    // Legacy getters kept for Firestore deserialization of old documents.
    public String getOfferedSlotId() { return offeredSlotId; }
    public Timestamp getOfferedAt() { return offeredAt; }

    // -------------------------------------------------------------------------
    // Setters
    // -------------------------------------------------------------------------

    /** @param id Firestore document ID. */
    public void setId(String id) { this.id = id; }

    public void setStudentId(String studentId) { this.studentId = studentId; }
    public void setCounselorId(String counselorId) { this.counselorId = counselorId; }
    public void setAssessmentId(String assessmentId) { this.assessmentId = assessmentId; }
    public void setReason(String reason) { this.reason = reason; }
    public void setNote(String note) { this.note = note; }
    public void setPreferredDates(List<String> preferredDates) { this.preferredDates = preferredDates; }
    public void setPreferredStartTime(String preferredStartTime) { this.preferredStartTime = preferredStartTime; }
    public void setPreferredEndTime(String preferredEndTime) { this.preferredEndTime = preferredEndTime; }
    public void setStatus(String status) { this.status = status; }
    public void setRequestedAt(Timestamp requestedAt) { this.requestedAt = requestedAt; }
    public void setResolvedSlotId(String resolvedSlotId) { this.resolvedSlotId = resolvedSlotId; }
    public void setResolvedAppointmentId(String resolvedAppointmentId) { this.resolvedAppointmentId = resolvedAppointmentId; }
    public void setResolvedAt(Timestamp resolvedAt) { this.resolvedAt = resolvedAt; }
    public void setOfferedSlotId(String offeredSlotId) { this.offeredSlotId = offeredSlotId; }
    public void setOfferedAt(Timestamp offeredAt) { this.offeredAt = offeredAt; }
}
