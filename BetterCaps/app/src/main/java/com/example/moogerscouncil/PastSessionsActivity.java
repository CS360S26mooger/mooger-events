package com.example.moogerscouncil;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Shows past booked sessions for the logged-in counselor.
 * Only appointments before today with status CONFIRMED, COMPLETED, or NO_SHOW
 * are included — unbooked expired slots and cancelled appointments are excluded.
 * Sorted newest-first so the most recent past session appears at the top.
 */
public class PastSessionsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView textEmpty;
    private ProgressBar progressBar;
    private AppointmentAdapter adapter;
    private AppointmentRepository appointmentRepository;
    private String counselorId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_past_sessions);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        counselorId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        appointmentRepository = new AppointmentRepository();

        recyclerView = findViewById(R.id.recyclerPastSessions);
        textEmpty = findViewById(R.id.textEmptyPast);
        progressBar = findViewById(R.id.progressBar);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AppointmentAdapter(this, new ArrayList<>());
        recyclerView.setAdapter(adapter);

        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPastSessions();
    }

    private void loadPastSessions() {
        List<Appointment> cached = SessionCache.getInstance()
                .getCounselorAppointments(counselorId);
        if (cached != null) {
            renderPast(cached);
        } else {
            progressBar.setVisibility(View.VISIBLE);
        }

        appointmentRepository.getAppointmentsForCounselor(counselorId,
                new AppointmentRepository.OnAppointmentsLoadedCallback() {
                    @Override
                    public void onSuccess(List<Appointment> appointments) {
                        progressBar.setVisibility(View.GONE);
                        SessionCache.getInstance()
                                .putCounselorAppointments(counselorId, appointments);
                        renderPast(appointments);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        progressBar.setVisibility(View.GONE);
                        if (cached == null) {
                            textEmpty.setVisibility(View.VISIBLE);
                        }
                    }
                });
    }

    private void renderPast(List<Appointment> all) {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        List<Appointment> past = all.stream()
                .filter(a -> a.getDate() != null && a.getDate().compareTo(today) < 0)
                .filter(a -> {
                    String s = a.getStatus();
                    return "CONFIRMED".equals(s) || "COMPLETED".equals(s) || "NO_SHOW".equals(s);
                })
                .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                .collect(Collectors.toList());

        adapter.setData(past);
        textEmpty.setVisibility(past.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(past.isEmpty() ? View.GONE : View.VISIBLE);
    }
}
