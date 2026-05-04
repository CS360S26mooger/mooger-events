package com.example.moogerscouncil;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for appointment-linked secure messages.
 */
public class SecureMessageAdapter extends RecyclerView.Adapter<SecureMessageAdapter.ViewHolder> {
    private final List<SecureMessage> messages = new ArrayList<>();
    private final String currentUid;

    public SecureMessageAdapter(String currentUid) {
        this.currentUid = currentUid;
    }

    public void setMessages(List<SecureMessage> newMessages) {
        messages.clear();
        if (newMessages != null) messages.addAll(newMessages);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_secure_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SecureMessage message = messages.get(position);
        boolean sentByMe = currentUid != null && currentUid.equals(message.getSenderId());
        holder.textMessageSender.setText(sentByMe
                ? holder.itemView.getContext().getString(R.string.message_sender_you)
                : senderLabel(holder, message.getSenderRole()));
        holder.textMessageBody.setText(message.getMessageText());
        holder.textMessageTime.setText(formatTimestamp(message));
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    private String senderLabel(ViewHolder holder, String senderRole) {
        if (UserRole.COUNSELOR.equals(senderRole)) {
            return holder.itemView.getContext().getString(R.string.message_sender_counselor);
        }
        if (UserRole.STUDENT.equals(senderRole)) {
            return holder.itemView.getContext().getString(R.string.message_sender_student);
        }
        return holder.itemView.getContext().getString(R.string.message_sender_unknown);
    }

    private String formatTimestamp(SecureMessage message) {
        if (message.getCreatedAt() == null) return "";
        return new SimpleDateFormat("MMM d, HH:mm", Locale.US)
                .format(message.getCreatedAt().toDate());
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textMessageSender;
        TextView textMessageBody;
        TextView textMessageTime;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textMessageSender = itemView.findViewById(R.id.textMessageSender);
            textMessageBody = itemView.findViewById(R.id.textMessageBody);
            textMessageTime = itemView.findViewById(R.id.textMessageTime);
        }
    }
}
