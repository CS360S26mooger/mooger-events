/*
 * MessageThreadActivity.java
 * Role: Displays the full secure message thread between a counselor and student.
 *       Messages from all past sessions are shown in chronological order with
 *       session-date dividers inserted by SecureMessageAdapter.
 *       Real-time updates via Firestore snapshot listener on the thread sub-collection.
 *
 * Design pattern: Observer (Firestore real-time listener); Repository (data layer).
 * Part of the BetterCAPS counseling platform — Sprint 11 messaging architecture.
 */
package com.example.moogerscouncil;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

/**
 * Thread-based secure message view for pre-session and cross-session communication.
 * Shows all messages between a counselor-student pair, grouped by session date.
 */
public class MessageThreadActivity extends AppCompatActivity {
    public static final String EXTRA_STUDENT_ID = "STUDENT_ID";
    public static final String EXTRA_COUNSELOR_ID = "COUNSELOR_ID";
    public static final String EXTRA_SESSION_DATE = "SESSION_DATE";
    public static final String EXTRA_SESSION_TIME = "SESSION_TIME";
    public static final String EXTRA_OTHER_NAME = "OTHER_NAME";

    private String studentId;
    private String counselorId;
    private String sessionDate;
    private String sessionTime;
    private String currentUid;
    private String senderRole;

    private SecureMessageRepository messageRepository;
    private SecureMessageAdapter adapter;

    private RecyclerView recyclerMessages;
    private TextInputEditText editMessage;
    private MaterialButton btnSendMessage;
    private ProgressBar progressMessages;
    private TextView textEmptyMessages;
    private ListenerRegistration messageListener;
    private int lastMessageCount = 0;
    private boolean initialMessagesRendered = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_thread);

        studentId   = getIntent().getStringExtra(EXTRA_STUDENT_ID);
        counselorId = getIntent().getStringExtra(EXTRA_COUNSELOR_ID);
        sessionDate = getIntent().getStringExtra(EXTRA_SESSION_DATE);
        sessionTime = getIntent().getStringExtra(EXTRA_SESSION_TIME);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        currentUid = user == null ? "" : user.getUid();
        senderRole = currentUid.equals(counselorId) ? UserRole.COUNSELOR : UserRole.STUDENT;

        TextView textMessageTitle = findViewById(R.id.textMessageTitle);
        String otherName = getIntent().getStringExtra(EXTRA_OTHER_NAME);
        textMessageTitle.setText(otherName == null || otherName.isEmpty()
                ? getString(R.string.message_thread_title)
                : getString(R.string.message_thread_title_with_name, otherName));

        recyclerMessages  = findViewById(R.id.recyclerMessages);
        editMessage       = findViewById(R.id.editMessage);
        btnSendMessage    = findViewById(R.id.btnSendMessage);
        progressMessages  = findViewById(R.id.progressMessages);
        textEmptyMessages = findViewById(R.id.textEmptyMessages);

        messageRepository = new SecureMessageRepository();
        adapter = new SecureMessageAdapter(currentUid);
        recyclerMessages.setLayoutManager(new LinearLayoutManager(this));
        recyclerMessages.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnSendMessage.setOnClickListener(v -> sendMessage());
        subscribeToMessages();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messageListener != null) {
            messageListener.remove();
            messageListener = null;
        }
    }

    private void subscribeToMessages() {
        if (counselorId == null || studentId == null
                || counselorId.isEmpty() || studentId.isEmpty()) {
            textEmptyMessages.setVisibility(View.VISIBLE);
            textEmptyMessages.setText(R.string.error_loading_messages);
            return;
        }
        progressMessages.setVisibility(View.VISIBLE);
        messageListener = messageRepository.listenToThread(counselorId, studentId,
                new SecureMessageRepository.OnMessagesLoadedCallback() {
                    @Override
                    public void onSuccess(List<SecureMessage> messages) {
                        progressMessages.setVisibility(View.GONE);
                        maybeNotifyIncomingMessage(messages);
                        adapter.setMessages(messages);
                        textEmptyMessages.setVisibility(messages.isEmpty() ? View.VISIBLE : View.GONE);
                        if (!messages.isEmpty()) {
                            recyclerMessages.scrollToPosition(adapter.getItemCount() - 1);
                        }
                        markRead();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        progressMessages.setVisibility(View.GONE);
                        textEmptyMessages.setVisibility(View.VISIBLE);
                        AppToast.show(MessageThreadActivity.this,
                                R.string.error_loading_messages, AppToast.LENGTH_LONG);
                    }
                });
    }

    private void maybeNotifyIncomingMessage(List<SecureMessage> messages) {
        if (!initialMessagesRendered) {
            initialMessagesRendered = true;
            lastMessageCount = messages.size();
            return;
        }
        if (messages.size() <= lastMessageCount) {
            lastMessageCount = messages.size();
            return;
        }
        SecureMessage latest = messages.get(messages.size() - 1);
        lastMessageCount = messages.size();
        if (latest.getSenderId() != null && !latest.getSenderId().equals(currentUid)) {
            AppToast.show(this, R.string.new_message_received, AppToast.LENGTH_SHORT);
        }
    }

    private void markRead() {
        if (counselorId == null || studentId == null) return;
        messageRepository.markThreadRead(counselorId, studentId, senderRole,
                new SecureMessageRepository.OnMessageActionCallback() {
                    @Override public void onSuccess() {}
                    @Override public void onFailure(Exception e) {}
                });
    }

    private void sendMessage() {
        String text = editMessage.getText() == null
                ? "" : editMessage.getText().toString().trim();
        if (text.isEmpty()) {
            editMessage.setError(getString(R.string.error_required));
            return;
        }
        btnSendMessage.setEnabled(false);
        messageRepository.sendMessage(
                counselorId, studentId,
                sessionDate != null ? sessionDate : "",
                sessionTime != null ? sessionTime : "",
                currentUid, senderRole, text,
                new SecureMessageRepository.OnMessageActionCallback() {
                    @Override
                    public void onSuccess() {
                        btnSendMessage.setEnabled(true);
                        editMessage.setText("");
                    }

                    @Override
                    public void onFailure(Exception e) {
                        btnSendMessage.setEnabled(true);
                        AppToast.show(MessageThreadActivity.this,
                                R.string.error_sending_message, AppToast.LENGTH_LONG);
                    }
                });
    }
}
