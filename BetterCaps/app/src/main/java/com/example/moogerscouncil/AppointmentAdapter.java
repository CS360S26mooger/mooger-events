package com.example.moogerscouncil;

import android.content.Context;
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
 * RecyclerView adapter for counselor's appointment list.
 * Shows student info, session time, and action buttons.
 */
public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.ViewHolder> {

    private List<Appointment> appointments;
    private Context context;

    public AppointmentAdapter(Context context, List<Appointment> appointments) {
        this.context = context;
        this.appointments = appointments;
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

        // Show first letter of studentId as avatar initial
        String initial = apt.getStudentId() != null && !apt.getStudentId().isEmpty()
                ? String.valueOf(apt.getStudentId().charAt(0)).toUpperCase()
                : "S";
        holder.studentInitial.setText(initial);
        holder.studentName.setText("Student");
        holder.sessionTopic.setText(apt.getStatus());
        holder.sessionTime.setText("🕙 " + apt.getTime());
        holder.sessionDate.setText(apt.getDate());

        holder.joinButton.setOnClickListener(v ->
                Toast.makeText(context, "Video call coming soon!", Toast.LENGTH_SHORT).show()
        );

        holder.noShowButton.setOnClickListener(v ->
                Toast.makeText(context, "Marked as No-Show", Toast.LENGTH_SHORT).show()
        );

        holder.crisisButton.setOnClickListener(v ->
                new androidx.appcompat.app.AlertDialog.Builder(context)
                        .setTitle("🚨 Crisis Alert")
                        .setMessage("Escalate this student to campus emergency services?")
                        .setPositiveButton("Escalate", null)
                        .setNegativeButton("Cancel", null)
                        .show()
        );

        holder.profileButton.setOnClickListener(v ->
                Toast.makeText(context, "Student profile coming soon!", Toast.LENGTH_SHORT).show()
        );

        holder.notesButton.setOnClickListener(v ->
                Toast.makeText(context, "Session notes coming soon!", Toast.LENGTH_SHORT).show()
        );
    }

    @Override
    public int getItemCount() {
        return appointments.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView studentInitial, studentName, sessionTopic, sessionTime, sessionDate;
        Button joinButton, noShowButton, crisisButton, profileButton, notesButton;

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