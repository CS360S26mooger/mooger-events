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
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import java.util.HashSet;
import java.util.Set;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

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

    private String counselorId;     // Auth UID — primary slot path key
    private String counselorDocId;  // Firestore doc ID — fallback for old manually-created counselors
    private String counselorName;

    private CustomCalendarView calendarView;
    private TextView labelSlots;
    private TextView textNoSlots;
    private RecyclerView recyclerSlots;
    private View progressBar;
    private com.google.android.material.button.MaterialButton buttonJoinWaitlist;

    private TimeSlotAdapter slotAdapter;
    private AvailabilitySchedule schedule;

    private AvailabilityRepository availabilityRepository;
    private AppointmentRepository appointmentRepository;
    private WaitlistRepository waitlistRepository;
    private String assessmentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        counselorId   = getIntent().getStringExtra("counselorId");
        counselorDocId = getIntent().getStringExtra("counselorDocId");
        counselorName = getIntent().getStringExtra("counselorName");
        assessmentId = getIntent().getStringExtra(QuizActivity.EXTRA_ASSESSMENT_ID);

        availabilityRepository = new AvailabilityRepository();
        appointmentRepository = new AppointmentRepository();
        waitlistRepository = new WaitlistRepository();

        TextView titleText = findViewById(R.id.counselorNameTitle);
        String name = counselorName != null ? counselorName : "Counselor";
        String header = "Book with\n";
        SpannableString spannable = new SpannableString(header + name);
        spannable.setSpan(
                new ForegroundColorSpan(Color.argb(160, 26, 26, 46)),
                header.length(), header.length() + name.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        titleText.setText(spannable);

        calendarView = findViewById(R.id.calendarView);
        labelSlots = findViewById(R.id.labelSlots);
        textNoSlots = findViewById(R.id.textNoSlots);
        progressBar = findViewById(R.id.progressBar);
        recyclerSlots = findViewById(R.id.slotsRecyclerView);
        buttonJoinWaitlist = findViewById(R.id.buttonJoinWaitlistBooking);
        buttonJoinWaitlist.setOnClickListener(v ->
                joinWaitlist(counselorId, assessmentId,
                        getString(R.string.waitlist_reason_no_slots)));

        recyclerSlots.setLayoutManager(new LinearLayoutManager(this));
        slotAdapter = new TimeSlotAdapter(new ArrayList<>(), this);
        recyclerSlots.setAdapter(slotAdapter);

        calendarView.setMinDate(System.currentTimeMillis());
        calendarView.setOnDateClickListener(this::showSlotsForDate);

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
     * Loads available slots for the counselor. Checks the session cache first (pre-warmed
     * by StudentHomeActivity on login) so the calendar appears instantly without a Firestore
     * round-trip. On cache miss, falls back to a live fetch with Auth-UID → doc-ID fallback.
     *
     * @param initialDate The date to display on load, typically today.
     */
    private void loadSlots(String initialDate) {
        // Cache-first: slots were pre-warmed under both uid and doc-ID keys at login.
        List<TimeSlot> cached = SessionCache.getInstance().getSlots(counselorId);
        if (cached == null && counselorDocId != null && !counselorDocId.equals(counselorId)) {
            cached = SessionCache.getInstance().getSlots(counselorDocId);
        }
        if (cached != null) {
            schedule = AvailabilitySchedule.fromSlots(counselorId, cached);
            calendarView.setHighlightedDates(slotDates(cached));
            showSlotsForDate(initialDate);
            return;
        }

        // Cache miss — live Firestore fetch with Auth-UID → doc-ID fallback
        progressBar.setVisibility(View.VISIBLE);

        availabilityRepository.getAvailableSlotsForCounselor(counselorId,
                new AvailabilityRepository.OnSlotsLoadedCallback() {
                    @Override
                    public void onSuccess(List<TimeSlot> slots) {
                        if (!slots.isEmpty()) {
                            progressBar.setVisibility(View.GONE);
                            SessionCache.getInstance().putSlots(counselorId, slots);
                            schedule = AvailabilitySchedule.fromSlots(counselorId, slots);
                            calendarView.setHighlightedDates(slotDates(slots));
                            showSlotsForDate(initialDate);
                            return;
                        }
                        // Zero slots on the Auth-UID path — try the Firestore doc ID
                        String fallback = counselorDocId;
                        if (fallback != null && !fallback.equals(counselorId)) {
                            availabilityRepository.getAvailableSlotsForCounselor(fallback,
                                    new AvailabilityRepository.OnSlotsLoadedCallback() {
                                        @Override
                                        public void onSuccess(List<TimeSlot> fbSlots) {
                                            progressBar.setVisibility(View.GONE);
                                            String key = fbSlots.isEmpty() ? counselorId : fallback;
                                            // Cache under both keys so next open is instant
                                            SessionCache.getInstance().putSlots(counselorId, fbSlots);
                                            SessionCache.getInstance().putSlots(fallback, fbSlots);
                                            schedule = AvailabilitySchedule.fromSlots(key, fbSlots);
                                            calendarView.setHighlightedDates(slotDates(fbSlots));
                                            showSlotsForDate(initialDate);
                                        }
                                        @Override
                                        public void onFailure(Exception e) {
                                            progressBar.setVisibility(View.GONE);
                                            showSlotsForDate(initialDate);
                                        }
                                    });
                        } else {
                            progressBar.setVisibility(View.GONE);
                            SessionCache.getInstance().putSlots(counselorId, slots);
                            schedule = AvailabilitySchedule.fromSlots(counselorId, slots);
                            calendarView.setHighlightedDates(slotDates(slots));
                            showSlotsForDate(initialDate);
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        progressBar.setVisibility(View.GONE);
                        android.util.Log.e("BOOKING", "slot load failed: " + e.getMessage());
                        Toast.makeText(BookingActivity.this,
                                getString(R.string.error_booking_failed),
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
            textNoSlots.setText(R.string.no_slots_waitlist_message);
            buttonJoinWaitlist.setVisibility(View.VISIBLE);
            recyclerSlots.setVisibility(View.GONE);
            labelSlots.setVisibility(View.VISIBLE);
            return;
        }

        List<TimeSlot> slots = schedule.getSlotsForDate(date);
        labelSlots.setVisibility(View.VISIBLE);

        if (slots.isEmpty()) {
            textNoSlots.setVisibility(View.VISIBLE);
            recyclerSlots.setVisibility(View.GONE);
            if (schedule.getDatesWithAvailability().isEmpty()) {
                textNoSlots.setText(R.string.no_slots_waitlist_message);
                buttonJoinWaitlist.setVisibility(View.VISIBLE);
            } else {
                textNoSlots.setText(R.string.no_slots_for_date);
                buttonJoinWaitlist.setVisibility(View.GONE);
            }
        } else {
            textNoSlots.setVisibility(View.GONE);
            recyclerSlots.setVisibility(View.VISIBLE);
            buttonJoinWaitlist.setVisibility(View.GONE);
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
                            SessionCache.getInstance().invalidateAppointments();
                            SessionCache.getInstance().invalidateSlots(counselorId, counselorDocId);
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

    private Set<String> slotDates(List<TimeSlot> slots) {
        Set<String> dates = new HashSet<>();
        for (TimeSlot s : slots) {
            if (s.getDate() != null && !s.getDate().isEmpty()) {
                dates.add(s.getDate());
            }
        }
        return dates;
    }

    private void joinWaitlist(String waitlistCounselorId, String assessmentId, String reason) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, R.string.error_login_required, Toast.LENGTH_SHORT).show();
            return;
        }

        WaitlistEntry entry = new WaitlistEntry(
                user.getUid(),
                waitlistCounselorId,
                assessmentId,
                reason);
        waitlistRepository.joinWaitlist(entry, new WaitlistRepository.OnWaitlistActionCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(BookingActivity.this,
                        R.string.waitlist_joined,
                        Toast.LENGTH_LONG).show();
            }

            @Override
            public void onAlreadyWaitlisted() {
                Toast.makeText(BookingActivity.this,
                        R.string.waitlist_already_joined,
                        Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(BookingActivity.this,
                        R.string.waitlist_error,
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}
