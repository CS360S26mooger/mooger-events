/*
 * AppointmentAdapter.java
 * Role: RecyclerView adapter for the counselor's appointment list.
 *       Each card shows student name, date/time, and status badge only.
 *       Tapping a card opens ManageSessionActivity where all actions live.
 *
 * Design pattern: Adapter pattern (RecyclerView); Repository pattern for data ops.
 * Part of the BetterCAPS counseling platform.
 */
package com.example.moogerscouncil;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for the counselor's appointment dashboard cards.
 *
 * <p>Each card shows student name, date, time, and a color-coded status badge.
 * Tapping opens {@link ManageSessionActivity} for all per-session actions.</p>
 */
public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.ViewHolder> {

    private List<Appointment> appointments;
    private final Context context;
    private final UserRepository userRepository;

    /**
     * Creates a new adapter.
     *
     * @param context      Activity context for inflation and intents.
     * @param appointments Initial appointment list (may be replaced via {@link #setData}).
     */
    public AppointmentAdapter(Context context, List<Appointment> appointments) {
        this.context = context;
        this.appointments = appointments;
        this.userRepository = new UserRepository();
    }

    /**
     * Replaces the current appointment list and notifies the adapter.
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

        holder.studentName.setText(R.string.loading_name);
        if (apt.getStudentId() != null) {
            userRepository.getUserName(apt.getStudentId(),
                    name -> holder.studentName.setText(name));
        } else {
            holder.studentName.setText(R.string.unknown_student);
        }

        holder.sessionTime.setText(normalizeTime(apt.getTime()));
        holder.sessionDate.setText(formatDate(apt.getDate()));
        holder.returningBadge.setVisibility(apt.isReturningStudent() ? View.VISIBLE : View.GONE);
        applyStatusBadge(holder, apt.getStatus());

        holder.itemView.setOnClickListener(v -> openManageSession(apt, holder));
    }

    @Override
    public int getItemCount() {
        return appointments.size();
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private void openManageSession(Appointment apt, ViewHolder holder) {
        Intent intent = new Intent(context, ManageSessionActivity.class);
        intent.putExtra(ManageSessionActivity.EXTRA_APPOINTMENT_ID, apt.getId());
        intent.putExtra(ManageSessionActivity.EXTRA_STUDENT_ID, apt.getStudentId());
        intent.putExtra(ManageSessionActivity.EXTRA_COUNSELOR_ID, apt.getCounselorId());
        intent.putExtra(ManageSessionActivity.EXTRA_DATE, apt.getDate());
        intent.putExtra(ManageSessionActivity.EXTRA_TIME, apt.getTime());
        intent.putExtra(ManageSessionActivity.EXTRA_STATUS, apt.getStatus());
        intent.putExtra(ManageSessionActivity.EXTRA_CRISIS_ID, apt.getCrisisEscalationId());
        intent.putExtra(ManageSessionActivity.EXTRA_RETURNING_STUDENT, apt.isReturningStudent());
        String name = holder.studentName.getText() == null
                ? "" : holder.studentName.getText().toString();
        intent.putExtra(ManageSessionActivity.EXTRA_STUDENT_NAME, name);
        context.startActivity(intent);
    }

    // ── Badge styling ─────────────────────────────────────────────────────────

    /**
     * Applies color-coded status badge styling to the appointment card.
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
                textColor = Color.parseColor("#E8761A");
                bgColor   = Color.parseColor("#FFF3E8");
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

    // ── Time / date helpers ───────────────────────────────────────────────────

    private static String formatDate(String raw) {
        if (raw == null || raw.isEmpty()) return "—";
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            in.setLenient(false);
            return new SimpleDateFormat("EEE, MMM d", Locale.US).format(in.parse(raw.trim()));
        } catch (Exception ignored) {}
        return raw;
    }

    /** Normalizes any stored time string to HH:mm (24-hour, zero-padded). */
    static String normalizeTime(String raw) {
        if (raw == null || raw.isEmpty()) return "—";
        if (raw.matches("\\d{1,2}:\\d{2}")) {
            String[] p = raw.split(":");
            return String.format(Locale.US, "%02d:%02d",
                    Integer.parseInt(p[0]), Integer.parseInt(p[1]));
        }
        try {
            SimpleDateFormat in = new SimpleDateFormat("h:mm a", Locale.US);
            in.setLenient(false);
            return new SimpleDateFormat("HH:mm", Locale.US).format(in.parse(raw.trim()));
        } catch (Exception ignored) {}
        return raw;
    }

    /**
     * Returns true if the current time is at least 10 minutes past the slot start time.
     *
     * @param date ISO date string "yyyy-MM-dd".
     * @param time Time string (HH:mm or h:mm a).
     * @return true if no-show marking window is open.
     */
    public static boolean isNoShowWindowOpen(String date, String time) {
        if (date == null || time == null) return false;
        try {
            String normalized = normalizeTime(time);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
            sdf.setLenient(false);
            Date slotStart = sdf.parse(date.trim() + " " + normalized);
            if (slotStart == null) return false;
            return System.currentTimeMillis() >= slotStart.getTime() + 10 * 60 * 1000L;
        } catch (Exception e) {
            return false;
        }
    }

    // ── ViewHolder ────────────────────────────────────────────────────────────

    /** ViewHolder for a single appointment card. */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView studentName, sessionTopic, sessionTime, sessionDate, returningBadge;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            studentName   = itemView.findViewById(R.id.studentName);
            sessionTopic  = itemView.findViewById(R.id.sessionTopic);
            sessionTime   = itemView.findViewById(R.id.sessionTime);
            sessionDate   = itemView.findViewById(R.id.sessionDate);
            returningBadge = itemView.findViewById(R.id.returningStudentBadge);
        }
    }
}
