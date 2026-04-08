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
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

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

        // Avatar initial from student ID (placeholder until name loads)
        String initial = apt.getStudentId() != null && !apt.getStudentId().isEmpty()
                ? String.valueOf(apt.getStudentId().charAt(0)).toUpperCase()
                : "S";
        holder.studentInitial.setText(initial);

        // Async student name lookup
        holder.studentName.setText("Loading…");
        if (apt.getStudentId() != null) {
            userRepository.getUserName(apt.getStudentId(),
                    name -> holder.studentName.setText(name));
        } else {
            holder.studentName.setText("Unknown");
        }

        holder.sessionTime.setText("🕙 " + apt.getTime());
        holder.sessionDate.setText(apt.getDate());

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

        switch (status) {
            case "CONFIRMED":
                holder.sessionTopic.setText(context.getString(R.string.status_confirmed));
                holder.sessionTopic.setTextColor(Color.parseColor("#4CAF50"));
                break;
            case "COMPLETED":
                holder.sessionTopic.setText(context.getString(R.string.status_completed));
                holder.sessionTopic.setTextColor(Color.parseColor("#9E9E9E"));
                holder.itemView.setAlpha(0.6f);
                break;
            case "CANCELLED":
                holder.sessionTopic.setText(context.getString(R.string.status_cancelled));
                holder.sessionTopic.setTextColor(Color.parseColor("#F44336"));
                holder.studentName.setPaintFlags(
                        holder.studentName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                break;
            case "NO_SHOW":
                holder.sessionTopic.setText(context.getString(R.string.status_no_show));
                holder.sessionTopic.setTextColor(Color.parseColor("#FF9800"));
                holder.itemView.setAlpha(0.7f);
                break;
            default:
                holder.sessionTopic.setText(status);
                holder.sessionTopic.setTextColor(Color.parseColor("#8A8A9A"));
                break;
        }
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

    @Override
    public int getItemCount() {
        return appointments.size();
    }

    /** ViewHolder for a single appointment card. */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView studentInitial, studentName, sessionTopic, sessionTime, sessionDate;
        Button joinButton, noShowButton, crisisButton, profileButton, notesButton;

        /**
         * Binds view references from {@code item_appointment.xml}.
         *
         * @param itemView The inflated card view.
         */
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            studentInitial = itemView.findViewById(R.id.studentInitial);
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
