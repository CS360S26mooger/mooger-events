package com.example.moogerscouncil;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;

public class StudentAppointmentAdapter extends RecyclerView.Adapter<StudentAppointmentAdapter.ViewHolder> {

    private List<Appointment> appointments;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    public StudentAppointmentAdapter(List<Appointment> appointments) {
        this.appointments = appointments;
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
        holder.timeText.setText(apt.getTime());
        holder.dateText.setText(apt.getDate());
        holder.statusText.setText("Status: " + apt.getStatus());

        // Fetch counselor name from 'counselors' collection
        String counselorId = apt.getCounselorId();
        if (counselorId == null || counselorId.isEmpty()) {
            holder.counselorNameText.setText("Counselor: Not Assigned");
        } else {
            db.collection("counselors").document(counselorId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            holder.counselorNameText.setText("Counselor: " + documentSnapshot.getString("name"));
                        } else {
                            // Sometimes the counselor ID might be in 'users' collection too
                            db.collection("users").document(counselorId)
                                    .get()
                                    .addOnSuccessListener(userDoc -> {
                                        if (userDoc.exists()) {
                                            holder.counselorNameText.setText("Counselor: " + userDoc.getString("name"));
                                        } else {
                                            holder.counselorNameText.setText("Counselor: Unknown");
                                        }
                                    });
                        }
                    });
        }
    }

    @Override
    public int getItemCount() {
        return appointments.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView timeText, dateText, counselorNameText, statusText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            timeText = itemView.findViewById(R.id.sessionTime);
            dateText = itemView.findViewById(R.id.sessionDate);
            counselorNameText = itemView.findViewById(R.id.studentName);
            statusText = itemView.findViewById(R.id.sessionTopic);

            // Hide counselor-only action buttons if they exist in the layout
            View buttonsLayout = itemView.findViewById(R.id.joinButton);
            if (buttonsLayout != null) {
                // For simplicity, we can hide the whole action buttons row if they are in a container,
                // but since they are in individual layouts, let's just hide the main ones.
                itemView.findViewById(R.id.joinButton).setVisibility(View.GONE);
                itemView.findViewById(R.id.noShowButton).setVisibility(View.GONE);
                itemView.findViewById(R.id.crisisButton).setVisibility(View.GONE);
                itemView.findViewById(R.id.profileButton).setVisibility(View.GONE);
                itemView.findViewById(R.id.notesButton).setVisibility(View.GONE);
            }
        }
    }
}