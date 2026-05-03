/*
 * CounselorWaitlistActivity.java
 * Role: Shows the counselor their active waitlist queue in FIFO order.
 *       "Create Slot for Student" opens a pastel dialog pre-populated with the
 *       student's preferred dates and time window; confirming creates the slot and
 *       auto-resolves the waitlist entry for that student.
 *
 * Design pattern: Activity backed by WaitlistRepository, AvailabilityRepository,
 *                 AppointmentRepository, and UserRepository.
 * Part of the BetterCAPS counseling platform.
 */
package com.example.moogerscouncil;

import android.app.Dialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListPopupWindow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Displays the counselor's active waitlist entries sorted FIFO (earliest request first).
 * The "Create Slot" dialog restricts date and time choices to the student's stated preferences.
 */
public class CounselorWaitlistActivity extends AppCompatActivity {

    private RecyclerView recycler;
    private TextView textEmpty;
    private WaitlistRepository waitlistRepository;
    private AvailabilityRepository availabilityRepository;
    private AppointmentRepository appointmentRepository;
    private UserRepository userRepository;
    private String counselorId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_counselor_waitlist);

        waitlistRepository = new WaitlistRepository();
        availabilityRepository = new AvailabilityRepository();
        appointmentRepository = new AppointmentRepository();
        userRepository = new UserRepository();

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }
        counselorId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        recycler = findViewById(R.id.recyclerCounselorWaitlist);
        textEmpty = findViewById(R.id.textWaitlistEmpty);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        loadQueue();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadQueue();
    }

    private void loadQueue() {
        waitlistRepository.getActiveWaitlistForCounselorOrdered(counselorId,
                new WaitlistRepository.OnWaitlistLoadedCallback() {
                    @Override
                    public void onSuccess(List<WaitlistEntry> entries) {
                        if (entries.isEmpty()) {
                            textEmpty.setVisibility(View.VISIBLE);
                            recycler.setVisibility(View.GONE);
                            return;
                        }
                        textEmpty.setVisibility(View.GONE);
                        recycler.setVisibility(View.VISIBLE);
                        resolveStudentNames(entries);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        AppToast.show(CounselorWaitlistActivity.this,
                                R.string.waitlist_load_error, AppToast.LENGTH_SHORT);
                    }
                });
    }

    private void resolveStudentNames(List<WaitlistEntry> entries) {
        Map<String, String> names = new HashMap<>();
        AtomicInteger remaining = new AtomicInteger(entries.size());
        for (WaitlistEntry entry : entries) {
            String studentId = entry.getStudentId();
            userRepository.getUserName(studentId, name -> {
                names.put(studentId, name);
                if (remaining.decrementAndGet() == 0) showList(entries, names);
            });
        }
    }

    private void showList(List<WaitlistEntry> entries, Map<String, String> names) {
        CounselorWaitlistAdapter adapter = new CounselorWaitlistAdapter(
                this, entries, names,
                entry -> showCreateSlotDialog(entry, names.get(entry.getStudentId())));
        recycler.setAdapter(adapter);
    }

    // -------------------------------------------------------------------------
    // Create-slot dialog
    // -------------------------------------------------------------------------

    /**
     * Shows a pastel dialog pre-populated with the student's preferred dates and
     * time window. On confirm, creates the slot and auto-books it for the student.
     */
    private void showCreateSlotDialog(WaitlistEntry entry, String studentName) {
        List<String> dateOptions = entry.getPreferredDates();
        List<String> timeOptions = buildTimesInRange(
                entry.getPreferredStartTime(), entry.getPreferredEndTime());

        if (dateOptions == null || dateOptions.isEmpty()
                || timeOptions.isEmpty()) {
            AppToast.show(this, R.string.waitlist_load_error, AppToast.LENGTH_SHORT);
            return;
        }

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_create_slot);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.88),
                    WindowManager.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.CENTER);
            window.setDimAmount(0.5f);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }

        TextView textStudent = dialog.findViewById(R.id.textSlotStudentName);
        textStudent.setText(studentName != null ? "for " + studentName : "");

        // Mutable holders for selected values
        final String[] selectedDate = {dateOptions.get(0)};
        final String[] selectedTime = {timeOptions.get(0)};

        TextView textDate = dialog.findViewById(R.id.textSlotDate);
        TextView textTime = dialog.findViewById(R.id.textSlotTime);
        LinearLayout selectorDate = dialog.findViewById(R.id.selectorSlotDate);
        LinearLayout selectorTime = dialog.findViewById(R.id.selectorSlotTime);
        MaterialButton btnConfirm = dialog.findViewById(R.id.btnCreateSlotConfirm);
        MaterialButton btnCancel = dialog.findViewById(R.id.btnCreateSlotCancel);

        textDate.setText(selectedDate[0]);
        textTime.setText(selectedTime[0]);

        selectorDate.setOnClickListener(v ->
                showPopup(selectorDate, dateOptions, picked -> {
                    selectedDate[0] = picked;
                    textDate.setText(picked);
                }));

        selectorTime.setOnClickListener(v ->
                showPopup(selectorTime, timeOptions, picked -> {
                    selectedTime[0] = picked;
                    textTime.setText(picked);
                }));

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            btnConfirm.setEnabled(false);
            createSlotAndResolve(entry, selectedDate[0], selectedTime[0], dialog);
        });

        dialog.show();
    }

    /**
     * Shows a {@link ListPopupWindow} anchored below the given view.
     */
    private void showPopup(View anchor, List<String> options, OnPickedListener listener) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, R.layout.item_time_option, options);

        ListPopupWindow popup = new ListPopupWindow(this);
        popup.setAdapter(adapter);
        popup.setAnchorView(anchor);
        popup.setModal(true);
        popup.setWidth(anchor.getWidth());
        popup.setBackgroundDrawable(
                ContextCompat.getDrawable(this, R.drawable.bg_time_popup));
        popup.setDropDownGravity(Gravity.TOP);
        popup.setVerticalOffset(4);

        popup.setOnItemClickListener((parent, view, position, id) -> {
            listener.onPicked((String) parent.getItemAtPosition(position));
            popup.dismiss();
        });

        popup.show();
    }

    private interface OnPickedListener {
        void onPicked(String value);
    }

    /**
     * Creates the slot via {@link AvailabilityRepository}, then books it for the
     * specific student and resolves their waitlist entry atomically.
     */
    private void createSlotAndResolve(WaitlistEntry entry, String date, String time,
                                       Dialog dialog) {
        availabilityRepository.addSlotAndReturn(counselorId, date, time,
                new AvailabilityRepository.OnSlotCreatedCallback() {
                    @Override
                    public void onSuccess(TimeSlot slot) {
                        appointmentRepository.bookAppointmentForWaitlist(
                                entry.getStudentId(), counselorId, slot,
                                new AppointmentRepository.OnWaitlistBookingCallback() {
                                    @Override
                                    public void onSuccess(String appointmentId) {
                                        waitlistRepository.resolveEntry(
                                                entry.getId(), slot.getId(), appointmentId,
                                                new WaitlistRepository.OnWaitlistSimpleCallback() {
                                                    @Override
                                                    public void onSuccess() {
                                                        dialog.dismiss();
                                                        AppToast.show(CounselorWaitlistActivity.this,
                                                                R.string.waitlist_auto_resolved,
                                                                AppToast.LENGTH_LONG);
                                                        loadQueue();
                                                    }

                                                    @Override
                                                    public void onFailure(Exception e) {
                                                        dialog.dismiss();
                                                        AppToast.show(CounselorWaitlistActivity.this,
                                                                R.string.waitlist_auto_resolve_error,
                                                                AppToast.LENGTH_LONG);
                                                        loadQueue();
                                                    }
                                                });
                                    }

                                    @Override
                                    public void onSlotTaken() {
                                        dialog.dismiss();
                                        AppToast.show(CounselorWaitlistActivity.this,
                                                R.string.waitlist_auto_resolve_error,
                                                AppToast.LENGTH_SHORT);
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        dialog.dismiss();
                                        AppToast.show(CounselorWaitlistActivity.this,
                                                R.string.waitlist_auto_resolve_error,
                                                AppToast.LENGTH_SHORT);
                                    }
                                });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        MaterialButton btn = dialog.findViewById(R.id.btnCreateSlotConfirm);
                        if (btn != null) btn.setEnabled(true);
                        AppToast.show(CounselorWaitlistActivity.this,
                                R.string.waitlist_auto_resolve_error, AppToast.LENGTH_SHORT);
                    }
                });
    }

    /**
     * Generates all 30-minute-interval times from {@code startTime} (inclusive)
     * up to {@code endTime} (exclusive).
     */
    private static List<String> buildTimesInRange(String startTime, String endTime) {
        List<String> times = new ArrayList<>();
        if (startTime == null || endTime == null) return times;
        int s = toMinutes(startTime);
        int e = toMinutes(endTime);
        for (int m = s; m < e; m += 30) {
            times.add(String.format(Locale.US, "%02d:%02d", m / 60, m % 60));
        }
        return times;
    }

    private static int toMinutes(String hhmm) {
        String[] parts = hhmm.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }
}
