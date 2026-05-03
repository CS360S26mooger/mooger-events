package com.example.moogerscouncil;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Displays a student's session note history as timestamped cards.
 * Each card is tappable and opens SessionNoteActivity for viewing/editing.
 * The timestamp shown is the appointment's own date+time (not the note-save time)
 * so the counselor can see which session a note belongs to at a glance.
 */
public class SessionNoteHistoryAdapter
        extends RecyclerView.Adapter<SessionNoteHistoryAdapter.ViewHolder> {

    private final List<SessionNote> notes;

    public SessionNoteHistoryAdapter(List<SessionNote> notes) {
        this.notes = notes;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_session_note_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SessionNote note = notes.get(position);

        holder.templateLabel.setText(NoteTemplate.getDisplayName(note.getTemplateKey()));
        holder.timestamp.setText(buildTimestamp(note));

        String raw = note.getNoteText() != null ? note.getNoteText() : "";
        String preview = raw
                .replace("[CONCERN]", "")
                .replace("[SUMMARY]", "")
                .replace("[INTERVENTIONS]", "")
                .replace("[PLAN]", "")
                .replaceAll("\n{2,}", "\n")
                .trim();
        holder.preview.setText(preview.isEmpty() ? "—" : preview);

        // Tap opens the note in SessionNoteActivity for viewing / editing
        holder.itemView.setOnClickListener(v -> {
            Context ctx = v.getContext();
            ctx.startActivity(SessionNoteActivity.newIntent(
                    ctx,
                    note.getAppointmentId(),
                    note.getCounselorId(),
                    note.getStudentId(),
                    note.getAppointmentDate(),
                    note.getAppointmentTime()));
        });
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    /**
     * Prefers the stored appointment date+time so the counselor sees which session
     * the note is for. Falls back to createdAt for notes saved before this field was added.
     */
    private String buildTimestamp(SessionNote note) {
        String apptDate = note.getAppointmentDate();
        if (apptDate != null && !apptDate.isEmpty()) {
            try {
                Date parsed = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(apptDate);
                String formatted = new SimpleDateFormat("EEE, MMM d", Locale.US).format(parsed);
                String time = note.getAppointmentTime();
                if (time != null && !time.isEmpty()) formatted += " · " + time;
                return "Session: " + formatted;
            } catch (Exception ignored) {}
        }
        // Legacy fallback: use note-save timestamp
        if (note.getCreatedAt() != null) {
            Date d = note.getCreatedAt().toDate();
            return new SimpleDateFormat("EEE, MMM d · HH:mm", Locale.US).format(d);
        }
        return "—";
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView timestamp, templateLabel, preview;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            timestamp     = itemView.findViewById(R.id.noteTimestamp);
            templateLabel = itemView.findViewById(R.id.noteTemplateLabel);
            preview       = itemView.findViewById(R.id.notePreview);
        }
    }
}
