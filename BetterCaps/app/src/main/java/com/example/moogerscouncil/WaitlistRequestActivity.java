/*
 * WaitlistRequestActivity.java
 * Role: Lets a student specify scheduling preferences (multi-date, time window, optional note)
 *       when joining a counselor's waitlist. Blocks submission if a matching available slot
 *       already exists so the student can book directly instead.
 *
 * Design pattern: Activity backed by WaitlistRepository and AvailabilityRepository.
 * Part of the BetterCAPS counseling platform.
 */
package com.example.moogerscouncil;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListPopupWindow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * Student preference form for joining a counselor's waitlist.
 *
 * <p>Time window rule: end time must be at least 30 minutes after start time.
 * The end time popup is rebuilt on every start time change to only show valid options.</p>
 *
 * <p>Guard logic: if at least one existing available slot already falls within the
 * student's selected dates and time window, the form is blocked and the student
 * is directed to book directly.</p>
 */
public class WaitlistRequestActivity extends AppCompatActivity {

    public static final String EXTRA_COUNSELOR_ID = "counselorId";
    public static final String EXTRA_ASSESSMENT_ID = "assessmentId";
    public static final String EXTRA_COUNSELOR_NAME = "counselorName";

    private CustomCalendarView calendarView;
    private LinearLayout selectorStartTime;
    private LinearLayout selectorEndTime;
    private TextView textStartTime;
    private TextView textEndTime;
    private EditText editNote;
    private Button buttonSubmit;

    private String selectedStart;
    private String selectedEnd;
    private List<String> currentEndOptions = new ArrayList<>();

    private final List<String> selectedDates = new ArrayList<>();

    private WaitlistRepository waitlistRepository;
    private AvailabilityRepository availabilityRepository;
    private AppointmentRepository appointmentRepository;

    private String counselorId;
    private String assessmentId;
    private String counselorName;

    private static final String[] ALL_TIMES = {
            "08:00", "08:30", "09:00", "09:30", "10:00", "10:30",
            "11:00", "11:30", "12:00", "12:30", "13:00", "13:30",
            "14:00", "14:30", "15:00", "15:30", "16:00", "16:30", "17:00"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waitlist_request);

        counselorId = getIntent().getStringExtra(EXTRA_COUNSELOR_ID);
        assessmentId = getIntent().getStringExtra(EXTRA_ASSESSMENT_ID);
        counselorName = getIntent().getStringExtra(EXTRA_COUNSELOR_NAME);

        waitlistRepository = new WaitlistRepository();
        availabilityRepository = new AvailabilityRepository();
        appointmentRepository = new AppointmentRepository();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        calendarView = findViewById(R.id.waitlistCalendar);
        selectorStartTime = findViewById(R.id.selectorStartTime);
        selectorEndTime = findViewById(R.id.selectorEndTime);
        textStartTime = findViewById(R.id.textStartTime);
        textEndTime = findViewById(R.id.textEndTime);
        editNote = findViewById(R.id.editWaitlistNote);
        buttonSubmit = findViewById(R.id.buttonSubmitWaitlist);

        // Default: start = 09:00, end = 09:30
        selectedStart = "09:00";
        selectedEnd = "09:30";
        textStartTime.setText(selectedStart);
        textEndTime.setText(selectedEnd);
        currentEndOptions = buildEndOptions(selectedStart);

        selectorStartTime.setOnClickListener(v -> showTimePopup(selectorStartTime, true));
        selectorEndTime.setOnClickListener(v -> showTimePopup(selectorEndTime, false));

        calendarView.setOnDateClickListener(date -> {
            if (selectedDates.contains(date)) {
                selectedDates.remove(date);
            } else {
                selectedDates.add(date);
            }
            calendarView.setHighlightedDates(new HashSet<>(selectedDates));
        });

        buttonSubmit.setOnClickListener(v -> validateAndSubmit());
    }

    /**
     * Shows a {@link ListPopupWindow} anchored directly below the given selector view.
     * For the start picker, rebuilds end options after selection.
     */
    private void showTimePopup(View anchor, boolean isStart) {
        List<String> options = isStart
                ? new ArrayList<>(Arrays.asList(ALL_TIMES)).subList(0, ALL_TIMES.length - 1)
                : currentEndOptions;

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, R.layout.item_time_option, new ArrayList<>(options));

        ListPopupWindow popup = new ListPopupWindow(this);
        popup.setAdapter(adapter);
        popup.setAnchorView(anchor);
        popup.setModal(true);
        popup.setWidth(anchor.getWidth());
        popup.setBackgroundDrawable(
                ContextCompat.getDrawable(this, R.drawable.bg_time_popup));
        // Always drop down below the anchor
        popup.setDropDownGravity(android.view.Gravity.TOP);
        popup.setVerticalOffset(4);

        popup.setOnItemClickListener((parent, view, position, id) -> {
            String picked = (String) parent.getItemAtPosition(position);
            popup.dismiss();
            if (isStart) {
                selectedStart = picked;
                textStartTime.setText(picked);
                currentEndOptions = buildEndOptions(picked);
                // If current end is no longer valid, snap to first valid option
                if (!currentEndOptions.contains(selectedEnd)) {
                    selectedEnd = currentEndOptions.get(0);
                    textEndTime.setText(selectedEnd);
                }
            } else {
                selectedEnd = picked;
                textEndTime.setText(picked);
            }
        });

        popup.show();
    }

    /**
     * Builds the list of valid end times: all slots at least 30 minutes after startTime.
     */
    List<String> buildEndOptions(String startTime) {
        List<String> opts = new ArrayList<>();
        int startMins = toMinutes(startTime);
        for (String t : ALL_TIMES) {
            if (toMinutes(t) - startMins >= 30) {
                opts.add(t);
            }
        }
        return opts;
    }

    static int toMinutes(String hhmm) {
        String[] parts = hhmm.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }

    private void validateAndSubmit() {
        if (selectedDates.isEmpty()) {
            AppToast.show(this, R.string.waitlist_no_dates_selected, AppToast.LENGTH_SHORT);
            return;
        }

        if (toMinutes(selectedEnd) - toMinutes(selectedStart) < 30) {
            AppToast.show(this, R.string.waitlist_invalid_time_range, AppToast.LENGTH_SHORT);
            return;
        }

        buttonSubmit.setEnabled(false);
        AppToast.show(this, R.string.waitlist_checking_slots, AppToast.LENGTH_SHORT);

        availabilityRepository.getAvailableSlotsForCounselor(counselorId,
                new AvailabilityRepository.OnSlotsLoadedCallback() {
                    @Override
                    public void onSuccess(List<TimeSlot> slots) {
                        TimeSlot match = WaitlistMatcher.findFirstMatchingSlot(
                                slots, selectedDates, selectedStart, selectedEnd);
                        if (match != null) {
                            buttonSubmit.setEnabled(true);
                            showSlotAvailableDialog(match);
                            return;
                        }
                        submitWaitlistEntry(selectedStart, selectedEnd);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        buttonSubmit.setEnabled(true);
                        AppToast.show(WaitlistRequestActivity.this,
                                R.string.waitlist_join_error, AppToast.LENGTH_SHORT);
                    }
                });
    }

    private void submitWaitlistEntry(String startTime, String endTime) {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) {
            buttonSubmit.setEnabled(true);
            AppToast.show(this, R.string.waitlist_join_error, AppToast.LENGTH_SHORT);
            return;
        }

        String note = editNote.getText().toString().trim();
        WaitlistEntry entry = new WaitlistEntry(uid, counselorId,
                new ArrayList<>(selectedDates), startTime, endTime,
                note.isEmpty() ? null : note, assessmentId);

        waitlistRepository.joinWaitlist(entry, new WaitlistRepository.OnWaitlistActionCallback() {
            @Override
            public void onSuccess() {
                AppToast.scheduleForNextActivity(WaitlistRequestActivity.this,
                        R.string.waitlist_join_success, AppToast.LENGTH_LONG);
                Intent home = new Intent(WaitlistRequestActivity.this, StudentHomeActivity.class);
                home.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(home);
                finish();
            }

            @Override
            public void onAlreadyWaitlisted() {
                AppToast.scheduleForNextActivity(WaitlistRequestActivity.this,
                        R.string.waitlist_already_on_list, AppToast.LENGTH_LONG);
                Intent home = new Intent(WaitlistRequestActivity.this, StudentHomeActivity.class);
                home.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(home);
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                buttonSubmit.setEnabled(true);
                AppToast.show(WaitlistRequestActivity.this,
                        R.string.waitlist_join_error, AppToast.LENGTH_SHORT);
            }
        });
    }

    private void showSlotAvailableDialog(TimeSlot slot) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_slot_available);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.88),
                    WindowManager.LayoutParams.WRAP_CONTENT);
            window.setGravity(android.view.Gravity.CENTER);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.5f);
        }

        String body = getString(R.string.dialog_slot_available_body, slot.getDate(), slot.getTime());
        ((TextView) dialog.findViewById(R.id.textSlotDetails)).setText(body);

        dialog.findViewById(R.id.btnBookNow).setOnClickListener(v -> {
            dialog.dismiss();
            // Use the exact same BookingConfirmationFragment flow as BookingActivity
            BookingConfirmationFragment fragment =
                    BookingConfirmationFragment.newInstance(counselorName, slot);
            fragment.setOnConfirmListener(confirmedSlot -> {
                String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                        ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
                if (uid == null) {
                    AppToast.show(this, R.string.waitlist_join_error, AppToast.LENGTH_SHORT);
                    return;
                }
                appointmentRepository.bookAppointment(uid, counselorId, confirmedSlot,
                        new AppointmentRepository.OnBookingCallback() {
                            @Override
                            public void onSuccess() {
                                SessionCache.getInstance().invalidateAppointments();
                                SessionCache.getInstance().invalidateSlots(counselorId);
                                AppToast.scheduleForNextActivity(WaitlistRequestActivity.this,
                                        R.string.dialog_slot_booking_success,
                                        AppToast.LENGTH_SHORT);
                                Intent home = new Intent(WaitlistRequestActivity.this,
                                        StudentHomeActivity.class);
                                home.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                startActivity(home);
                                finish();
                            }

                            @Override
                            public void onSlotTaken() {
                                AppToast.show(WaitlistRequestActivity.this,
                                        R.string.dialog_slot_taken_fallback,
                                        AppToast.LENGTH_LONG);
                                submitWaitlistEntry(selectedStart, selectedEnd);
                            }

                            @Override
                            public void onFailure(Exception e) {
                                AppToast.show(WaitlistRequestActivity.this,
                                        R.string.waitlist_join_error, AppToast.LENGTH_SHORT);
                            }
                        });
            });
            fragment.show(getSupportFragmentManager(), "booking_confirm");
        });

        dialog.findViewById(R.id.btnJoinWaitlistInstead).setOnClickListener(v -> {
            dialog.dismiss();
            submitWaitlistEntry(selectedStart, selectedEnd);
        });

        dialog.show();
    }
}
