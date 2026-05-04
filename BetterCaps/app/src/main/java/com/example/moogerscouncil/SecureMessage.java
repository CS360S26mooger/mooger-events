package com.example.moogerscouncil;

import com.google.firebase.Timestamp;

/**
 * Thread-keyed in-app message for secure pre-session and cross-session communication.
 * Stored at messageThreads/{counselorId_studentId}/messages/{messageId}.
 * The sessionDate / sessionTime fields drive date-divider grouping in the UI.
 */
public class SecureMessage {
    private String id;
    private String counselorId;
    private String studentId;
    private String senderId;
    private String senderRole;
    private String messageText;
    private String sessionDate;
    private String sessionTime;
    private Timestamp createdAt;
    private boolean read;

    /** Required empty constructor for Firestore. */
    public SecureMessage() {}

    /**
     * Creates a new outgoing message for a counselor-student thread.
     *
     * @param counselorId  Firestore UID of the counselor.
     * @param studentId    Firestore UID of the student.
     * @param senderId     UID of the user sending this message.
     * @param senderRole   {@link UserRole#COUNSELOR} or {@link UserRole#STUDENT}.
     * @param sessionDate  The appointment date ("yyyy-MM-dd") this message belongs to.
     * @param sessionTime  The appointment time ("HH:mm") this message belongs to.
     * @param messageText  The message body.
     */
    public SecureMessage(String counselorId, String studentId,
                         String senderId, String senderRole,
                         String sessionDate, String sessionTime,
                         String messageText) {
        this.counselorId = counselorId;
        this.studentId = studentId;
        this.senderId = senderId;
        this.senderRole = senderRole;
        this.sessionDate = sessionDate;
        this.sessionTime = sessionTime;
        this.messageText = messageText;
        this.createdAt = Timestamp.now();
        this.read = false;
    }

    /** Returns the canonical thread document ID: counselorId + "_" + studentId. */
    public String getThreadId() {
        return counselorId + "_" + studentId;
    }

    public String getId() { return id; }
    public String getCounselorId() { return counselorId; }
    public String getStudentId() { return studentId; }
    public String getSenderId() { return senderId; }
    public String getSenderRole() { return senderRole; }
    public String getMessageText() { return messageText; }
    public String getSessionDate() { return sessionDate; }
    public String getSessionTime() { return sessionTime; }
    public Timestamp getCreatedAt() { return createdAt; }
    public boolean isRead() { return read; }

    public void setId(String id) { this.id = id; }
    public void setCounselorId(String counselorId) { this.counselorId = counselorId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public void setSenderRole(String senderRole) { this.senderRole = senderRole; }
    public void setMessageText(String messageText) { this.messageText = messageText; }
    public void setSessionDate(String sessionDate) { this.sessionDate = sessionDate; }
    public void setSessionTime(String sessionTime) { this.sessionTime = sessionTime; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public void setRead(boolean read) { this.read = read; }
}
