package com.example.moogerscouncil;

import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a student's completed intake questionnaire.
 * Stored in the Firestore intakeAssessments collection and used by
 * {@link IntakeMatcher} to recommend counselors.
 */
public class IntakeAssessment {

    public static final String URGENCY_LOW = "LOW";
    public static final String URGENCY_MEDIUM = "MEDIUM";
    public static final String URGENCY_HIGH = "HIGH";
    public static final String URGENCY_CRISIS = "CRISIS";

    private String id;
    private String studentId;
    private String primaryConcern;
    private String duration;
    private String supportType;
    private String urgencyLevel;
    private List<String> recommendedSpecializations;
    private String matchedCounselorId;
    private String matchedCounselorName;
    private Timestamp createdAt;
    private boolean active;

    /** Required empty constructor for Firestore. */
    public IntakeAssessment() {
        recommendedSpecializations = new ArrayList<>();
        urgencyLevel = URGENCY_LOW;
        active = true;
    }

    /**
     * Creates a complete intake assessment from quiz answers.
     *
     * @param studentId Firebase Auth UID of the student.
     * @param primaryConcern Main concern selected in the quiz.
     * @param duration Duration answer selected in the quiz.
     * @param supportType Preferred support type selected in the quiz.
     * @param urgencyLevel Normalized urgency level.
     * @param tags Recommended specialization tags for matching.
     */
    public IntakeAssessment(String studentId, String primaryConcern,
                            String duration, String supportType,
                            String urgencyLevel, List<String> tags) {
        this.studentId = studentId;
        this.primaryConcern = primaryConcern;
        this.duration = duration;
        this.supportType = supportType;
        this.urgencyLevel = urgencyLevel;
        this.recommendedSpecializations = tags == null ? new ArrayList<>() : new ArrayList<>(tags);
        this.createdAt = Timestamp.now();
        this.active = true;
    }

    public String getId() { return id; }
    public String getStudentId() { return studentId; }
    public String getPrimaryConcern() { return primaryConcern; }
    public String getDuration() { return duration; }
    public String getSupportType() { return supportType; }
    public String getUrgencyLevel() { return urgencyLevel; }
    public List<String> getRecommendedSpecializations() { return recommendedSpecializations; }
    public String getMatchedCounselorId() { return matchedCounselorId; }
    public String getMatchedCounselorName() { return matchedCounselorName; }
    public Timestamp getCreatedAt() { return createdAt; }
    public boolean isActive() { return active; }

    public void setId(String id) { this.id = id; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public void setPrimaryConcern(String primaryConcern) { this.primaryConcern = primaryConcern; }
    public void setDuration(String duration) { this.duration = duration; }
    public void setSupportType(String supportType) { this.supportType = supportType; }
    public void setUrgencyLevel(String urgencyLevel) { this.urgencyLevel = urgencyLevel; }
    public void setRecommendedSpecializations(List<String> tags) {
        this.recommendedSpecializations = tags == null ? new ArrayList<>() : new ArrayList<>(tags);
    }
    public void setMatchedCounselorId(String matchedCounselorId) {
        this.matchedCounselorId = matchedCounselorId;
    }
    public void setMatchedCounselorName(String matchedCounselorName) {
        this.matchedCounselorName = matchedCounselorName;
    }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public void setActive(boolean active) { this.active = active; }
}
