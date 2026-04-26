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

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;

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

    /** Firebase Auth UID — used for slot/appointment queries (written to slot documents). */
    private String counselorId;
    /** Firestore document ID — used for profile reads/writes (may differ from Auth UID). */
    private String counselorDocId;
    private AppointmentRepository appointmentRepository;
    private AvailabilityRepository availabilityRepository;
    private CounselorRepository counselorRepository;

    /** Master list of all counselor appointments — never filtered in place. */
    private List<Appointment> masterAppointments = new ArrayList<>();

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

        counselorNameText = findViewById(R.id.counselorNameText);
        todayCount = findViewById(R.id.todaySessionCount);
        totalCount = findViewById(R.id.totalPatientsCount);
        weekCount = findViewById(R.id.weekSessionCount);
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
                        loadAppointments();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        counselorDocId = counselorId;
                        loadAppointments();
                    }
                });

        // "Add Availability" banner → AvailabilitySetupActivity
        // Slots use Auth UID as counselorId, so no doc ID needed here
        CardView addSlotBanner = findViewById(R.id.addSlotBanner);
        addSlotBanner.setOnClickListener(v ->
                startActivity(new Intent(this, AvailabilitySetupActivity.class)));

        // Edit profile button — pass the real Firestore doc ID for profile ops
        ImageButton editProfileButton = findViewById(R.id.buttonEditProfile);
        editProfileButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, CounselorProfileEditActivity.class);
            intent.putExtra("COUNSELOR_DOC_ID", counselorDocId);
            startActivity(intent);
        });

        // Logout
        ImageButton logoutBtn = findViewById(R.id.logoutBtn);
        logoutBtn.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

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

    @Override
    protected void onResume() {
        super.onResume();
        loadAppointments(); // Refresh on return from AvailabilitySetupActivity
    }

    /**
     * Fetches all appointments for this counselor via {@link AppointmentRepository}
     * and populates the master list. Defaults to "Today" tab on first load.
     */
    private void loadAppointments() {
        progressBar.setVisibility(View.VISIBLE);

        appointmentRepository.getAppointmentsForCounselor(counselorId,
                new AppointmentRepository.OnAppointmentsLoadedCallback() {
                    @Override
                    public void onSuccess(List<Appointment> appointments) {
                        progressBar.setVisibility(View.GONE);
                        masterAppointments = appointments;
                        int selectedTab = tabLayout.getSelectedTabPosition();
                        filterByTab(selectedTab < 0 ? 0 : selectedTab);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(CounselorDashboardActivity.this,
                                getString(R.string.error_loading_appointments),
                                Toast.LENGTH_LONG).show();
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

        adapter.setData(filtered);
        updateStats(today, filtered.size());
    }

    /**
     * Updates the three stat cards with accurate counts.
     * Today's sessions and total are always computed from the master list;
     * the third card reflects the currently filtered (tab) count.
     *
     * @param today         Today's date string in "yyyy-MM-dd" format.
     * @param filteredCount The count of appointments in the current tab view.
     */
    private void updateStats(String today, int filteredCount) {
        long todaySessionCount = masterAppointments.stream()
                .filter(a -> today.equals(a.getDate()))
                .count();

        todayCount.setText(String.valueOf(todaySessionCount));
        totalCount.setText(String.valueOf(masterAppointments.size()));
        weekCount.setText(String.valueOf(filteredCount));
    }
}
