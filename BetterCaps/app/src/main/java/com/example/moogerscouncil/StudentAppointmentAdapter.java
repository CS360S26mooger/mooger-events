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

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

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

        holder.timeText.setText(apt.getTime() != null ? apt.getTime() : "—");
        holder.dateText.setText(apt.getDate() != null ? apt.getDate() : "—");

        // Status with colour badge
        String status = apt.getStatus() != null ? apt.getStatus() : "UNKNOWN";
        holder.statusText.setText(statusLabel(status));
        holder.statusText.setTextColor(statusColour(status));

        // Counselor initial placeholder while name loads
        holder.counselorNameText.setText("Loading…");
        holder.initialText.setText("?");

        String counselorId = apt.getCounselorId();
        if (counselorId == null || counselorId.isEmpty()) {
            holder.counselorNameText.setText("Counselor: Not Assigned");
            holder.initialText.setText("?");
        } else {
            counselorRepository.getCounselor(counselorId,
                    new CounselorRepository.OnCounselorFetchedCallback() {
                        @Override
                        public void onSuccess(Counselor counselor) {
                            String name = counselor.getName() != null
                                    ? counselor.getName() : "Unknown";
                            holder.counselorNameText.setText(name);
                            holder.initialText.setText(
                                    name.isEmpty() ? "?" : String.valueOf(name.charAt(0)));
                        }

                        @Override
                        public void onFailure(Exception e) {
                            holder.counselorNameText.setText("Counselor: Unknown");
                            holder.initialText.setText("?");
                        }
                    });
        }
    }

    @Override
    public int getItemCount() {
        return appointments.size();
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
            case "COMPLETED":  return 0xFF388E3C; // green
            case "CONFIRMED":  return 0xFF3D5AF1; // blue
            case "CANCELLED":
            case "NO_SHOW":    return 0xFFD32F2F; // red
            default:           return 0xFF8A8A9A; // grey
        }
    }

    /**
     * ViewHolder for a single appointment history card.
     * Hides all counselor-only action buttons on construction.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView timeText, dateText, counselorNameText, statusText, initialText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            timeText          = itemView.findViewById(R.id.sessionTime);
            dateText          = itemView.findViewById(R.id.sessionDate);
            counselorNameText = itemView.findViewById(R.id.studentName);
            statusText        = itemView.findViewById(R.id.sessionTopic);
            initialText       = itemView.findViewById(R.id.studentInitial);

            // Hide counselor-only action buttons
            hideIfPresent(itemView, R.id.joinButton);
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
