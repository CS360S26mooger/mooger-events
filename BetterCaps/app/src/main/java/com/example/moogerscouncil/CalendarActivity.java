/*
 * CalendarActivity.java
 * Role: Shows a student's appointments for a selected date via a CalendarView.
 *       All Firestore reads go through AppointmentRepository.
 *
 * Design pattern: Repository pattern (AppointmentRepository).
 * Part of the BetterCAPS counseling platform.
 */
package com.example.moogerscouncil;

import android.os.Bundle;
import android.widget.CalendarView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Calendar screen for students showing appointments on a selected date.
 * Fetches appointments per-date via {@link AppointmentRepository}.
 */
public class CalendarActivity extends AppCompatActivity {

    private CalendarView calendarView;
    private TextView selectedDateText;
    private RecyclerView recyclerView;
    private StudentAppointmentAdapter adapter;
    private final List<Appointment> appointmentList = new ArrayList<>();
    private AppointmentRepository appointmentRepository;
    private String studentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        appointmentRepository = new AppointmentRepository();
        studentId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        calendarView = findViewById(R.id.calendarView);
        selectedDateText = findViewById(R.id.selectedDateText);
        recyclerView = findViewById(R.id.appointmentsForDateRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new StudentAppointmentAdapter(appointmentList);
        recyclerView.setAdapter(adapter);

        // Load today's appointments on open
        Calendar today = Calendar.getInstance();
        String todayString = formatDate(today.get(Calendar.YEAR),
                today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH));
        fetchAppointmentsForDate(todayString);

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            String selectedDate = formatDate(year, month, dayOfMonth);
            selectedDateText.setText("Appointments for " + selectedDate);
            fetchAppointmentsForDate(selectedDate);
        });
    }

    private String formatDate(int year, int month, int day) {
        return String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day);
    }

    /**
     * Shows appointments for the given date. Checks the session cache first
     * (populated by StudentHomeActivity on login) for instant rendering,
     * then falls back to a Firestore fetch if cache is empty.
     *
     * @param date Date in "yyyy-MM-dd" format.
     */
    private void fetchAppointmentsForDate(String date) {
        // Try cache first — the full appointment list is pre-loaded by StudentHomeActivity
        List<Appointment> cached = SessionCache.getInstance().getStudentAppointments(studentId);
        if (cached != null) {
            filterByDate(cached, date);
        }

        // Always fetch fresh in background to catch any changes
        appointmentRepository.getAppointmentsForStudentOnDate(studentId, date,
                new AppointmentRepository.OnAppointmentsLoadedCallback() {
                    @Override
                    public void onSuccess(List<Appointment> appointments) {
                        filterByDate(appointments, date);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        // Non-critical: cached data (if any) already shown
                    }
                });
    }

    /** Filters an appointment list to CONFIRMED/COMPLETED for a given date and displays. */
    private void filterByDate(List<Appointment> all, String date) {
        appointmentList.clear();
        for (Appointment a : all) {
            String s = a.getStatus();
            if (("CONFIRMED".equals(s) || "COMPLETED".equals(s))
                    && date.equals(a.getDate())) {
                appointmentList.add(a);
            }
        }
        adapter.notifyDataSetChanged();
    }
}
