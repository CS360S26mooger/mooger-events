package com.example.moogerscouncil;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

/**
 * Activity for the counselor's view.
 * Displays upcoming appointments for the logged-in counselor.
 */
public class CounselorDashboardActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AppointmentAdapter adapter;
    private List<Appointment> appointmentList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ProgressBar progressBar;
    private TextView welcomeTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_counselor_dashboard);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        String counselorId = mAuth.getCurrentUser().getUid();

        welcomeTitle = findViewById(R.id.welcomeTitle);
        progressBar = findViewById(R.id.progressBar);
        recyclerView = findViewById(R.id.appointmentsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        appointmentList = new ArrayList<>();
        adapter = new AppointmentAdapter(appointmentList);
        recyclerView.setAdapter(adapter);

        // Fetch counselor name
        db.collection("users").document(counselorId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        welcomeTitle.setText("Hello, " + doc.getString("name"));
                    }
                });

        fetchAppointments(counselorId);
    }

    private void fetchAppointments(String counselorId) {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("appointments")
                .whereEqualTo("counselorId", counselorId)
                .orderBy("date", Query.Direction.ASCENDING)
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
                    if (appointmentList.isEmpty()) {
                        Toast.makeText(this, "No upcoming appointments.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error fetching appointments: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}