/*
 * HistoryActivity.java
 * Role: Shows a student's full appointment history ordered by date.
 *       All Firestore reads go through AppointmentRepository.
 *
 * Design pattern: Repository pattern (AppointmentRepository).
 * Part of the BetterCAPS counseling platform.
 */
package com.example.moogerscouncil;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays the student's past and upcoming appointments in date order.
 * Fetches all appointments via {@link AppointmentRepository} and sorts
 * client-side (no composite Firestore index required).
 */
public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView textEmptyHistory;
    private StudentAppointmentAdapter adapter;
    private final List<Appointment> appointmentList = new ArrayList<>();
    private AppointmentRepository appointmentRepository;
    private String studentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        appointmentRepository = new AppointmentRepository();
        studentId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());

        textEmptyHistory = findViewById(R.id.textEmptyHistory);

        recyclerView = findViewById(R.id.historyRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StudentAppointmentAdapter(appointmentList);
        recyclerView.setAdapter(adapter);

        fetchHistory();
    }

    /**
     * Fetches the student's full appointment history via {@link AppointmentRepository}.
     * Results are sorted client-side by date ascending.
     */
    private void fetchHistory() {
        appointmentRepository.getAppointmentsForStudent(studentId,
                new AppointmentRepository.OnAppointmentsLoadedCallback() {
                    @Override
                    public void onSuccess(List<Appointment> appointments) {
                        appointmentList.clear();
                        appointmentList.addAll(appointments);
                        adapter.notifyDataSetChanged();

                        textEmptyHistory.setVisibility(
                                appointments.isEmpty() ? View.VISIBLE : View.GONE);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(HistoryActivity.this,
                                getString(R.string.error_loading_appointments),
                                Toast.LENGTH_SHORT).show();
                        textEmptyHistory.setVisibility(View.VISIBLE);
                    }
                });
    }
}
