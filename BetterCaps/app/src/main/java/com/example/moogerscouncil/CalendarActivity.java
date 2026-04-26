package com.example.moogerscouncil;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CalendarActivity extends AppCompatActivity {

    private CustomCalendarView calendarView;
    private TextView selectedDateText;
    private RecyclerView recyclerView;
    private StudentAppointmentAdapter adapter;

    private final List<Appointment> appointmentList = new ArrayList<>();
    private final List<Appointment> allAppointments = new ArrayList<>();

    private AppointmentRepository appointmentRepository;
    private String studentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        appointmentRepository = new AppointmentRepository();
        studentId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());

        calendarView      = findViewById(R.id.calendarView);
        selectedDateText  = findViewById(R.id.selectedDateText);
        recyclerView      = findViewById(R.id.appointmentsForDateRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new StudentAppointmentAdapter(appointmentList);
        recyclerView.setAdapter(adapter);

        String todayStr = toDateStr(Calendar.getInstance());
        selectedDateText.setText(friendlyLabel(todayStr));

        calendarView.setOnDateClickListener(date -> {
            selectedDateText.setText(friendlyLabel(date));
            filterByDate(allAppointments, date);
        });

        // Instant render from cache
        List<Appointment> cached = SessionCache.getInstance().getStudentAppointments(studentId);
        if (cached != null) {
            allAppointments.clear();
            allAppointments.addAll(cached);
            calendarView.setHighlightedDates(bookedDates(allAppointments));
            filterByDate(allAppointments, todayStr);
        }

        // Background refresh from Firestore
        appointmentRepository.getAppointmentsForStudent(studentId,
                new AppointmentRepository.OnAppointmentsLoadedCallback() {
                    @Override
                    public void onSuccess(List<Appointment> appointments) {
                        allAppointments.clear();
                        allAppointments.addAll(appointments);
                        calendarView.setHighlightedDates(bookedDates(allAppointments));
                        filterByDate(allAppointments, calendarView.getSelectedDate());
                    }
                    @Override
                    public void onFailure(Exception e) { /* cached data still shown */ }
                });
    }

    private Set<String> bookedDates(List<Appointment> all) {
        Set<String> dates = new HashSet<>();
        for (Appointment a : all) {
            String s = a.getStatus();
            if (("CONFIRMED".equals(s) || "COMPLETED".equals(s)) && a.getDate() != null) {
                dates.add(a.getDate());
            }
        }
        return dates;
    }

    private void filterByDate(List<Appointment> all, String date) {
        String today = toDateStr(Calendar.getInstance());
        appointmentList.clear();
        for (Appointment a : all) {
            String s = a.getStatus();
            if ("CONFIRMED".equals(s) && a.getDate() != null
                    && a.getDate().compareTo(today) < 0) {
                appointmentRepository.updateAppointmentStatus(a.getId(), "NO_SHOW",
                        new AppointmentRepository.OnStatusUpdateCallback() {
                            @Override public void onSuccess() {
                                SessionCache.getInstance().invalidateAppointments();
                            }
                            @Override public void onFailure(Exception e) { /* best effort */ }
                        });
                continue;
            }
            if (("CONFIRMED".equals(s) || "COMPLETED".equals(s)) && date.equals(a.getDate())) {
                appointmentList.add(a);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private String friendlyLabel(String dateStr) {
        if (dateStr == null) return "Appointments for Today";
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            String friendly = new SimpleDateFormat("EEE, MMM d", Locale.US)
                    .format(in.parse(dateStr));
            return "Appointments for " + friendly;
        } catch (Exception ignored) {
            return "Appointments for " + dateStr;
        }
    }

    private String toDateStr(Calendar cal) {
        return String.format(Locale.US, "%04d-%02d-%02d",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH));
    }
}
