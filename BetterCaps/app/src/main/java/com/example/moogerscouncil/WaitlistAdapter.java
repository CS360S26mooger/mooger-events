/*
 * WaitlistAdapter.java
 * Role: RecyclerView adapter for the student-side active waitlist list.
 *       Displays counselor name (resolved by caller), preferred dates, time window,
 *       and a cancel button for each ACTIVE entry.
 *
 * Design pattern: Adapter (RecyclerView.Adapter), used by StudentWaitlistActivity.
 * Part of the BetterCAPS counseling platform.
 */
package com.example.moogerscouncil;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Adapter that binds {@link WaitlistEntry} objects to {@code item_waitlist_student} rows.
 *
 * <p>Counselor display names are pre-resolved by {@link StudentWaitlistActivity} and
 * passed in as a {@code counselorNames} map (keyed by counselorId) so this adapter
 * stays free of Firestore calls.</p>
 */
public class WaitlistAdapter extends RecyclerView.Adapter<WaitlistAdapter.ViewHolder> {

    /** Callback interface for cancel button taps. */
    public interface OnCancelClickListener {
        void onCancelClick(WaitlistEntry entry);
    }

    private final Context context;
    private final List<WaitlistEntry> entries;
    private final Map<String, String> counselorNames;
    private final OnCancelClickListener cancelListener;

    /**
     * @param context        The hosting Activity context.
     * @param entries        The waitlist entries to display.
     * @param counselorNames Map of counselorId → display name for quick lookup.
     * @param cancelListener Callback invoked when the student taps "Cancel Request".
     */
    public WaitlistAdapter(Context context, List<WaitlistEntry> entries,
                           Map<String, String> counselorNames,
                           OnCancelClickListener cancelListener) {
        this.context = context;
        this.entries = entries;
        this.counselorNames = counselorNames;
        this.cancelListener = cancelListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_waitlist_student, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WaitlistEntry entry = entries.get(position);

        String name = counselorNames.get(entry.getCounselorId());
        holder.textCounselorName.setText(name != null ? name : entry.getCounselorId());

        holder.textStatus.setText(statusLabel(entry.getStatus()));

        List<String> dates = entry.getPreferredDates();
        if (dates != null && !dates.isEmpty()) {
            holder.textDates.setText(context.getString(
                    R.string.waitlist_dates_label, TextUtils.join(", ", dates)));
            holder.textDates.setVisibility(View.VISIBLE);
        } else {
            holder.textDates.setVisibility(View.GONE);
        }

        if (entry.getPreferredStartTime() != null && entry.getPreferredEndTime() != null) {
            holder.textTimeWindow.setText(context.getString(
                    R.string.waitlist_time_window_label,
                    entry.getPreferredStartTime(), entry.getPreferredEndTime()));
            holder.textTimeWindow.setVisibility(View.VISIBLE);
        } else {
            holder.textTimeWindow.setVisibility(View.GONE);
        }

        String note = entry.getNote();
        if (note != null && !note.isEmpty()) {
            holder.textNote.setText("Note: " + note);
            holder.textNote.setVisibility(View.VISIBLE);
        } else {
            holder.textNote.setVisibility(View.GONE);
        }

        if (entry.getRequestedAt() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
            holder.textRequestedAt.setText(context.getString(
                    R.string.waitlist_requested_label,
                    sdf.format(entry.getRequestedAt().toDate())));
        } else {
            holder.textRequestedAt.setText("");
        }

        holder.buttonCancel.setOnClickListener(v -> cancelListener.onCancelClick(entry));
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    private String statusLabel(String status) {
        if (WaitlistEntry.STATUS_RESOLVED.equals(status)
                || WaitlistEntry.STATUS_BOOKED.equals(status)
                || WaitlistEntry.STATUS_OFFERED.equals(status)) {
            return context.getString(R.string.waitlist_status_resolved);
        }
        if (WaitlistEntry.STATUS_CANCELLED.equals(status)
                || WaitlistEntry.STATUS_EXPIRED.equals(status)) {
            return context.getString(R.string.waitlist_status_cancelled);
        }
        return context.getString(R.string.waitlist_status_active);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textCounselorName;
        TextView textStatus;
        TextView textDates;
        TextView textTimeWindow;
        TextView textNote;
        TextView textRequestedAt;
        Button buttonCancel;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textCounselorName = itemView.findViewById(R.id.textWaitlistCounselorName);
            textStatus = itemView.findViewById(R.id.textWaitlistStatus);
            textDates = itemView.findViewById(R.id.textWaitlistDates);
            textTimeWindow = itemView.findViewById(R.id.textWaitlistTimeWindow);
            textNote = itemView.findViewById(R.id.textWaitlistNote);
            textRequestedAt = itemView.findViewById(R.id.textWaitlistRequestedAt);
            buttonCancel = itemView.findViewById(R.id.buttonCancelWaitlist);
        }
    }
}
