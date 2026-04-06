package com.example.moogerscouncil;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;

public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.ViewHolder> {

    private List<Appointment> appointments;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    public AppointmentAdapter(List<Appointment> appointments) {
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

        // Fetch student name from 'users' collection
        db.collection("users").document(apt.getStudentId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        holder.studentNameText.setText("Student: " + documentSnapshot.getString("name"));
                    } else {
                        holder.studentNameText.setText("Student: Unknown");
                    }
                });
    }

    @Override
    public int getItemCount() {
        return appointments.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView timeText, dateText, studentNameText, statusText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            timeText = itemView.findViewById(R.id.aptTime);
            dateText = itemView.findViewById(R.id.aptDate);
            studentNameText = itemView.findViewById(R.id.aptStudentName);
            statusText = itemView.findViewById(R.id.aptStatus);
        }
    }
}