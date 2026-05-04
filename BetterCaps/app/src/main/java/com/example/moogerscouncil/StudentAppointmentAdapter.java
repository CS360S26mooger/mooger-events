/*
 * StudentAppointmentAdapter.java
 * Role: RecyclerView adapter for displaying a student's appointment history.
 *       Shows counselor name (fetched via CounselorRepository), date, time,
 *       and a colour-coded status badge. Cards are info-only; no action buttons.
 *
 * Design pattern: Adapter pattern (RecyclerView).
 * Part of the BetterCAPS counseling platform.
 */
package com.example.moogerscouncil;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for the student's appointment history screen.
 * Each card shows counselor name, date, time, and status badge.
 */
public class StudentAppointmentAdapter
        extends RecyclerView.Adapter<StudentAppointmentAdapter.ViewHolder> {

    private final List<Appointment> appointments;
    private final CounselorRepository counselorRepository;

    public StudentAppointmentAdapter(List<Appointment> appointments) {
        this.appointments = appointments;
        this.counselorRepository = new CounselorRepository();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_student_appointment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Appointment apt = appointments.get(position);

        holder.timeText.setText(normalizeTime(apt.getTime()));
        holder.dateText.setText(formatDate(apt.getDate()));

        // Status pill
        String status = apt.getStatus() != null ? apt.getStatus() : "UNKNOWN";
        holder.statusText.setText(statusLabel(status));
        holder.statusText.setTextColor(statusColour(status));
        ViewCompat.setBackgroundTintList(holder.statusText,
                ColorStateList.valueOf(statusBgColour(status)));

        // Returning badge — not applicable for student history view
        holder.returningBadge.setVisibility(View.GONE);

        // Counselor name async lookup
        holder.counselorNameText.setText(R.string.loading_name);
        String counselorId = apt.getCounselorId();
        if (counselorId == null || counselorId.isEmpty()) {
            holder.counselorNameText.setText(R.string.unknown_student);
            return;
        }
        holder.itemView.setTag(counselorId);
        Counselor cached = SessionCache.getInstance().getSingleCounselor(counselorId);
        if (cached != null) {
            holder.counselorNameText.setText(
                    cached.getName() != null ? cached.getName() : "Your Counselor");
        } else {
            counselorRepository.getCounselor(counselorId,
                    new CounselorRepository.OnCounselorFetchedCallback() {
                        @Override
                        public void onSuccess(Counselor counselor) {
                            if (!counselorId.equals(holder.itemView.getTag())) return;
                            SessionCache.getInstance().putSingleCounselor(counselorId, counselor);
                            holder.counselorNameText.setText(
                                    counselor.getName() != null ? counselor.getName() : "Your Counselor");
                        }
                        @Override
                        public void onFailure(Exception e) {
                            if (!counselorId.equals(holder.itemView.getTag())) return;
                            holder.counselorNameText.setText("Counselor: Unknown");
                        }
                    });
        }
    }

    @Override
    public int getItemCount() {
        return appointments.size();
    }

    // ── Formatting helpers ────────────────────────────────────────────────────

    private static String formatDate(String raw) {
        if (raw == null || raw.isEmpty()) return "—";
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            in.setLenient(false);
            return new SimpleDateFormat("EEE, MMM d", Locale.US).format(in.parse(raw.trim()));
        } catch (Exception ignored) {}
        return raw;
    }

    private static String normalizeTime(String raw) {
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

    private String statusLabel(String status) {
        switch (status) {
            case "COMPLETED": return "Completed";
            case "CONFIRMED": return "Upcoming";
            case "CANCELLED": return "Cancelled";
            case "NO_SHOW":   return "No Show";
            default:          return status;
        }
    }

    private int statusColour(String status) {
        switch (status) {
            case "COMPLETED": return 0xFF388E3C;
            case "CONFIRMED": return 0xFF3D5AF1;
            case "CANCELLED":
            case "NO_SHOW":   return 0xFFD32F2F;
            default:          return 0xFF8A8A9A;
        }
    }

    private int statusBgColour(String status) {
        switch (status) {
            case "COMPLETED": return Color.parseColor("#E8F5E9");
            case "CONFIRMED": return Color.parseColor("#EDE7F6");
            case "CANCELLED":
            case "NO_SHOW":   return Color.parseColor("#FFEBEE");
            default:          return Color.parseColor("#F2F4F8");
        }
    }

    // ── ViewHolder ────────────────────────────────────────────────────────────

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView timeText, dateText, counselorNameText, statusText, returningBadge;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            timeText          = itemView.findViewById(R.id.sessionTime);
            dateText          = itemView.findViewById(R.id.sessionDate);
            counselorNameText = itemView.findViewById(R.id.studentName);
            statusText        = itemView.findViewById(R.id.sessionTopic);
            returningBadge    = itemView.findViewById(R.id.returningStudentBadge);
        }
    }
}
