package com.example.moogerscouncil;

import android.os.Bundle;
import android.widget.CalendarView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CalendarActivity extends AppCompatActivity {

    private CalendarView calendarView;
    private TextView selectedDateText;
    private RecyclerView recyclerView;
    private StudentAppointmentAdapter adapter;
    private List<Appointment> appointmentList;
    private FirebaseFirestore db;
    private String studentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        db = FirebaseFirestore.getInstance();
        studentId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        calendarView = findViewById(R.id.calendarView);
        selectedDateText = findViewById(R.id.selectedDateText);
        recyclerView = findViewById(R.id.appointmentsForDateRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        appointmentList = new ArrayList<>();
        adapter = new StudentAppointmentAdapter(appointmentList);
        recyclerView.setAdapter(adapter);

        // Get today's date in "yyyy-MM-dd" format
        Calendar today = Calendar.getInstance();
        String todayString = formatDate(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH));
        fetchAppointmentsForDate(todayString);

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            String selectedDate = formatDate(year, month, dayOfMonth);
            selectedDateText.setText("Appointments for " + selectedDate);
            fetchAppointmentsForDate(selectedDate);
        });
    }

    private String formatDate(int year, int month, int day) {
        // Match the format used in Firestore (e.g. 2026-04-10)
        return String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day);
    }

    private void fetchAppointmentsForDate(String date) {
        db.collection("appointments")
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("date", date)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    appointmentList.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Appointment apt = doc.toObject(Appointment.class);
                        apt.setId(doc.getId());
                        appointmentList.add(apt);
                    }
                    adapter.notifyDataSetChanged();
                });
    }
}