package com.example.moogerscouncil;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository for appointment-linked in-app secure messages.
 */
public class SecureMessageRepository {
    private final FirebaseFirestore db;
    private final CollectionReference messagesCollection;
    private final CollectionReference appointmentsCollection;

    public SecureMessageRepository() {
        db = FirebaseFirestore.getInstance();
        messagesCollection = db.collection("secureMessages");
        appointmentsCollection = db.collection("appointments");
    }

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

    public void getMessagesForAppointment(String appointmentId,
                                          OnMessagesLoadedCallback callback) {
        messagesCollection
                .whereEqualTo("appointmentId", appointmentId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<SecureMessage> messages = new ArrayList<>();
                    snapshot.getDocuments().forEach(doc -> {
                        SecureMessage message = doc.toObject(SecureMessage.class);
                        if (message != null) {
                            message.setId(doc.getId());
                            messages.add(message);
                        }
                    });
                    Collections.sort(messages, (a, b) -> {
                        Timestamp ta = a.getCreatedAt();
                        Timestamp tb = b.getCreatedAt();
                        if (ta == null && tb == null) return 0;
                        if (ta == null) return -1;
                        if (tb == null) return 1;
                        return ta.compareTo(tb);
                    });
                    callback.onSuccess(messages);
                })
                .addOnFailureListener(callback::onFailure);
    }

    public ListenerRegistration listenForMessagesForAppointment(String appointmentId,
                                                                OnMessagesLoadedCallback callback) {
        return messagesCollection
                .whereEqualTo("appointmentId", appointmentId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        callback.onFailure(error);
                        return;
                    }
                    if (snapshot == null) return;
                    List<SecureMessage> messages = new ArrayList<>();
                    snapshot.getDocuments().forEach(doc -> {
                        SecureMessage message = doc.toObject(SecureMessage.class);
                        if (message != null) {
                            message.setId(doc.getId());
                            messages.add(message);
                        }
                    });
                    Collections.sort(messages, (a, b) -> {
                        Timestamp ta = a.getCreatedAt();
                        Timestamp tb = b.getCreatedAt();
                        if (ta == null && tb == null) return 0;
                        if (ta == null) return -1;
                        if (tb == null) return 1;
                        return ta.compareTo(tb);
                    });
                    callback.onSuccess(messages);
                });
    }

    public void hasUnreadMessagesForAppointment(String appointmentId, String receiverId,
                                                OnUnreadStatusCallback callback) {
        if (appointmentId == null || appointmentId.isEmpty()
                || receiverId == null || receiverId.isEmpty()) {
            callback.onResult(false);
            return;
        }
        messagesCollection
                .whereEqualTo("appointmentId", appointmentId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    boolean hasUnread = false;
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                        String docReceiver = doc.getString("receiverId");
                        Boolean read = doc.getBoolean("read");
                        if (receiverId.equals(docReceiver) && !Boolean.TRUE.equals(read)) {
                            hasUnread = true;
                            break;
                        }
                    }
                    callback.onResult(hasUnread);
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void sendMessage(SecureMessage message, OnMessageActionCallback callback) {
        DocumentReference messageRef = messagesCollection.document();
        message.setId(messageRef.getId());
        message.setCreatedAt(Timestamp.now());
        message.setRead(false);
        messageRef.set(message)
                .addOnSuccessListener(unused -> {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("hasPreSessionMessage", true);
                    updates.put("lastMessageAt", Timestamp.now());
                    appointmentsCollection.document(message.getAppointmentId())
                            .set(updates, SetOptions.merge())
                            .addOnSuccessListener(update -> callback.onSuccess())
                            .addOnFailureListener(callback::onFailure);
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void markMessagesRead(String appointmentId, String receiverId,
                                 OnMessageActionCallback callback) {
        messagesCollection
                .whereEqualTo("appointmentId", appointmentId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    WriteBatch batch = db.batch();
                    int[] count = {0};
                    snapshot.getDocuments().forEach(doc -> {
                        String docReceiver = doc.getString("receiverId");
                        Boolean read = doc.getBoolean("read");
                        if (receiverId.equals(docReceiver) && !Boolean.TRUE.equals(read)) {
                            batch.update(doc.getReference(), "read", true);
                            count[0]++;
                        }
                    });
                    if (count[0] == 0) {
                        callback.onSuccess();
                        return;
                    }
                    batch.commit()
                            .addOnSuccessListener(unused -> callback.onSuccess())
                            .addOnFailureListener(callback::onFailure);
                })
                .addOnFailureListener(callback::onFailure);
    }
}
