package com.example.moogerscouncil;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import java.util.ArrayList;
import java.util.List;

/**
 * Activity for booking a counselor's time slot.
 * Fetches available slots from Firestore and performs atomic booking transactions.
 */
public class BookingActivity extends AppCompatActivity implements TimeSlotAdapter.OnBookClickListener {

    private String counselorId;
    private String counselorName;
    private RecyclerView recyclerView;
    private TimeSlotAdapter adapter;
    private List<TimeSlot> slotList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ProgressBar progressBar;
    private TextView titleText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        counselorId = getIntent().getStringExtra("counselorId");
        counselorName = getIntent().getStringExtra("counselorName");

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        slotList = new ArrayList<>();

        titleText = findViewById(R.id.counselorNameTitle);
        titleText.setText("Book with " + (counselorName != null ? counselorName : "Counselor"));

        progressBar = findViewById(R.id.progressBar);
        recyclerView = findViewById(R.id.slotsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TimeSlotAdapter(slotList, this);
        recyclerView.setAdapter(adapter);

        fetchSlots();
    }

    private void fetchSlots() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("slots")
                .whereEqualTo("counselorId", counselorId)
                .whereEqualTo("available", true)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    progressBar.setVisibility(View.GONE);
                    slotList.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        TimeSlot slot = doc.toObject(TimeSlot.class);
                        slot.setId(doc.getId());
                        slotList.add(slot);
                    }
                    adapter.notifyDataSetChanged();
                    if (slotList.isEmpty()) {
                        Toast.makeText(this, "No available slots found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onBookClick(TimeSlot slot) {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please log in to book.", Toast.LENGTH_SHORT).show();
            return;
        }

        String studentId = mAuth.getCurrentUser().getUid();
        progressBar.setVisibility(View.VISIBLE);

        // Atomic Transaction: Update slot and create appointment
        db.runTransaction(transaction -> {
            DocumentReference slotRef = db.collection("slots").document(slot.getId());
            Boolean available = transaction.get(slotRef).getBoolean("available");

            if (available != null && available) {
                // 1. Mark slot as unavailable
                transaction.update(slotRef, "available", false);

                // 2. Create Appointment document
                DocumentReference appointmentRef = db.collection("appointments").document();
                Appointment appointment = new Appointment();
                appointment.setId(appointmentRef.getId());
                appointment.setStudentId(studentId);
                appointment.setCounselorId(counselorId);
                appointment.setSlotId(slot.getId());
                appointment.setDate(slot.getDate());
                appointment.setTime(slot.getTime());
                appointment.setStatus("CONFIRMED");

                transaction.set(appointmentRef, appointment);
                return null;
            } else {
                throw new RuntimeException("Slot is no longer available!");
            }
        }).addOnSuccessListener(result -> {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Booking Successful!", Toast.LENGTH_LONG).show();
            fetchSlots(); // Refresh list
        }).addOnFailureListener(e -> {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Booking Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }
}