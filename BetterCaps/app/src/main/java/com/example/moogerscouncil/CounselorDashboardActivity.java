/*
 * CounselorDashboardActivity.java
 * Role: Main screen for counselors. Shows a tabbed appointment list (Today/This Week/
 *       This Month), corrected stats, and entry points for editing their profile and
 *       managing availability slots. All Firestore operations go through repositories.
 *
 * Design pattern: Repository pattern (AppointmentRepository, AvailabilityRepository,
 *                 CounselorRepository).
 * Part of the BetterCAPS counseling platform.
 */
package com.example.moogerscouncil;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Dashboard for logged-in counselors.
 * Displays a TabLayout with Today / This Week / This Month appointment lists,
 * corrected session statistics, and navigation to profile editing and slot management.
 */
public class CounselorDashboardActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private AppointmentAdapter adapter;
    private TabLayout tabLayout;
    private TextView counselorNameText;
    private TextView todayCount;
    private TextView totalCount;
    private TextView weekCount;
    private TextView waitlistCount;

    /** Firebase Auth UID — used for slot/appointment queries (written to slot documents). */
    private String counselorId;
    /** Firestore document ID — used for profile reads/writes (may differ from Auth UID). */
    private String counselorDocId;
    private AppointmentRepository appointmentRepository;
    private AvailabilityRepository availabilityRepository;
    private CounselorRepository counselorRepository;
    private WaitlistRepository waitlistRepository;

    /** Master list of all counselor appointments — never filtered in place. */
    private List<Appointment> masterAppointments = new ArrayList<>();

    /** Real-time Firestore listener — pushes appointment changes automatically. */
    private ListenerRegistration appointmentListener;

    private TextView textSectionLabel;
    private int cachedWaitlistCount = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_counselor_dashboard);

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        appointmentRepository = new AppointmentRepository();
        availabilityRepository = new AvailabilityRepository();
        counselorRepository = new CounselorRepository();
        waitlistRepository = new WaitlistRepository();

        counselorNameText = findViewById(R.id.counselorNameText);
        todayCount = findViewById(R.id.todaySessionCount);
        totalCount = findViewById(R.id.totalPatientsCount);
        weekCount = findViewById(R.id.weekSessionCount);
        waitlistCount = findViewById(R.id.waitlistCount);
        progressBar = findViewById(R.id.progressBar);
        tabLayout = findViewById(R.id.tabLayout);

        recyclerView = findViewById(R.id.appointmentsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AppointmentAdapter(this, new ArrayList<>());
        recyclerView.setAdapter(adapter);

        // counselorId = Auth UID — used for slot/appointment queries because
        // AvailabilitySetupActivity and BookingActivity write documents with
        // FirebaseAuth.getCurrentUser().getUid() as the counselorId field.
        counselorId = mAuth.getCurrentUser().getUid();

        // Resolve the Firestore document ID for this counselor.
        // Counselors are pre-created in the Firebase console, so their document ID may differ
        // from their Auth UID. We match on the 'uid' field (stamped on first login) or, if
        // that field is not yet set, on the document ID equalling the Auth UID (old pattern
        // where the console operator used the Auth UID as the document ID).
        counselorRepository.getAllCounselors(
                new CounselorRepository.OnCounselorsLoadedCallback() {
                    @Override
                    public void onSuccess(List<Counselor> counselors) {
                        Counselor matched = null;

                        // Primary: find by uid field == Auth UID (set on previous login)
                        for (Counselor c : counselors) {
                            if (counselorId.equals(c.getUid())) {
                                matched = c;
                                break;
                            }
                        }

                        // Secondary: find by Firestore doc ID == Auth UID (old console pattern)
                        if (matched == null) {
                            for (Counselor c : counselors) {
                                if (counselorId.equals(c.getId())) {
                                    matched = c;
                                    break;
                                }
                            }
                        }

                        if (matched != null) {
                            counselorDocId = matched.getId();
                            String name = matched.getName();
                            counselorNameText.setText(name != null ? name : "Dr. Counselor");

                            // Stamp Auth UID onto the Firestore document if not already set,
                            // so CounselorAdapter can reliably pass the correct slot query ID
                            // to BookingActivity on the student side.
                            if (!counselorId.equals(matched.getUid())) {
                                counselorRepository.stampAuthUid(counselorDocId, counselorId,
                                        new CounselorRepository.OnUpdateCallback() {
                                            @Override public void onSuccess() {}
                                            @Override public void onFailure(Exception e) {}
                                        });
                            }
                        } else {
                            counselorDocId = counselorId; // last resort: assume doc ID == Auth UID
                        }
                        subscribeToAppointments();
                        loadWaitlistCount();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        counselorDocId = counselorId;
                        subscribeToAppointments();
                        loadWaitlistCount();
                    }
                });

        // Waitlist count card → CounselorWaitlistActivity
        CardView waitlistCountCard = findViewById(R.id.waitlistCountCard);
        waitlistCountCard.setOnClickListener(v -> {
            cachedWaitlistCount = -1;
            startActivity(new Intent(this, CounselorWaitlistActivity.class));
        });

        // "Add Availability" banner → AvailabilitySetupActivity
        // Slots use Auth UID as counselorId, so no doc ID needed here
        CardView addSlotBanner = findViewById(R.id.addSlotBanner);
        addSlotBanner.setOnClickListener(v ->
                startActivity(new Intent(this, AvailabilitySetupActivity.class)));
        textSectionLabel = findViewById(R.id.textSectionLabel);
        findViewById(R.id.buttonPastSessions).setOnClickListener(v ->
                startActivity(new Intent(this, PastSessionsActivity.class)));
        findViewById(R.id.buttonExportToCalendar).setOnClickListener(v ->
                exportNextAppointmentToCalendar());

        // Edit profile button — pass the real Firestore doc ID for profile ops
        ImageButton editProfileButton = findViewById(R.id.buttonEditProfile);
        editProfileButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, CounselorProfileEditActivity.class);
            intent.putExtra("COUNSELOR_DOC_ID", counselorDocId);
            startActivity(intent);
        });

        // Logout — two-step confirmation matching the student-side flow
        ImageButton logoutBtn = findViewById(R.id.logoutBtn);
        logoutBtn.setOnClickListener(v -> showLogoutConfirmation());

        // Tab selection
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                filterByTab(tab.getPosition());
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    /**
     * Shows the same custom logout confirmation dialog used on the student side.
     * Uses dialog_exit.xml for a consistent pastel-pink rounded visual.
     */
    private void showLogoutConfirmation() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_exit);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.88f);
            params.gravity = Gravity.CENTER;
            window.setAttributes(params);
        }

        MaterialButton btnConfirm = dialog.findViewById(R.id.btnExitConfirm);
        MaterialButton btnCancel  = dialog.findViewById(R.id.btnExitCancel);
        btnConfirm.setText(R.string.logout_confirm_yes);
        btnCancel.setText(R.string.button_cancel);

        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            SessionCache.getInstance().clearAll();
            mAuth.signOut();
            Intent intent = new Intent(CounselorDashboardActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finishAffinity();
        });
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (counselorId != null && appointmentListener == null) {
            subscribeToAppointments();
        }
        if (counselorDocId != null) {
            refreshCounselorName();
        }
        // Waitlist count: render cached value instantly, refresh in background only
        // on first load or if explicitly invalidated
        if (counselorId != null && cachedWaitlistCount < 0) {
            loadWaitlistCount();
        } else if (counselorId != null) {
            waitlistCount.setText(String.valueOf(cachedWaitlistCount));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (appointmentListener != null) {
            appointmentListener.remove();
            appointmentListener = null;
        }
    }

    private void refreshCounselorName() {
        counselorRepository.getCounselor(counselorDocId,
                new CounselorRepository.OnCounselorFetchedCallback() {
                    @Override
                    public void onSuccess(Counselor counselor) {
                        String name = counselor.getName();
                        if (name != null) {
                            counselorNameText.setText(name);
                        }
                    }
                    @Override
                    public void onFailure(Exception e) { }
                });
    }

    /**
     * Subscribes to real-time Firestore updates for this counselor's appointments.
     * Called once — renders from SessionCache instantly, then Firestore pushes
     * only real changes (new booking, cancellation, no-show) silently in the
     * background. The UI never rebuilds unless data actually changed.
     */
    private void subscribeToAppointments() {
        if (counselorId == null) return;

        // Render from cache immediately — zero wait, no spinner
        List<Appointment> cached = SessionCache.getInstance().getCounselorAppointments(counselorId);
        boolean hadCache = cached != null;
        if (hadCache) {
            masterAppointments = cached;
            int selectedTab = tabLayout.getSelectedTabPosition();
            filterByTab(selectedTab < 0 ? 0 : selectedTab);
        } else {
            progressBar.setVisibility(View.VISIBLE);
        }

        // Subscribe once — Firestore delivers the initial snapshot from its own
        // offline cache (redundant with ours, suppressed below) then pushes
        // only genuine server-side mutations going forward.
        final boolean[] initialSnapshotHandled = {false};
        appointmentListener = appointmentRepository.listenForCounselorAppointments(
                counselorId,
                new AppointmentRepository.OnAppointmentsLoadedCallback() {
                    @Override
                    public void onSuccess(List<Appointment> appointments) {
                        progressBar.setVisibility(View.GONE);

                        // Skip the first callback if we already rendered from SessionCache —
                        // it's the same data from Firestore's offline cache and causes a
                        // redundant UI rebuild / flicker.
                        if (!initialSnapshotHandled[0]) {
                            initialSnapshotHandled[0] = true;
                            if (hadCache && !appointmentsChanged(appointments)) {
                                return;
                            }
                        } else if (!appointmentsChanged(appointments)) {
                            return;
                        }

                        SessionCache.getInstance().putCounselorAppointments(counselorId, appointments);
                        masterAppointments = appointments;
                        int selectedTab = tabLayout.getSelectedTabPosition();
                        filterByTab(selectedTab < 0 ? 0 : selectedTab);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        progressBar.setVisibility(View.GONE);
                        if (!hadCache) {
                            AppToast.show(CounselorDashboardActivity.this,
                                    getString(R.string.error_loading_appointments),
                                    AppToast.LENGTH_LONG);
                        }
                    }
                });
    }

    /**
     * Filters the master appointment list by the selected tab's date range
     * and updates the adapter and stats.
     *
     * @param tabIndex 0 = Today, 1 = This Week, 2 = This Month.
     */
    private void filterByTab(int tabIndex) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String today = sdf.format(new Date());
        Calendar cal = Calendar.getInstance();

        List<Appointment> filtered;
        switch (tabIndex) {
            case 0: // Today
                filtered = masterAppointments.stream()
                        .filter(a -> today.equals(a.getDate()))
                        .collect(Collectors.toList());
                break;

            case 1: // This Week
                cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
                String weekStart = sdf.format(cal.getTime());
                cal.add(Calendar.DAY_OF_WEEK, 6);
                String weekEnd = sdf.format(cal.getTime());
                filtered = masterAppointments.stream()
                        .filter(a -> a.getDate() != null
                                && a.getDate().compareTo(weekStart) >= 0
                                && a.getDate().compareTo(weekEnd) <= 0)
                        .collect(Collectors.toList());
                break;

            case 2: // This Month
            default:
                String monthPrefix = today.substring(0, 7); // "yyyy-MM"
                filtered = masterAppointments.stream()
                        .filter(a -> a.getDate() != null
                                && a.getDate().startsWith(monthPrefix))
                        .collect(Collectors.toList());
                break;
        }

        List<Appointment> visible = filtered.stream()
                .filter(a -> !"CANCELLED".equals(a.getStatus())
                        && !"NO_SHOW".equals(a.getStatus()))
                .collect(Collectors.toList());
        adapter.setData(visible);
        updateStats(today, filtered);
    }

    /**
     * Updates the three stat cards — all counts show only CONFIRMED (active) appointments
     * so the dashboard reflects current bookings, not historical totals.
     *
     * @param today        Today's date string in "yyyy-MM-dd" format.
     * @param tabFiltered  The date-filtered list for the current tab (pre-filtered by date range).
     */
    private void updateStats(String today, List<Appointment> tabFiltered) {
        long todayConfirmed = masterAppointments.stream()
                .filter(a -> today.equals(a.getDate()) && "CONFIRMED".equals(a.getStatus()))
                .count();
        long totalConfirmed = masterAppointments.stream()
                .filter(a -> "CONFIRMED".equals(a.getStatus()))
                .count();
        long tabConfirmed = tabFiltered.stream()
                .filter(a -> "CONFIRMED".equals(a.getStatus()))
                .count();

        todayCount.setText(String.valueOf(todayConfirmed));
        totalCount.setText(String.valueOf(totalConfirmed));
        weekCount.setText(String.valueOf(tabConfirmed));
    }

    /**
     * Checks if the incoming appointment list differs from what's currently displayed.
     * Prevents redundant UI rebuilds when the snapshot listener fires but nothing changed.
     */
    static boolean appointmentsChanged(List<Appointment> current, List<Appointment> incoming) {
        if (incoming.size() != current.size()) return true;
        for (int i = 0; i < incoming.size(); i++) {
            Appointment a = current.get(i);
            Appointment b = incoming.get(i);
            if (a.getId() == null || !a.getId().equals(b.getId())) return true;
            if (a.getStatus() == null || !a.getStatus().equals(b.getStatus())) return true;
        }
        return false;
    }

    private boolean appointmentsChanged(List<Appointment> incoming) {
        if (incoming.size() != masterAppointments.size()) return true;
        for (int i = 0; i < incoming.size(); i++) {
            Appointment a = masterAppointments.get(i);
            Appointment b = incoming.get(i);
            if (a.getId() == null || !a.getId().equals(b.getId())) return true;
            if (a.getStatus() == null || !a.getStatus().equals(b.getStatus())) return true;
        }
        return false;
    }

    private void loadWaitlistCount() {
        if (cachedWaitlistCount >= 0) {
            waitlistCount.setText(String.valueOf(cachedWaitlistCount));
        }
        waitlistRepository.getActiveWaitlistCountForCounselor(counselorId,
                new WaitlistRepository.OnWaitlistCountCallback() {
                    @Override
                    public void onSuccess(int count) {
                        if (count != cachedWaitlistCount) {
                            cachedWaitlistCount = count;
                            waitlistCount.setText(String.valueOf(count));
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        if (cachedWaitlistCount < 0) {
                            waitlistCount.setText("0");
                        }
                    }
                });
    }

    private void exportNextAppointmentToCalendar() {
        Appointment next = null;
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        for (Appointment appointment : masterAppointments) {
            if (!"CONFIRMED".equals(appointment.getStatus())) continue;
            if (appointment.getDate() == null || appointment.getDate().compareTo(today) < 0) {
                continue;
            }
            if (next == null || appointment.getDate().compareTo(next.getDate()) < 0) {
                next = appointment;
            }
        }
        if (next == null) {
            AppToast.show(this, R.string.no_appointments_to_export, AppToast.LENGTH_SHORT);
            return;
        }
        try {
            String counselorName = counselorNameText.getText() == null
                    ? null : counselorNameText.getText().toString();
            startActivity(CalendarSyncHelper.buildInsertEventIntent(next, counselorName));
        } catch (ActivityNotFoundException e) {
            AppToast.show(this, R.string.calendar_export_unavailable, AppToast.LENGTH_LONG);
        }
    }
}
