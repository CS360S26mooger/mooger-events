package com.example.moogerscouncil;
/**
 * Represents a bookable time slot for a counseling session.
 * Manages availability state and booking logic.
 */
public class TimeSlot {
    private String id;
    private String counselorId;
    private String date;
    private String time;
    private boolean available;

    public TimeSlot() {} // Required by Firestore

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCounselorId() { return counselorId; }
    public void setCounselorId(String counselorId) { this.counselorId = counselorId; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    /**
     * Books this slot, marking it as unavailable.
     * @throws IllegalStateException if slot is already booked
     */
    public void book() {
        if (!available) {
            throw new IllegalStateException("Slot is already booked");
        }
        this.available = false;
    }
}

