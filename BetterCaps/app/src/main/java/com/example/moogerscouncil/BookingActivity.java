/*
 * BookingActivity.java
 * Role: Allows a student to browse a counselor's available time slots on a
 *       CalendarView, select a slot, review a confirmation summary, and atomically
 *       book an appointment via AppointmentRepository's Firestore transaction.
 *
 * Design pattern: Repository pattern (AvailabilityRepository + AppointmentRepository).
 * Part of the BetterCAPS counseling platform.
 */
package com.example.moogerscouncil;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CalendarView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Booking screen for students. Displays a calendar, shows available time slots
 * for the selected date, and presents a confirmation bottom sheet before committing
 * the booking via an atomic Firestore transaction.
 *
 * <p>Launched from {@link CounselorProfileActivity} with extras:
 * {@code counselorId} and {@code counselorName}.</p>
 */
public class BookingActivity extends AppCompatActivity
        implements TimeSlotAdapter.OnBookClickListener {

    private String counselorId;
    private String counselorName;

    private CalendarView calendarView;
    private TextView labelSlots;
    private TextView textNoSlots;
    private RecyclerView recyclerSlots;
    private ProgressBar progressBar;

    private TimeSlotAdapter slotAdapter;
    private AvailabilitySchedule schedule;

    private AvailabilityRepository availabilityRepository;
    private AppointmentRepository appointmentRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        counselorId = getIntent().getStringExtra("counselorId");
        counselorName = getIntent().getStringExtra("counselorName");

        availabilityRepository = new AvailabilityRepository();
        appointmentRepository = new AppointmentRepository();

        TextView titleText = findViewById(R.id.counselorNameTitle);
        titleText.setText(getString(R.string.title_booking,
                counselorName != null ? counselorName : "Counselor"));

        calendarView = findViewById(R.id.calendarView);
        labelSlots = findViewById(R.id.labelSlots);
        textNoSlots = findViewById(R.id.textNoSlots);
        progressBar = findViewById(R.id.progressBar);
        recyclerSlots = findViewById(R.id.slotsRecyclerView);

        recyclerSlots.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        slotAdapter = new TimeSlotAdapter(new ArrayList<>(), this);
        recyclerSlots.setAdapter(slotAdapter);

        calendarView.setMinDate(System.currentTimeMillis() - 1000);
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            String selectedDate = String.format(Locale.US, "%04d-%02d-%02d",
                    year, month + 1, dayOfMonth);
            showSlotsForDate(selectedDate);
        });

        loadSlots(todayString());
    }

    /** Returns today's date as a "yyyy-MM-dd" string. */
    private String todayString() {
        Calendar c = Calendar.getInstance();
        return String.format(Locale.US, "%04d-%02d-%02d",
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH) + 1,
                c.get(Calendar.DAY_OF_MONTH));
    }

    /**
     * Fetches all available slots for the counselor via {@link AvailabilityRepository},
     * builds an {@link AvailabilitySchedule}, then immediately shows slots for
     * {@code initialDate} so the user sees something without tapping the calendar.
     *
     * @param initialDate The date to display on load, typically today.
     */
    private void loadSlots(String initialDate) {
        progressBar.setVisibility(View.VISIBLE);

        availabilityRepository.getAvailableSlotsForCounselor(counselorId,
                new AvailabilityRepository.OnSlotsLoadedCallback() {
                    @Override
                    public void onSuccess(List<TimeSlot> slots) {
                        progressBar.setVisibility(View.GONE);
                        schedule = AvailabilitySchedule.fromSlots(counselorId, slots);
                        // Auto-display slots for the initial date so the UI is not blank
                        showSlotsForDate(initialDate);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(BookingActivity.this,
                                getString(R.string.error_loading_slots),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Updates the slot RecyclerView to show available slots for the given date.
     * Shows an empty-state message when no slots exist for that day.
     *
     * @param date The selected date in "yyyy-MM-dd" format.
     */
    private void showSlotsForDate(String date) {
        if (schedule == null) {
            textNoSlots.setVisibility(View.VISIBLE);
            recyclerSlots.setVisibility(View.GONE);
            labelSlots.setVisibility(View.VISIBLE);
            return;
        }

        List<TimeSlot> slots = schedule.getSlotsForDate(date);
        labelSlots.setVisibility(View.VISIBLE);

        if (slots.isEmpty()) {
            textNoSlots.setVisibility(View.VISIBLE);
            recyclerSlots.setVisibility(View.GONE);
        } else {
            textNoSlots.setVisibility(View.GONE);
            recyclerSlots.setVisibility(View.VISIBLE);
            slotAdapter.setData(slots);
        }
    }

    /**
     * Called when the student taps "Book" on a time slot card.
     * Shows the {@link BookingConfirmationFragment} bottom sheet.
     *
     * @param slot The selected {@link TimeSlot}.
     */
    @Override
    public void onBookClick(TimeSlot slot) {
        BookingConfirmationFragment fragment =
                BookingConfirmationFragment.newInstance(counselorName, slot);

        fragment.setOnConfirmListener(confirmedSlot -> {
            if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                Toast.makeText(this, getString(R.string.error_booking_failed),
                        Toast.LENGTH_SHORT).show();
                return;
            }

            String studentId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            progressBar.setVisibility(View.VISIBLE);

            appointmentRepository.bookAppointment(studentId, counselorId, confirmedSlot,
                    new AppointmentRepository.OnBookingCallback() {
                        @Override
                        public void onSuccess() {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(BookingActivity.this,
                                    getString(R.string.booking_success),
                                    Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(BookingActivity.this,
                                    StudentHomeActivity.class));
                            finish();
                        }

                        @Override
                        public void onSlotTaken() {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(BookingActivity.this,
                                    getString(R.string.error_slot_taken),
                                    Toast.LENGTH_LONG).show();
                            // Remove taken slot from local schedule and refresh
                            if (schedule != null) {
                                schedule.getSlotsForDate(confirmedSlot.getDate())
                                        .remove(confirmedSlot);
                                showSlotsForDate(confirmedSlot.getDate());
                            }
                        }

                        @Override
                        public void onFailure(Exception e) {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(BookingActivity.this,
                                    getString(R.string.error_booking_failed),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        });

        fragment.show(getSupportFragmentManager(), "booking_confirm");
    }
}
