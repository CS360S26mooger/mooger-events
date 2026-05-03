package com.example.moogerscouncil;

import com.google.firebase.Timestamp;

/** Represents a slot offered to a waitlisted student. */
public class WaitlistOffer {

    public static final String STATUS_OFFERED = "OFFERED";
    public static final String STATUS_ACCEPTED = "ACCEPTED";
    public static final String STATUS_DECLINED = "DECLINED";
    public static final String STATUS_EXPIRED = "EXPIRED";

    private String id;
    private String waitlistEntryId;
    private String studentId;
    private String counselorId;
    private String slotId;
    private String date;
    private String time;
    private String status;
    private Timestamp offeredAt;
    private Timestamp expiresAt;

    /** Required empty constructor for Firestore. */
    public WaitlistOffer() {}

    /** Creates an offered-slot record from an active waitlist entry and available slot. */
    public WaitlistOffer(WaitlistEntry entry, TimeSlot slot) {
        this.waitlistEntryId = entry.getId();
        this.studentId = entry.getStudentId();
        this.counselorId = entry.getCounselorId();
        this.slotId = slot.getId();
        this.date = slot.getDate();
        this.time = slot.getTime();
        this.status = STATUS_OFFERED;
        this.offeredAt = Timestamp.now();
    }

    public String getId() { return id; }
    public String getWaitlistEntryId() { return waitlistEntryId; }
    public String getStudentId() { return studentId; }
    public String getCounselorId() { return counselorId; }
    public String getSlotId() { return slotId; }
    public String getDate() { return date; }
    public String getTime() { return time; }
    public String getStatus() { return status; }
    public Timestamp getOfferedAt() { return offeredAt; }
    public Timestamp getExpiresAt() { return expiresAt; }

    public void setId(String id) { this.id = id; }
    public void setWaitlistEntryId(String waitlistEntryId) { this.waitlistEntryId = waitlistEntryId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public void setCounselorId(String counselorId) { this.counselorId = counselorId; }
    public void setSlotId(String slotId) { this.slotId = slotId; }
    public void setDate(String date) { this.date = date; }
    public void setTime(String time) { this.time = time; }
    public void setStatus(String status) { this.status = status; }
    public void setOfferedAt(Timestamp offeredAt) { this.offeredAt = offeredAt; }
    public void setExpiresAt(Timestamp expiresAt) { this.expiresAt = expiresAt; }
}
