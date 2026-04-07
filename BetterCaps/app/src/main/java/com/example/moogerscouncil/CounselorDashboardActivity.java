package com.example.moogerscouncil;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.cardview.widget.CardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dashboard for counselors. Shows upcoming appointments,
 * stats, and allows adding availability slots.
 */
public class CounselorDashboardActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private AppointmentAdapter adapter;
    private List<Appointment> appointmentList;
    private TextView counselorNameText, todayCount, totalCount, weekCount;
    private String counselorId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_counselor_dashboard);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        appointmentList = new ArrayList<>();

        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        counselorId = mAuth.getCurrentUser().getUid();

        counselorNameText = findViewById(R.id.counselorNameText);
        todayCount = findViewById(R.id.todaySessionCount);
        totalCount = findViewById(R.id.totalPatientsCount);
        weekCount = findViewById(R.id.weekSessionCount);
        progressBar = findViewById(R.id.progressBar);

        recyclerView = findViewById(R.id.appointmentsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AppointmentAdapter(this, appointmentList);
        recyclerView.setAdapter(adapter);

        // Load counselor name from Firestore
        db.collection("counselors").document(counselorId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        counselorNameText.setText(name != null ? name : "Dr. Counselor");
                    }
                });

        // Add slot banner
        CardView addSlotBanner = findViewById(R.id.addSlotBanner);
        addSlotBanner.setOnClickListener(v -> showAddSlotDialog());

        // Logout
        ImageButton logoutBtn = findViewById(R.id.logoutBtn);
        logoutBtn.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        loadAppointments();
    }

    private void loadAppointments() {
        progressBar.setVisibility(View.VISIBLE);

        db.collection("appointments")
                .whereEqualTo("counselorId", counselorId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    progressBar.setVisibility(View.GONE);
                    appointmentList.clear();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Appointment apt = doc.toObject(Appointment.class);
                        apt.setId(doc.getId());
                        appointmentList.add(apt);
                    }

                    adapter.notifyDataSetChanged();

                    // Update stats
                    todayCount.setText(String.valueOf(appointmentList.size()));
                    totalCount.setText(String.valueOf(appointmentList.size()));
                    weekCount.setText(String.valueOf(appointmentList.size()));
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showAddSlotDialog() {
        Calendar calendar = Calendar.getInstance();

        // Pick date first
        DatePickerDialog datePicker = new DatePickerDialog(this,
                (view, year, month, day) -> {
                    String date = year + "-" + (month + 1) + "-" + day;

                    // Then pick time
                    TimePickerDialog timePicker = new TimePickerDialog(this,
                            (tview, hour, minute) -> {
                                String time = String.format("%02d:%02d", hour, minute);
                                addSlotToFirestore(date, time);
                            },
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            true
                    );
                    timePicker.show();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePicker.show();
    }

    private void addSlotToFirestore(String date, String time) {
        Map<String, Object> slot = new HashMap<>();
        slot.put("counselorId", counselorId);
        slot.put("date", date);
        slot.put("time", time);
        slot.put("available", true);

        db.collection("slots").add(slot)
                .addOnSuccessListener(ref ->
                        Toast.makeText(this,
                                "Slot added: " + date + " at " + time,
                                Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}