package com.example.moogerscouncil;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class AddSlotActivity extends AppCompatActivity {

    private CustomCalendarView calendarView;
    private CardView cardSelectedInfo;
    private TextView textSelectedDate;
    private TextView textSelectedTime;
    private MaterialButton buttonCreateSlot;
    private FrameLayout progressOverlay;

    private String selectedDate = null;
    private String selectedTime = null;

    private String counselorId;
    private AvailabilityRepository availabilityRepository;
    private AvailabilitySettingsRepository settingsRepository;
    private WaitlistRepository waitlistRepository;
    private AppointmentRepository appointmentRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_slot);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        counselorId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        availabilityRepository = new AvailabilityRepository();
        settingsRepository = new AvailabilitySettingsRepository();
        waitlistRepository = new WaitlistRepository();
        appointmentRepository = new AppointmentRepository();

        calendarView = findViewById(R.id.calendarView);
        cardSelectedInfo = findViewById(R.id.cardSelectedInfo);
        textSelectedDate = findViewById(R.id.textSelectedDate);
        textSelectedTime = findViewById(R.id.textSelectedTime);
        buttonCreateSlot = findViewById(R.id.buttonCreateSlot);
        progressOverlay = findViewById(R.id.progressOverlay);

        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());

        calendarView.setMinDate(System.currentTimeMillis());
        calendarView.setOnDateClickListener(this::onDateSelected);

        buttonCreateSlot.setOnClickListener(v -> confirmCreateSlot());

        loadExistingSlotDates();
    }

    private void loadExistingSlotDates() {
        availabilityRepository.getSlotsForCounselor(counselorId,
                new AvailabilityRepository.OnSlotsLoadedCallback() {
                    @Override
                    public void onSuccess(List<TimeSlot> slots) {
                        Set<String> dates = new HashSet<>();
                        for (TimeSlot s : slots) dates.add(s.getDate());
                        calendarView.setHighlightedDates(dates);
                    }
                    @Override
                    public void onFailure(Exception e) { /* non-critical */ }
                });
    }

    private void onDateSelected(String date) {
        selectedDate = date;
        selectedTime = null;
        buttonCreateSlot.setVisibility(View.GONE);
        textSelectedTime.setVisibility(View.GONE);

        // Show date card immediately so the counselor sees the selection
        cardSelectedInfo.setVisibility(View.VISIBLE);
        textSelectedDate.setText(formatDate(date));

        Calendar now = Calendar.getInstance();
        new TimePickerDialog(this, (view, hour, minute) -> {
            selectedTime = String.format(Locale.US, "%02d:%02d", hour, minute);
            textSelectedTime.setText(selectedTime);
            textSelectedTime.setVisibility(View.VISIBLE);
            buttonCreateSlot.setVisibility(View.VISIBLE);
        }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show();
    }

    private void confirmCreateSlot() {
        if (selectedDate == null || selectedTime == null) return;
        progressOverlay.setVisibility(View.VISIBLE);
        buttonCreateSlot.setEnabled(false);

        settingsRepository.getSettings(counselorId,
                new AvailabilitySettingsRepository.OnSettingsLoadedCallback() {
                    @Override
                    public void onSuccess(AvailabilitySettings settings) {
                        validateAndCreate(settings.getBufferMinutes());
                    }
                    @Override
                    public void onFailure(Exception e) {
                        validateAndCreate(0);
                    }
                });
    }

    private void validateAndCreate(int bufferMinutes) {
        availabilityRepository.canAddSlotWithBuffer(counselorId, selectedDate, selectedTime,
                bufferMinutes,
                new AvailabilityRepository.OnBufferCheckCallback() {
                    @Override
                    public void onAvailable() {
                        availabilityRepository.addSlotAndReturn(counselorId, selectedDate,
                                selectedTime,
                                new AvailabilityRepository.OnSlotCreatedCallback() {
                                    @Override
                                    public void onSuccess(TimeSlot slot) {
                                        SessionCache.getInstance().invalidateSlots(counselorId);
                                        AppToast.show(AddSlotActivity.this,
                                                getString(R.string.slot_added),
                                                AppToast.LENGTH_SHORT);
                                        tryAutoResolveWaitlist(slot);
                                        finish();
                                    }
                                    @Override
                                    public void onFailure(Exception e) {
                                        progressOverlay.setVisibility(View.GONE);
                                        buttonCreateSlot.setEnabled(true);
                                        AppToast.show(AddSlotActivity.this,
                                                getString(R.string.error_adding_slot),
                                                AppToast.LENGTH_SHORT);
                                    }
                                });
                    }
                    @Override
                    public void onConflict(String reason) {
                        progressOverlay.setVisibility(View.GONE);
                        buttonCreateSlot.setEnabled(true);
                        AppToast.show(AddSlotActivity.this,
                                R.string.slot_conflicts_buffer, AppToast.LENGTH_LONG);
                    }
                    @Override
                    public void onFailure(Exception e) {
                        progressOverlay.setVisibility(View.GONE);
                        buttonCreateSlot.setEnabled(true);
                        AppToast.show(AddSlotActivity.this,
                                getString(R.string.error_adding_slot), AppToast.LENGTH_SHORT);
                    }
                });
    }

    private void tryAutoResolveWaitlist(TimeSlot slot) {
        waitlistRepository.getActiveWaitlistForCounselorOrdered(counselorId,
                new WaitlistRepository.OnWaitlistLoadedCallback() {
                    @Override
                    public void onSuccess(List<WaitlistEntry> entries) {
                        WaitlistEntry match = WaitlistMatcher.findFirstMatch(
                                entries, slot.getDate(), slot.getTime());
                        if (match == null) return;
                        appointmentRepository.bookAppointmentForWaitlist(
                                match.getStudentId(), counselorId, slot,
                                new AppointmentRepository.OnWaitlistBookingCallback() {
                                    @Override
                                    public void onSuccess(String appointmentId) {
                                        waitlistRepository.resolveEntry(match.getId(),
                                                slot.getId(), appointmentId,
                                                new WaitlistRepository.OnWaitlistSimpleCallback() {
                                                    @Override
                                                    public void onSuccess() {
                                                        AppToast.show(AddSlotActivity.this,
                                                                R.string.waitlist_auto_resolved,
                                                                AppToast.LENGTH_LONG);
                                                    }
                                                    @Override
                                                    public void onFailure(Exception e) { /* best-effort */ }
                                                });
                                    }
                                    @Override
                                    public void onSlotTaken() { /* already booked */ }
                                    @Override
                                    public void onFailure(Exception e) { /* best-effort */ }
                                });
                    }
                    @Override
                    public void onFailure(Exception e) { /* best-effort */ }
                });
    }

    private String formatDate(String raw) {
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(raw);
            return new SimpleDateFormat("EEE, MMM d", Locale.US).format(d);
        } catch (ParseException e) {
            return raw;
        }
    }
}
