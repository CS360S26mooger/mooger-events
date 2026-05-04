/*
 * SecureMessageAdapter.java
 * Role: RecyclerView adapter for the counselor-student message thread.
 *       Displays message bubbles (outgoing pink / incoming grey) and inserts
 *       session-date divider chips whenever the sessionDate changes between
 *       consecutive messages, grouping the full history by appointment session.
 *
 * Design pattern: Adapter pattern (RecyclerView); two view types.
 * Part of the BetterCAPS counseling platform — Sprint 11 messaging architecture.
 */
package com.example.moogerscouncil;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for the full counselor-student message thread.
 * Inserts {@code TYPE_DATE_DIVIDER} rows between messages from different sessions.
 */
public class SecureMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_MESSAGE  = 0;
    private static final int TYPE_DIVIDER  = 1;

    /** Wrapper that holds either a real message or a synthetic session divider. */
    private static class ListItem {
        final SecureMessage message;  // non-null for TYPE_MESSAGE
        final String dividerLabel;    // non-null for TYPE_DIVIDER

        ListItem(SecureMessage msg) {
            this.message = msg;
            this.dividerLabel = null;
        }
        ListItem(String label) {
            this.message = null;
            this.dividerLabel = label;
        }
    }

    private final List<ListItem> items = new ArrayList<>();
    private final String currentUid;

    public SecureMessageAdapter(String currentUid) {
        this.currentUid = currentUid;
    }

    /**
     * Replaces the message list and re-inserts session dividers wherever
     * the sessionDate changes between consecutive messages.
     *
     * @param newMessages Chronologically sorted message list from the repository.
     */
    public void setMessages(List<SecureMessage> newMessages) {
        items.clear();
        if (newMessages == null || newMessages.isEmpty()) {
            notifyDataSetChanged();
            return;
        }
        String lastDate = null;
        for (SecureMessage msg : newMessages) {
            String msgDate = msg.getSessionDate();
            if (msgDate != null && !msgDate.equals(lastDate)) {
                items.add(new ListItem(buildDividerLabel(msgDate, msg.getSessionTime())));
                lastDate = msgDate;
            }
            items.add(new ListItem(msg));
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).message == null ? TYPE_DIVIDER : TYPE_MESSAGE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_DIVIDER) {
            View view = inflater.inflate(R.layout.item_message_date_divider, parent, false);
            return new DividerViewHolder(view);
        }
        View view = inflater.inflate(R.layout.item_secure_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ListItem item = items.get(position);
        if (holder instanceof DividerViewHolder) {
            ((DividerViewHolder) holder).textDivider.setText(item.dividerLabel);
        } else if (holder instanceof MessageViewHolder) {
            bindMessage((MessageViewHolder) holder, item.message);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private void bindMessage(MessageViewHolder holder, SecureMessage message) {
        boolean sentByMe = currentUid != null && currentUid.equals(message.getSenderId());
        holder.textMessageSender.setText(sentByMe
                ? holder.itemView.getContext().getString(R.string.message_sender_you)
                : senderLabel(holder, message.getSenderRole()));
        holder.textMessageBody.setText(message.getMessageText());
        holder.textMessageTime.setText(formatTimestamp(message));
    }

    private String senderLabel(MessageViewHolder holder, String senderRole) {
        if (UserRole.COUNSELOR.equals(senderRole))
            return holder.itemView.getContext().getString(R.string.message_sender_counselor);
        if (UserRole.STUDENT.equals(senderRole))
            return holder.itemView.getContext().getString(R.string.message_sender_student);
        return holder.itemView.getContext().getString(R.string.message_sender_unknown);
    }

    private String formatTimestamp(SecureMessage message) {
        if (message.getCreatedAt() == null) return "";
        return new SimpleDateFormat("HH:mm", Locale.US)
                .format(message.getCreatedAt().toDate());
    }

    /** Formats a "yyyy-MM-dd" + "HH:mm" pair into "EEE, MMM d · HH:mm". */
    private static String buildDividerLabel(String date, String time) {
        String datePart = date;
        try {
            Date parsed = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date);
            datePart = new SimpleDateFormat("EEE, MMM d", Locale.US).format(parsed);
        } catch (ParseException ignored) {}
        if (time == null || time.isEmpty()) return datePart;
        return datePart + " · " + time;
    }

    // ── ViewHolders ───────────────────────────────────────────────────────────

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView textMessageSender;
        TextView textMessageBody;
        TextView textMessageTime;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textMessageSender = itemView.findViewById(R.id.textMessageSender);
            textMessageBody   = itemView.findViewById(R.id.textMessageBody);
            textMessageTime   = itemView.findViewById(R.id.textMessageTime);
        }
    }

    static class DividerViewHolder extends RecyclerView.ViewHolder {
        TextView textDivider;

        DividerViewHolder(@NonNull View itemView) {
            super(itemView);
            textDivider = itemView.findViewById(R.id.textMessageDateDivider);
        }
    }
}
