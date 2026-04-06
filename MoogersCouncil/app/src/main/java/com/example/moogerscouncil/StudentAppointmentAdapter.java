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
        db.collection("counselors").document(apt.getCounselorId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        holder.counselorNameText.setText("Counselor: " + documentSnapshot.getString("name"));
                    } else {
                        // Sometimes the counselor ID might be in 'users' collection too
                        db.collection("users").document(apt.getCounselorId())
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

    @Override
    public int getItemCount() {
        return appointments.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView timeText, dateText, counselorNameText, statusText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            timeText = itemView.findViewById(R.id.aptTime);
            dateText = itemView.findViewById(R.id.aptDate);
            counselorNameText = itemView.findViewById(R.id.aptStudentName); // Using existing ID
            statusText = itemView.findViewById(R.id.aptStatus);
        }
    }
}