package com.example.moogerscouncil;

import com.google.firebase.Timestamp;

/**
 * Appointment-linked in-app message used for secure pre-session communication.
 */
public class SecureMessage {
    private String id;
    private String appointmentId;
    private String counselorId;
    private String studentId;
    private String senderId;
    private String receiverId;
    private String senderRole;
    private String messageText;
    private Timestamp createdAt;
    private boolean read;

    /** Required empty constructor for Firestore. */
    public SecureMessage() {}

    public SecureMessage(String appointmentId, String counselorId, String studentId,
                         String senderId, String receiverId, String senderRole,
                         String messageText) {
        this.appointmentId = appointmentId;
        this.counselorId = counselorId;
        this.studentId = studentId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.senderRole = senderRole;
        this.messageText = messageText;
        this.createdAt = Timestamp.now();
        this.read = false;
    }

    public String getId() { return id; }
    public String getAppointmentId() { return appointmentId; }
    public String getCounselorId() { return counselorId; }
    public String getStudentId() { return studentId; }
    public String getSenderId() { return senderId; }
    public String getReceiverId() { return receiverId; }
    public String getSenderRole() { return senderRole; }
    public String getMessageText() { return messageText; }
    public Timestamp getCreatedAt() { return createdAt; }
    public boolean isRead() { return read; }

    public void setId(String id) { this.id = id; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }
    public void setCounselorId(String counselorId) { this.counselorId = counselorId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }
    public void setSenderRole(String senderRole) { this.senderRole = senderRole; }
    public void setMessageText(String messageText) { this.messageText = messageText; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public void setRead(boolean read) { this.read = read; }
}
