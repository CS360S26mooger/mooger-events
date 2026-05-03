/*
 * CounselorWaitlistAdapter.java
 * Role: RecyclerView adapter for the counselor-side FIFO waitlist queue.
 *       Shows student name, preferred dates, time window, note, and a
 *       "Create Slot for Student" button.
 *
 * Design pattern: Adapter (RecyclerView.Adapter), used by CounselorWaitlistActivity.
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

import java.util.List;
import java.util.Map;

/**
 * Binds FIFO-ordered {@link WaitlistEntry} objects to {@code item_waitlist_counselor} rows.
 *
 * <p>Student display names are pre-resolved by {@link CounselorWaitlistActivity} and
 * passed in as a {@code studentNames} map (keyed by studentId) so this adapter stays
 * free of Firestore calls.</p>
 */
public class CounselorWaitlistAdapter
        extends RecyclerView.Adapter<CounselorWaitlistAdapter.ViewHolder> {

    /** Callback for the "Create Slot for Student" button. */
    public interface OnCreateSlotClickListener {
        void onCreateSlotClick(WaitlistEntry entry);
    }

    private final Context context;
    private final List<WaitlistEntry> entries;
    private final Map<String, String> studentNames;
    private final OnCreateSlotClickListener slotListener;

    /**
     * @param context      The hosting Activity context.
     * @param entries      FIFO-sorted active waitlist entries.
     * @param studentNames Map of studentId → display name for quick lookup.
     * @param slotListener Callback invoked when counselor taps "Create Slot for Student".
     */
    public CounselorWaitlistAdapter(Context context, List<WaitlistEntry> entries,
                                    Map<String, String> studentNames,
                                    OnCreateSlotClickListener slotListener) {
        this.context = context;
        this.entries = entries;
        this.studentNames = studentNames;
        this.slotListener = slotListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_waitlist_counselor, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WaitlistEntry entry = entries.get(position);

        String name = studentNames.get(entry.getStudentId());
        holder.textStudentName.setText(name != null ? name : entry.getStudentId());

        holder.textQueuePosition.setText(String.valueOf(position + 1));

        List<String> dates = entry.getPreferredDates();
        if (dates != null && !dates.isEmpty()) {
            holder.textPreferredDates.setText(context.getString(
                    R.string.waitlist_dates_label, TextUtils.join(", ", dates)));
            holder.textPreferredDates.setVisibility(View.VISIBLE);
        } else {
            holder.textPreferredDates.setVisibility(View.GONE);
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

        holder.buttonCreateSlot.setOnClickListener(v -> slotListener.onCreateSlotClick(entry));
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textStudentName;
        TextView textQueuePosition;
        TextView textPreferredDates;
        TextView textTimeWindow;
        TextView textNote;
        Button buttonCreateSlot;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textStudentName = itemView.findViewById(R.id.textStudentName);
            textQueuePosition = itemView.findViewById(R.id.textQueuePosition);
            textPreferredDates = itemView.findViewById(R.id.textPreferredDates);
            textTimeWindow = itemView.findViewById(R.id.textTimeWindow);
            textNote = itemView.findViewById(R.id.textStudentNote);
            buttonCreateSlot = itemView.findViewById(R.id.buttonCreateSlot);
        }
    }
}
