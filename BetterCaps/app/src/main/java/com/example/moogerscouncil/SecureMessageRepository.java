/*
 * SecureMessageRepository.java
 * Role: Repository for counselor-student thread-based in-app messaging.
 *       All messages are stored at messageThreads/{counselorId_studentId}/messages/{msgId}.
 *       A thread document at messageThreads/{counselorId_studentId} tracks lastMessageAt
 *       and per-role unread counters, enabling O(1) path lookups for any pair.
 *
 * Design pattern: Repository (data layer); Observer (real-time Firestore listener).
 * Part of the BetterCAPS counseling platform — Sprint 11 messaging architecture.
 */
package com.example.moogerscouncil;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository for thread-based secure in-app messages.
 * Firestore path: messageThreads/{counselorId_studentId}/messages/{messageId}.
 */
public class SecureMessageRepository {
    private final FirebaseFirestore db;

    public SecureMessageRepository() {
        db = FirebaseFirestore.getInstance();
    }

    // ── Callbacks ──────────────────────────────────────────────────────────────

    public interface OnMessagesLoadedCallback {
        void onSuccess(List<SecureMessage> messages);
        void onFailure(Exception e);
    }

    public interface OnMessageActionCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface OnUnreadStatusCallback {
        void onResult(boolean hasUnread);
        void onFailure(Exception e);
    }

    // ── Path helper ────────────────────────────────────────────────────────────

    private CollectionReference messagesFor(String counselorId, String studentId) {
        return db.collection("messageThreads")
                .document(counselorId + "_" + studentId)
                .collection("messages");
    }

    private DocumentReference threadRef(String counselorId, String studentId) {
        return db.collection("messageThreads")
                .document(counselorId + "_" + studentId);
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    /**
     * One-time fetch of all messages for a counselor-student pair, ordered by createdAt.
     *
     * @param counselorId Counselor UID.
     * @param studentId   Student UID.
     * @param callback    Delivers sorted message list or error.
     */
    public void getThreadMessages(String counselorId, String studentId,
                                  OnMessagesLoadedCallback callback) {
        messagesFor(counselorId, studentId)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<SecureMessage> messages = new ArrayList<>();
                    snapshot.getDocuments().forEach(doc -> {
                        SecureMessage msg = doc.toObject(SecureMessage.class);
                        if (msg != null) {
                            msg.setId(doc.getId());
                            messages.add(msg);
                        }
                    });
                    callback.onSuccess(messages);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Attaches a real-time Firestore listener for a counselor-student thread.
     * Delivers all messages ordered by createdAt on every change.
     *
     * @param counselorId Counselor UID.
     * @param studentId   Student UID.
     * @param callback    Delivers sorted message list on each update or error.
     * @return {@link ListenerRegistration} — caller must remove() on destroy.
     */
    public ListenerRegistration listenToThread(String counselorId, String studentId,
                                               OnMessagesLoadedCallback callback) {
        return messagesFor(counselorId, studentId)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        callback.onFailure(error);
                        return;
                    }
                    if (snapshot == null) return;
                    List<SecureMessage> messages = new ArrayList<>();
                    snapshot.getDocuments().forEach(doc -> {
                        SecureMessage msg = doc.toObject(SecureMessage.class);
                        if (msg != null) {
                            msg.setId(doc.getId());
                            messages.add(msg);
                        }
                    });
                    callback.onSuccess(messages);
                });
    }

    /**
     * Checks whether a thread has any unread messages for the given reader role.
     *
     * @param counselorId Counselor UID.
     * @param studentId   Student UID.
     * @param readerRole  {@link UserRole#COUNSELOR} or {@link UserRole#STUDENT} — the reader's role.
     * @param callback    Returns true if any unread message exists for that role.
     */
    public void hasUnreadInThread(String counselorId, String studentId,
                                  String readerRole, OnUnreadStatusCallback callback) {
        if (counselorId == null || studentId == null) {
            callback.onResult(false);
            return;
        }
        // Messages are unread for a role when senderId is from the opposite role and read == false.
        String senderRole = UserRole.COUNSELOR.equals(readerRole)
                ? UserRole.STUDENT : UserRole.COUNSELOR;
        messagesFor(counselorId, studentId)
                .whereEqualTo("senderRole", senderRole)
                .whereEqualTo("read", false)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> callback.onResult(!snapshot.isEmpty()))
                .addOnFailureListener(callback::onFailure);
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    /**
     * Sends a message into the counselor-student thread and updates thread metadata.
     *
     * @param counselorId  Counselor UID (canonical thread key, always counselor first).
     * @param studentId    Student UID.
     * @param sessionDate  Appointment date ("yyyy-MM-dd") for session-divider grouping.
     * @param sessionTime  Appointment time ("HH:mm") for session-divider label.
     * @param senderId     UID of the sender.
     * @param senderRole   {@link UserRole#COUNSELOR} or {@link UserRole#STUDENT}.
     * @param text         Message body text.
     * @param callback     Called on success or failure.
     */
    public void sendMessage(String counselorId, String studentId,
                            String sessionDate, String sessionTime,
                            String senderId, String senderRole,
                            String text, OnMessageActionCallback callback) {
        SecureMessage message = new SecureMessage(
                counselorId, studentId, senderId, senderRole,
                sessionDate, sessionTime, text);

        DocumentReference msgRef = messagesFor(counselorId, studentId).document();
        message.setId(msgRef.getId());

        String counterField = UserRole.COUNSELOR.equals(senderRole)
                ? "unreadByStudent" : "unreadByCounselor";

        Map<String, Object> threadUpdate = new HashMap<>();
        threadUpdate.put("counselorId", counselorId);
        threadUpdate.put("studentId", studentId);
        threadUpdate.put("lastMessageAt", Timestamp.now());

        WriteBatch batch = db.batch();
        batch.set(msgRef, message);
        batch.set(threadRef(counselorId, studentId), threadUpdate, SetOptions.merge());
        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Marks all messages in the thread that were sent by the opposite role as read.
     * Decrements the thread-level unread counter for the reader's role.
     *
     * @param counselorId Counselor UID.
     * @param studentId   Student UID.
     * @param readerRole  The role of the person reading (whose unread count to clear).
     * @param callback    Called on success or failure.
     */
    public void markThreadRead(String counselorId, String studentId,
                               String readerRole, OnMessageActionCallback callback) {
        String senderRole = UserRole.COUNSELOR.equals(readerRole)
                ? UserRole.STUDENT : UserRole.COUNSELOR;
        messagesFor(counselorId, studentId)
                .whereEqualTo("senderRole", senderRole)
                .whereEqualTo("read", false)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        callback.onSuccess();
                        return;
                    }
                    WriteBatch batch = db.batch();
                    snapshot.getDocuments().forEach(doc -> batch.update(doc.getReference(), "read", true));
                    String counterField = UserRole.COUNSELOR.equals(readerRole)
                            ? "unreadByCounselor" : "unreadByStudent";
                    batch.update(threadRef(counselorId, studentId), counterField, 0);
                    batch.commit()
                            .addOnSuccessListener(unused -> callback.onSuccess())
                            .addOnFailureListener(callback::onFailure);
                })
                .addOnFailureListener(callback::onFailure);
    }
}
