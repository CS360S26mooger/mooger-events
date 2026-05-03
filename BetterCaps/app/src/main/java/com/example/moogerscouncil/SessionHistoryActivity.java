package com.example.moogerscouncil;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Counselor-facing chronological appointment history for one student.
 */
public class SessionHistoryActivity extends AppCompatActivity {

    private final List<Appointment> appointments = new ArrayList<>();
    private HistoryAdapter adapter;
    private TextView textEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_history);

        String studentId = getIntent().getStringExtra(StudentProfileActivity.EXTRA_STUDENT_ID);
        ((ImageButton) findViewById(R.id.buttonBack)).setOnClickListener(v -> finish());
        textEmpty = findViewById(R.id.textHistoryEmpty);
        RecyclerView recycler = findViewById(R.id.recyclerSessionHistory);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter(appointments);
        recycler.setAdapter(adapter);

        if (studentId == null) {
            textEmpty.setVisibility(View.VISIBLE);
            return;
        }
        loadHistory(studentId);
    }

    private void loadHistory(String studentId) {
        new AppointmentRepository().getAppointmentsForStudentHistory(studentId,
                new AppointmentRepository.OnAppointmentsLoadedCallback() {
                    @Override
                    public void onSuccess(List<Appointment> loaded) {
                        appointments.clear();
                        appointments.addAll(loaded);
                        adapter.notifyDataSetChanged();
                        textEmpty.setVisibility(loaded.isEmpty() ? View.VISIBLE : View.GONE);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        textEmpty.setVisibility(View.VISIBLE);
                        AppToast.show(SessionHistoryActivity.this,
                                R.string.error_loading_appointments,
                                AppToast.LENGTH_LONG);
                    }
                });
    }

    private static class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.Holder> {
        private final List<Appointment> data;

        HistoryAdapter(List<Appointment> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_session_history, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            Appointment appointment = data.get(position);
            holder.dateTime.setText(String.valueOf(appointment.getDate())
                    + "  " + String.valueOf(appointment.getTime()));
            holder.counselor.setText(holder.itemView.getContext().getString(
                    R.string.history_counselor_id,
                    valueOrDash(appointment.getCounselorId())));
            holder.status.setText(valueOrDash(appointment.getStatus()));
            boolean followUp = appointment.isNoShowFollowUpRequired();
            holder.followUp.setVisibility(followUp ? View.VISIBLE : View.GONE);
            holder.followUp.setText(holder.itemView.getContext().getString(
                    R.string.no_show_followup_status,
                    valueOrDash(appointment.getNoShowFollowUpStatus())));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        private static String valueOrDash(String value) {
            return value == null || value.isEmpty() ? "-" : value;
        }

        private static class Holder extends RecyclerView.ViewHolder {
            TextView dateTime;
            TextView counselor;
            TextView status;
            TextView followUp;

            Holder(@NonNull View itemView) {
                super(itemView);
                dateTime = itemView.findViewById(R.id.textHistoryDateTime);
                counselor = itemView.findViewById(R.id.textHistoryCounselor);
                status = itemView.findViewById(R.id.textHistoryStatus);
                followUp = itemView.findViewById(R.id.textHistoryFollowUp);
            }
        }
    }
}
