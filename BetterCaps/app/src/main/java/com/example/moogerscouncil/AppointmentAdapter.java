/*
 * AppointmentAdapter.java
 * Role: RecyclerView adapter for the counselor's appointment list.
 *       Looks up student names via UserRepository, applies status badge styling
 *       (CONFIRMED/COMPLETED/CANCELLED/NO_SHOW), and wires the No-Show button
 *       to AppointmentRepository.updateAppointmentStatus().
 *
 * Design pattern: Adapter pattern (RecyclerView); Repository pattern for data ops.
 * Part of the BetterCAPS counseling platform.
 */
package com.example.moogerscouncil;

import android.content.res.ColorStateList;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Locale;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for the counselor's appointment dashboard cards.
 *
 * <p>Each card shows:
 * <ul>
 *   <li>Student name (async lookup via {@link UserRepository})</li>
 *   <li>Date and time</li>
 *   <li>Status badge (color-coded: green/grey/red)</li>
 *   <li>No-Show button wired to {@link AppointmentRepository}</li>
 * </ul>
 * </p>
 */
public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.ViewHolder> {

    private List<Appointment> appointments;
    private final Context context;
    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;

    /**
     * Creates a new adapter.
     *
     * @param context      Activity context for inflation and toasts.
     * @param appointments Initial appointment list (may be replaced via {@link #setData}).
     */
    public AppointmentAdapter(Context context, List<Appointment> appointments) {
        this.context = context;
        this.appointments = appointments;
        this.userRepository = new UserRepository();
        this.appointmentRepository = new AppointmentRepository();
    }

    /**
     * Replaces the current appointment list and notifies the adapter.
     * Called by {@link CounselorDashboardActivity} when the tab changes.
     *
     * @param newList The filtered appointment list for the current tab.
     */
    public void setData(List<Appointment> newList) {
        this.appointments = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_appointment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Appointment apt = appointments.get(position);

        // Async student name lookup
        holder.studentName.setText("Loading…");
        if (apt.getStudentId() != null) {
            userRepository.getUserName(apt.getStudentId(),
                    name -> holder.studentName.setText(name));
        } else {
            holder.studentName.setText("Unknown");
        }

        holder.sessionTime.setText(normalizeTime(apt.getTime()));
        holder.sessionDate.setText(formatDate(apt.getDate()));

        // Status badge styling
        applyStatusBadge(holder, apt.getStatus());

        // No-Show button
        holder.noShowButton.setOnClickListener(v ->
                markNoShow(apt, holder, position));

        // Placeholder action buttons (Phase 4)
        holder.joinButton.setOnClickListener(v ->
                Toast.makeText(context, "Video call coming in Phase 4", Toast.LENGTH_SHORT).show());
        holder.crisisButton.setOnClickListener(v ->
                new androidx.appcompat.app.AlertDialog.Builder(context)
                        .setTitle("Crisis Alert")
                        .setMessage("Escalate this student to campus emergency services?")
                        .setPositiveButton("Escalate", null)
                        .setNegativeButton("Cancel", null)
                        .show());
        holder.profileButton.setOnClickListener(v ->
                Toast.makeText(context, "Student profile coming in Phase 4", Toast.LENGTH_SHORT).show());
        holder.notesButton.setOnClickListener(v ->
                Toast.makeText(context, "Session notes coming in Phase 4", Toast.LENGTH_SHORT).show());
    }

    /**
     * Applies color-coded status badge styling to the appointment card.
     * CONFIRMED = green; COMPLETED = muted grey; CANCELLED / NO_SHOW = red with strikethrough.
     *
     * @param holder The ViewHolder for the current card.
     * @param status The appointment status string.
     */
    private void applyStatusBadge(ViewHolder holder, String status) {
        holder.itemView.setAlpha(1.0f);
        holder.studentName.setPaintFlags(
                holder.studentName.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);

        if (status == null) return;

        String label;
        int textColor;
        int bgColor;
        switch (status) {
            case "CONFIRMED":
                label     = context.getString(R.string.status_confirmed);
                textColor = Color.parseColor("#3D5AF1");
                bgColor   = Color.parseColor("#EDE7F6");
                break;
            case "COMPLETED":
                label     = context.getString(R.string.status_completed);
                textColor = Color.parseColor("#388E3C");
                bgColor   = Color.parseColor("#E8F5E9");
                holder.itemView.setAlpha(0.75f);
                break;
            case "CANCELLED":
                label     = context.getString(R.string.status_cancelled);
                textColor = Color.parseColor("#D32F2F");
                bgColor   = Color.parseColor("#FFEBEE");
                holder.studentName.setPaintFlags(
                        holder.studentName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                break;
            case "NO_SHOW":
                label     = context.getString(R.string.status_no_show);
                textColor = Color.parseColor("#E65100");
                bgColor   = Color.parseColor("#FFF3E0");
                holder.itemView.setAlpha(0.75f);
                break;
            default:
                label     = status;
                textColor = Color.parseColor("#8A8A9A");
                bgColor   = Color.parseColor("#F2F4F8");
                break;
        }
        holder.sessionTopic.setText(label);
        holder.sessionTopic.setTextColor(textColor);
        ViewCompat.setBackgroundTintList(holder.sessionTopic,
                ColorStateList.valueOf(bgColor));
    }

    /**
     * Updates the appointment status to "NO_SHOW" via {@link AppointmentRepository}
     * and refreshes the card on success.
     *
     * @param apt      The appointment to mark.
     * @param holder   The ViewHolder to refresh.
     * @param position The adapter position.
     */
    private void markNoShow(Appointment apt, ViewHolder holder, int position) {
        appointmentRepository.updateAppointmentStatus(apt.getId(), "NO_SHOW",
                new AppointmentRepository.OnStatusUpdateCallback() {
                    @Override
                    public void onSuccess() {
                        apt.setStatus("NO_SHOW");
                        applyStatusBadge(holder, "NO_SHOW");
                        Toast.makeText(context, context.getString(R.string.marked_no_show),
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(context, "Failed to update status",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private static String formatDate(String raw) {
        if (raw == null || raw.isEmpty()) return "—";
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            in.setLenient(false);
            return new SimpleDateFormat("EEE, MMM d", Locale.US).format(in.parse(raw.trim()));
        } catch (Exception ignored) {}
        return raw;
    }

    /** Normalizes any stored time string to HH:mm (24-hour, zero-padded, no AM/PM). */
    private static String normalizeTime(String raw) {
        if (raw == null || raw.isEmpty()) return "—";
        // Already HH:mm or H:mm 24h — just zero-pad the hour
        if (raw.matches("\\d{1,2}:\\d{2}")) {
            String[] p = raw.split(":");
            return String.format(Locale.US, "%02d:%02d",
                    Integer.parseInt(p[0]), Integer.parseInt(p[1]));
        }
        // 12h with AM/PM e.g. "10:00 AM", "2:00 PM"
        try {
            SimpleDateFormat in = new SimpleDateFormat("h:mm a", Locale.US);
            in.setLenient(false);
            return new SimpleDateFormat("HH:mm", Locale.US).format(in.parse(raw.trim()));
        } catch (Exception ignored) {}
        return raw;
    }

    @Override
    public int getItemCount() {
        return appointments.size();
    }

    /** ViewHolder for a single appointment card. */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView studentName, sessionTopic, sessionTime, sessionDate;
        Button joinButton, noShowButton, crisisButton, profileButton, notesButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            studentName = itemView.findViewById(R.id.studentName);
            sessionTopic = itemView.findViewById(R.id.sessionTopic);
            sessionTime = itemView.findViewById(R.id.sessionTime);
            sessionDate = itemView.findViewById(R.id.sessionDate);
            joinButton = itemView.findViewById(R.id.joinButton);
            noShowButton = itemView.findViewById(R.id.noShowButton);
            crisisButton = itemView.findViewById(R.id.crisisButton);
            profileButton = itemView.findViewById(R.id.profileButton);
            notesButton = itemView.findViewById(R.id.notesButton);
        }
    }
}
