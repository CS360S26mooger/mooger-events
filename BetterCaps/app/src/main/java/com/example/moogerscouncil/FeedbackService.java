/*
 * FeedbackService.java
 * Role: Firestore-mapped data container for the 'feedback' collection.
 *       Intentionally has no studentId field — anonymity is enforced at
 *       the schema level per US-21 design requirements.
 *
 * Design pattern: Repository pattern (this is the model; FeedbackRepository
 *                 handles all reads/writes).
 * Part of the BetterCAPS counseling platform.
 */
package com.example.moogerscouncil;

import com.google.firebase.Timestamp;

/**
 * Represents an anonymous post-session feedback submission.
 * Maps directly to a document in the Firestore 'feedback' collection.
 *
 * <p>IMPORTANT: This class intentionally has no studentId field.
 * Anonymity is enforced at the schema level — the feedback document
 * cannot be traced back to the student who submitted it. This is a
 * design decision, not an oversight.</p>
 *
 * <p>Follows the Firestore model convention: no-argument constructor,
 * private fields, public getters/setters.</p>
 */
public class FeedbackService {

    private String id;
    private String appointmentId;
    private int rating;           // 1–5
    private String comment;       // optional
    private Timestamp submittedAt;

    /** Required empty constructor for Firestore deserialization. */
    public FeedbackService() {}

    /**
     * Constructs a FeedbackService with all required fields.
     * Sets {@code submittedAt} to the current server time.
     *
     * @param appointmentId The appointment this feedback relates to.
     * @param rating        Star rating, 1–5.
     * @param comment       Optional text comment (may be null or empty).
     */
    public FeedbackService(String appointmentId, int rating, String comment) {
        this.appointmentId = appointmentId;
        this.rating = rating;
        this.comment = comment;
        this.submittedAt = Timestamp.now();
    }

    // --- Getters ---

    public String getId() { return id; }
    public String getAppointmentId() { return appointmentId; }
    public int getRating() { return rating; }
    public String getComment() { return comment; }
    public Timestamp getSubmittedAt() { return submittedAt; }

    // --- Setters (required by Firestore) ---

    public void setId(String id) { this.id = id; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }
    public void setRating(int rating) { this.rating = rating; }
    public void setComment(String comment) { this.comment = comment; }
    public void setSubmittedAt(Timestamp submittedAt) { this.submittedAt = submittedAt; }
}
