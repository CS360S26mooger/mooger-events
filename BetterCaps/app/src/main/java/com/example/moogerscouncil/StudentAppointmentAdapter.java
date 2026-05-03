/*
 * StudentAppointmentAdapter.java
 * Role: RecyclerView adapter for displaying a student's appointment history.
 *       Shows counselor name (fetched via CounselorRepository), date, time,
 *       and a colour-coded status badge. Counselor-only action buttons are hidden.
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
 * Each card shows counselor name, date, time, and status.
 * Counselor-facing action buttons (Join, No-Show, Crisis) are hidden.
 *
 * <p>Counselor names are fetched via {@link CounselorRepository} — no direct
 * Firestore access from this adapter.</p>
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
                .inflate(R.layout.item_appointment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Appointment apt = appointments.get(position);

        holder.timeText.setText(normalizeTime(apt.getTime()));
        holder.dateText.setText(formatDate(apt.getDate()));

        // Status pill — text, text colour, and pastel background tint
        String status = apt.getStatus() != null ? apt.getStatus() : "UNKNOWN";
        holder.statusText.setText(statusLabel(status));
        holder.statusText.setTextColor(statusColour(status));
        ViewCompat.setBackgroundTintList(holder.statusText,
                ColorStateList.valueOf(statusBgColour(status)));

        holder.counselorNameText.setText("Loading…");

        String counselorId = apt.getCounselorId();
        if (counselorId == null || counselorId.isEmpty()) {
            holder.counselorNameText.setText("Counselor: Not Assigned");
        } else {
            // Tag the ViewHolder so recycled views don't show stale async results
            holder.itemView.setTag(counselorId);

            // Check session cache first — avoids a Firestore round-trip for every card
            Counselor cached = SessionCache.getInstance().getSingleCounselor(counselorId);
            if (cached != null) {
                String name = cached.getName() != null ? cached.getName() : "Your Counselor";
                holder.counselorNameText.setText(name);
            } else {
                counselorRepository.getCounselor(counselorId,
                        new CounselorRepository.OnCounselorFetchedCallback() {
                            @Override
                            public void onSuccess(Counselor counselor) {
                                // Guard: ViewHolder may have been recycled for a different row
                                if (!counselorId.equals(holder.itemView.getTag())) return;
                                SessionCache.getInstance().putSingleCounselor(counselorId, counselor);
                                String name = counselor.getName() != null
                                        ? counselor.getName() : "Your Counselor";
                                holder.counselorNameText.setText(name);
                            }

                            @Override
                            public void onFailure(Exception e) {
                                if (!counselorId.equals(holder.itemView.getTag())) return;
                                holder.counselorNameText.setText("Counselor: Unknown");
                            }
                        });
            }
        }
    }

    @Override
    public int getItemCount() {
        return appointments.size();
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
            case "COMPLETED":  return "Completed";
            case "CONFIRMED":  return "Upcoming";
            case "CANCELLED":  return "Cancelled";
            case "NO_SHOW":    return "No Show";
            default:           return status;
        }
    }

    private int statusColour(String status) {
        switch (status) {
            case "COMPLETED":  return 0xFF388E3C;
            case "CONFIRMED":  return 0xFF3D5AF1;
            case "CANCELLED":
            case "NO_SHOW":    return 0xFFD32F2F;
            default:           return 0xFF8A8A9A;
        }
    }

    private int statusBgColour(String status) {
        switch (status) {
            case "COMPLETED":  return Color.parseColor("#E8F5E9");
            case "CONFIRMED":  return Color.parseColor("#EDE7F6");
            case "CANCELLED":
            case "NO_SHOW":    return Color.parseColor("#FFEBEE");
            default:           return Color.parseColor("#F2F4F8");
        }
    }

    /**
     * ViewHolder for a single appointment history card.
     * Hides all counselor-only action buttons on construction.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView timeText, dateText, counselorNameText, statusText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            timeText          = itemView.findViewById(R.id.sessionTime);
            dateText          = itemView.findViewById(R.id.sessionDate);
            counselorNameText = itemView.findViewById(R.id.studentName);
            statusText        = itemView.findViewById(R.id.sessionTopic);

            // Hide counselor-only action buttons
            hideIfPresent(itemView, R.id.noShowButton);
            hideIfPresent(itemView, R.id.crisisButton);
            hideIfPresent(itemView, R.id.profileButton);
            hideIfPresent(itemView, R.id.notesButton);
        }

        private void hideIfPresent(View root, int id) {
            View v = root.findViewById(id);
            if (v != null) v.setVisibility(View.GONE);
        }
    }
}
