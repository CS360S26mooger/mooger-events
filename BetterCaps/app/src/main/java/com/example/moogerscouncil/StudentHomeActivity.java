package com.example.moogerscouncil;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RatingBar;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Student home dashboard shown after successful login or registration.
 * Displays the next upcoming CONFIRMED appointment, crisis support, and navigation.
 * Slide-to-cancel is wired to Firestore via AppointmentRepository.
 */
public class StudentHomeActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private TextView counselorNameText, counselorRoleText, welcomeNameText;
    private TextView upcomingSessionDate, upcomingSessionTime;
    private boolean isOverlayActive = false;
    private View privacyOverlay;
    private UserRepository userRepository;

    // Home specialist list
    private androidx.recyclerview.widget.RecyclerView homeSpecialistRecycler;
    private CounselorAdapter homeSpecialistAdapter;
    private final java.util.List<Counselor> allCounselors = new java.util.ArrayList<>();
    private TextView textNoSpecialists;
    private android.widget.TextView activeChip;

    // Upcoming session
    private Appointment upcomingAppointment;
    private View sessionTimeRow;
    private View sliderContainer;
    private SeekBar slideToCancelSlider;
    private View slideFill;

    // Feedback
    private FeedbackRepository feedbackRepository;
    private AppointmentRepository appointmentRepository;
    private IntakeAssessmentRepository intakeAssessmentRepository;
    private WaitlistRepository waitlistRepository;
    private CardView cardFeedbackPrompt;
    private TextView textFeedbackPromptSubtitle;
    private MaterialButton buttonGiveFeedback;
    private MaterialButton buttonDismissFeedback;
    private Appointment pendingFeedbackAppointment;
    private boolean feedbackDismissed = false;
    private CardView cardLatestMatch;
    private TextView textLatestMatchSubtitle;
    private CardView cardWaitlistStatus;
    private TextView textWaitlistStatusSubtitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_home);

        mAuth = FirebaseAuth.getInstance();
        userRepository = new UserRepository();
        feedbackRepository = new FeedbackRepository();
        appointmentRepository = new AppointmentRepository();
        intakeAssessmentRepository = new IntakeAssessmentRepository();
        waitlistRepository = new WaitlistRepository();

        counselorNameText     = findViewById(R.id.upcomingCounselorName);
        counselorRoleText     = findViewById(R.id.upcomingCounselorRole);
        welcomeNameText       = findViewById(R.id.welcomeName);
        privacyOverlay        = findViewById(R.id.privacyOverlay);
        upcomingSessionDate   = findViewById(R.id.upcomingSessionDate);
        upcomingSessionTime   = findViewById(R.id.upcomingSessionTime);
        sessionTimeRow        = findViewById(R.id.sessionTimeRow);
        sliderContainer       = findViewById(R.id.sliderContainer);
        slideToCancelSlider   = findViewById(R.id.slideToCancelSlider);
        slideFill             = findViewById(R.id.slideFill);

        cardFeedbackPrompt       = findViewById(R.id.cardFeedbackPrompt);
        textFeedbackPromptSubtitle = findViewById(R.id.textFeedbackPromptSubtitle);
        buttonGiveFeedback       = findViewById(R.id.buttonGiveFeedback);
        buttonDismissFeedback    = findViewById(R.id.buttonDismissFeedback);
        cardLatestMatch          = findViewById(R.id.cardLatestMatch);
        textLatestMatchSubtitle  = findViewById(R.id.textLatestMatchSubtitle);
        cardWaitlistStatus       = findViewById(R.id.cardWaitlistStatus);
        textWaitlistStatusSubtitle = findViewById(R.id.textWaitlistStatusSubtitle);

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        redirectCounselorSessionIfNeeded();

        // Fetch display name — cache-first to avoid delay on re-opens
        String uid = user.getUid();
        Student cachedStudent = SessionCache.getInstance().getStudent(uid);
        if (cachedStudent != null) {
            applyStudentName(cachedStudent);
        } else {
            userRepository.getCurrentUser(new UserRepository.OnUserFetchedCallback() {
                @Override
                public void onSuccess(Student student) {
                    SessionCache.getInstance().putStudent(student);
                    applyStudentName(student);
                }
                @Override
                public void onFailure(Exception e) { welcomeNameText.setText("Student"); }
            });
        }

        // Eye-slash button inside the fake app overlay
        ImageButton overlayExitBtn = privacyOverlay.findViewById(R.id.overlayExitBtn);
        if (overlayExitBtn == null) throw new IllegalStateException("overlayExitBtn missing");
        overlayExitBtn.setOnClickListener(v -> {
            isOverlayActive = false;
            privacyOverlay.setVisibility(View.GONE);
        });

        // Crisis banner
        CardView crisisBanner = findViewById(R.id.crisisBanner);
        crisisBanner.setOnClickListener(v ->
            EmergencyDialogFragment.newInstance().show(getSupportFragmentManager(), "emergency"));

        // Find My Match
        CardView findMatchCard = findViewById(R.id.findMatchCard);
        findMatchCard.setOnClickListener(v -> startActivity(new Intent(this, QuizActivity.class)));

        // AI Chat placeholder
        CardView aiChatCard = findViewById(R.id.aiChatCard);
        aiChatCard.setOnClickListener(v ->
            AppToast.show(this, "AI Chat coming soon!", AppToast.LENGTH_SHORT));

        // Slide-to-cancel SeekBar
        float thumbPx = 42 * getResources().getDisplayMetrics().density;
        slideToCancelSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int containerWidth = seekBar.getWidth();
                float thumbLeft = (progress / 100f) * (containerWidth - thumbPx);
                int fillWidth = Math.round(thumbLeft + thumbPx);
                android.view.ViewGroup.LayoutParams lp = slideFill.getLayoutParams();
                lp.width = fillWidth;
                slideFill.setLayoutParams(lp);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (seekBar.getProgress() >= 95) {
                    handleCancellation();
                } else {
                    resetSlider(thumbPx);
                }
            }
        });
        // Pre-set fill width once view is attached (safe even when container is gone)
        slideFill.post(() -> {
            android.view.ViewGroup.LayoutParams lp = slideFill.getLayoutParams();
            lp.width = Math.round(thumbPx);
            slideFill.setLayoutParams(lp);
        });

        // Post-session feedback button
        findViewById(R.id.btnPostSessionFeedback).setOnClickListener(v -> {
            if (pendingFeedbackAppointment != null) {
                showFeedbackDialog(pendingFeedbackAppointment.getId());
            } else {
                AppToast.show(this, getString(R.string.no_pending_feedback),
                        AppToast.LENGTH_SHORT);
            }
        });

        // Search bar — only navigator to full directory
        findViewById(R.id.searchBarHome).setOnClickListener(v ->
                startActivity(new Intent(this, CounselorListActivity.class)));

        // Home specialist RecyclerView — live counselors, chip-filtered in-place
        homeSpecialistRecycler = findViewById(R.id.homeSpecialistRecycler);
        textNoSpecialists = findViewById(R.id.textNoSpecialists);
        homeSpecialistAdapter = new CounselorAdapter(this, new java.util.ArrayList<>());
        homeSpecialistRecycler.setLayoutManager(
                new androidx.recyclerview.widget.LinearLayoutManager(this));
        homeSpecialistRecycler.setAdapter(homeSpecialistAdapter);

        // Show cached counselors instantly (no blank screen), then always
        // refresh from Firestore in the background during onCreate so that a
        // counselor added since the last fetch always appears.
        // onResume does NOT re-fetch — this is the only refresh per Activity creation.
        java.util.List<Counselor> cachedCounselors = SessionCache.getInstance().getCounselors();
        if (cachedCounselors != null) {
            allCounselors.clear();
            allCounselors.addAll(cachedCounselors);
            refreshSpecialistList();
            warmCounselorCache(cachedCounselors);
        }
        new CounselorRepository().getAllCounselors(new CounselorRepository.OnCounselorsLoadedCallback() {
            @Override public void onSuccess(java.util.List<Counselor> list) {
                // Merge: overlay fresh Firestore data onto whatever is currently in
                // allCounselors (which may include counselors added by populateSessionCard
                // or from the cache). This prevents a partial Firestore result from
                // wiping counselors that are already correctly displayed.
                java.util.Map<String, Counselor> merged = new java.util.LinkedHashMap<>();
                for (Counselor c : allCounselors) {
                    String key = c.getId() != null ? c.getId() : c.getUid();
                    if (key != null) merged.put(key, c);
                }
                for (Counselor c : list) {
                    String key = c.getId() != null ? c.getId() : c.getUid();
                    if (key != null) merged.put(key, c);
                }
                java.util.List<Counselor> mergedList = new java.util.ArrayList<>(merged.values());
                if (!mergedList.isEmpty()) {
                    SessionCache.getInstance().putCounselors(mergedList);
                }
                allCounselors.clear();
                allCounselors.addAll(mergedList);
                refreshSpecialistList();
                if (!mergedList.isEmpty()) {
                    warmCounselorCache(mergedList);
                    warmSlotCache(mergedList);
                }
            }
            @Override public void onFailure(Exception e) {
                // Firestore failed — still refresh the display with whatever is
                // already in allCounselors (populated from cache or populateSessionCard).
                refreshSpecialistList();
            }
        });

        // Build specialization chips dynamically from SpecializationTags
        android.widget.LinearLayout homeChipContainer = findViewById(R.id.homeChipContainer);
        buildHomeChips(homeChipContainer);

        // Bottom navigation
        findViewById(R.id.navCalendar).setOnClickListener(v ->
                startActivity(new Intent(this, CalendarActivity.class)));
        findViewById(R.id.navHistory).setOnClickListener(v ->
                startActivity(new Intent(this, HistoryActivity.class)));

        View navLogout = findViewById(R.id.navLogout);
        navLogout.setOnClickListener(v -> showLogoutDialog());

        // Eye button → discreet mode
        ImageButton discreetBtn = findViewById(R.id.discreetModeBtn);
        discreetBtn.setOnClickListener(v -> activateDiscreetMode());

        // Intercept back press to show exit confirmation dialog
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showExitDialog();
            }
        });

        cardWaitlistStatus.setOnClickListener(v ->
                startActivity(new Intent(this, StudentWaitlistActivity.class)));

        // Load upcoming session and feedback prompt on first open
        fetchUpcomingSession();
        checkForPendingFeedback();
        loadIntakeAndWaitlistSummary(uid);
    }

    // -------------------------------------------------------------------------
    // Upcoming session
    // -------------------------------------------------------------------------

    /**
     * Fetches all CONFIRMED appointments for this student, picks the earliest
     * one that is today or in the future, and populates the session card.
     * Called on every onResume so the card stays fresh after bookings/cancellations.
     */
    private void fetchUpcomingSession() {
        String studentId = mAuth.getCurrentUser().getUid();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // Session-scoped cache: show instantly if available.
        // Only fetch from Firestore when cache is null (first open after login,
        // or after explicit invalidation from booking / cancellation).
        List<Appointment> cached = SessionCache.getInstance().getStudentAppointments(studentId);
        if (cached != null) {
            resolveUpcoming(cached, today);
            return;
        }

        appointmentRepository.getAppointmentsForStudent(studentId,
                new AppointmentRepository.OnAppointmentsLoadedCallback() {
                    @Override
                    public void onSuccess(List<Appointment> appointments) {
                        SessionCache.getInstance().putStudentAppointments(studentId, appointments);
                        resolveUpcoming(appointments, today);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        clearSessionCard();
                    }
                });
    }

    /** Finds the earliest CONFIRMED appointment that hasn't passed yet and shows it. */
    private void resolveUpcoming(List<Appointment> appointments, String today) {
        String nowTime = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.US)
                .format(new java.util.Date());
        Appointment next = null;
        for (Appointment a : appointments) {
            if (!"CONFIRMED".equals(a.getStatus())) continue;
            if (a.getDate() == null) continue;
            int dateCompare = a.getDate().compareTo(today);
            if (dateCompare < 0) continue;
            if (dateCompare == 0 && a.getTime() != null && a.getTime().compareTo(nowTime) < 0) {
                continue;
            }
            if (next == null || a.getDate().compareTo(next.getDate()) < 0
                    || (a.getDate().equals(next.getDate())
                        && a.getTime() != null && next.getTime() != null
                        && a.getTime().compareTo(next.getTime()) < 0)) {
                next = a;
            }
        }
        upcomingAppointment = next;
        if (next != null) {
            populateSessionCard(next);
        } else {
            clearSessionCard();
        }
    }

    /**
     * Populates the session card with the appointment details and looks up
     * the counselor name for display.
     */
    private void populateSessionCard(Appointment appointment) {
        String prettyDate = formatAppointmentDate(appointment.getDate());
        upcomingSessionDate.setText(prettyDate);
        upcomingSessionTime.setText(appointment.getTime());
        sessionTimeRow.setVisibility(View.VISIBLE);
        sliderContainer.setVisibility(View.VISIBLE);

        // Counselor name — check cache, then the already-loaded list, then Firestore.
        // cId is the Auth UID stored in the appointment. For old manually-created counselors
        // the Auth UID differs from the Firestore doc ID, so getCounselor(cId) would fail.
        // Searching allCounselors by uid covers that case without an extra Firestore read.
        String cId = appointment.getCounselorId();
        Counselor cachedCounselor = SessionCache.getInstance().getSingleCounselor(cId);
        if (cachedCounselor != null) {
            String name = cachedCounselor.getName() != null ? cachedCounselor.getName() : "Your Counselor";
            counselorNameText.setText(name);
            counselorRoleText.setText(appointment.getTime() + " · " + prettyDate);
        } else {
            // Search the in-memory list first (uid or doc ID match)
            Counselor fromList = null;
            for (Counselor c : allCounselors) {
                if (cId.equals(c.getUid()) || cId.equals(c.getId())) {
                    fromList = c;
                    break;
                }
            }
            if (fromList != null) {
                SessionCache.getInstance().putSingleCounselor(cId, fromList);
                String name = fromList.getName() != null ? fromList.getName() : "Your Counselor";
                counselorNameText.setText(name);
                counselorRoleText.setText(appointment.getTime() + " · " + prettyDate);
            } else {
                counselorNameText.setText("Loading…");
                counselorRoleText.setText("Session on " + prettyDate);

                new CounselorRepository().getCounselor(cId,
                        new CounselorRepository.OnCounselorFetchedCallback() {
                            @Override
                            public void onSuccess(Counselor counselor) {
                                if (upcomingAppointment == null) return;
                                SessionCache.getInstance().putSingleCounselor(cId, counselor);
                                String name = counselor.getName() != null ? counselor.getName() : "Your Counselor";
                                counselorNameText.setText(name);
                                counselorRoleText.setText(appointment.getTime() + " · " + prettyDate);
                                // If this counselor wasn't in the list (the common case when
                                // getAllCounselors() dropped their document), add them now so
                                // they appear in the specialist RecyclerView.
                                boolean alreadyInList = false;
                                for (Counselor existing : allCounselors) {
                                    if (cId.equals(existing.getUid()) || cId.equals(existing.getId())) {
                                        alreadyInList = true;
                                        break;
                                    }
                                }
                                if (!alreadyInList) {
                                    // Add silently — getAllCounselors() callback owns the display
                                    // update. Calling applyHomeChipFilter() here would show only
                                    // this one counselor before the full list has loaded.
                                    allCounselors.add(counselor);
                                }
                            }
                            @Override
                            public void onFailure(Exception e) {
                                if (upcomingAppointment == null) return;
                                counselorNameText.setText("Your Counselor");
                            }
                        });
            }
        }
    }

    /** Resets the session card to the empty / no-session state. */
    private void clearSessionCard() {
        upcomingAppointment = null;
        counselorNameText.setText("No upcoming session");
        counselorRoleText.setText("Book an appointment below");
        sessionTimeRow.setVisibility(View.GONE);
        sliderContainer.setVisibility(View.GONE);
    }

    private String formatAppointmentDate(String raw) {
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(raw);
            return new SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(d);
        } catch (Exception e) {
            return raw;
        }
    }

    // -------------------------------------------------------------------------
    // Slide-to-cancel
    // -------------------------------------------------------------------------

    /**
     * Confirms cancellation with the user, then calls
     * {@link AppointmentRepository#cancelAppointment} to mark the appointment
     * CANCELLED in Firestore and restore the slot's availability.
     */
    private void handleCancellation() {
        float thumbPx = 42 * getResources().getDisplayMetrics().density;

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_cancel_appointment);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.85),
                    WindowManager.LayoutParams.WRAP_CONTENT);
            window.setGravity(android.view.Gravity.CENTER);
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
            window.setDimAmount(0.55f);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }

        MaterialButton btnConfirm = dialog.findViewById(R.id.btnConfirmCancel);
        MaterialButton btnKeep    = dialog.findViewById(R.id.btnKeepAppointment);

        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            resetSlider(thumbPx);
            if (upcomingAppointment == null) {
                clearSessionCard();
                return;
            }
            appointmentRepository.cancelAppointment(
                    upcomingAppointment.getId(),
                    upcomingAppointment.getCounselorId(),
                    upcomingAppointment.getSlotId(),
                    new AppointmentRepository.OnStatusUpdateCallback() {
                        @Override
                        public void onSuccess() {
                            SessionCache.getInstance().invalidateAppointments();
                            AppToast.show(StudentHomeActivity.this,
                                    "Appointment cancelled.", AppToast.LENGTH_SHORT);
                            fetchUpcomingSession();
                        }
                        @Override
                        public void onFailure(Exception e) {
                            AppToast.show(StudentHomeActivity.this,
                                    "Could not cancel. Please try again.",
                                    AppToast.LENGTH_SHORT);
                        }
                    });
        });
        btnKeep.setOnClickListener(v -> {
            dialog.dismiss();
            resetSlider(thumbPx);
        });

        // Reset slider no matter how the dialog is dismissed (including outside-tap)
        dialog.setOnDismissListener(d -> resetSlider(thumbPx));

        dialog.show();
    }

    private void resetSlider(float thumbPx) {
        slideToCancelSlider.setProgress(0);
        android.view.ViewGroup.LayoutParams lp = slideFill.getLayoutParams();
        lp.width = Math.round(thumbPx);
        slideFill.setLayoutParams(lp);
    }

    // -------------------------------------------------------------------------
    // Feedback
    // -------------------------------------------------------------------------

    private void checkForPendingFeedback() {
        String studentId = mAuth.getCurrentUser().getUid();
        appointmentRepository.getCompletedAppointmentsNeedingFeedback(studentId,
                new AppointmentRepository.OnAppointmentsLoadedCallback() {
                    @Override
                    public void onSuccess(List<Appointment> completed) {
                        if (completed.isEmpty()) return;
                        Appointment latest = completed.get(completed.size() - 1);
                        feedbackRepository.hasFeedbackForAppointment(latest.getId(),
                                feedbackExists -> {
                                    if (!feedbackExists) {
                                        pendingFeedbackAppointment = latest;
                                        showFeedbackPromptCard(latest);
                                    }
                                });
                    }
                    @Override
                    public void onFailure(Exception e) { /* silent */ }
                });
    }

    private void showFeedbackPromptCard(Appointment appointment) {
        cardFeedbackPrompt.setVisibility(View.VISIBLE);

        new CounselorRepository().getCounselor(appointment.getCounselorId(),
                new CounselorRepository.OnCounselorFetchedCallback() {
                    @Override
                    public void onSuccess(Counselor counselor) {
                        textFeedbackPromptSubtitle.setText(
                                getString(R.string.feedback_prompt_subtitle, counselor.getName()));
                    }
                    @Override
                    public void onFailure(Exception e) {
                        textFeedbackPromptSubtitle.setText(
                                getString(R.string.feedback_prompt_subtitle_generic));
                    }
                });

        buttonGiveFeedback.setOnClickListener(v -> showFeedbackDialog(appointment.getId()));
        buttonDismissFeedback.setOnClickListener(v -> {
            feedbackDismissed = true;
            cardFeedbackPrompt.setVisibility(View.GONE);
        });
    }

    private void showFeedbackDialog(String appointmentId) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_feedback, null);
        AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        RatingBar ratingBar = dialogView.findViewById(R.id.feedbackRating);
        EditText commentEdit = dialogView.findViewById(R.id.feedbackComment);
        Button submitBtn = dialogView.findViewById(R.id.btnSubmitFeedback);
        submitBtn.setOnClickListener(v -> {
            int rating = (int) ratingBar.getRating();
            if (rating == 0) {
                AppToast.show(this, getString(R.string.error_rating_required),
                        AppToast.LENGTH_SHORT);
                return;
            }
            String comment = commentEdit.getText() != null
                    ? commentEdit.getText().toString().trim() : "";
            FeedbackService feedback = new FeedbackService(appointmentId, rating, comment);
            feedbackRepository.submitFeedback(feedback,
                    new FeedbackRepository.OnFeedbackSubmittedCallback() {
                        @Override
                        public void onSuccess() {
                            AppToast.show(StudentHomeActivity.this,
                                    getString(R.string.feedback_submitted),
                                    AppToast.LENGTH_SHORT);
                            dialog.dismiss();
                            cardFeedbackPrompt.setVisibility(View.GONE);
                            pendingFeedbackAppointment = null;
                        }
                        @Override
                        public void onFailure(Exception e) {
                            AppToast.show(StudentHomeActivity.this,
                                    getString(R.string.error_feedback_submission),
                                    AppToast.LENGTH_LONG);
                        }
                    });
        });
        dialog.show();
    }

    // -------------------------------------------------------------------------
    // Privacy / discreet mode
    // -------------------------------------------------------------------------

    /**
     * Pre-populates the single-counselor cache entries from the full counselor list.
     * This way, populateSessionCard() and CounselorProfileActivity can read
     * counselor details instantly without a separate Firestore round-trip.
     */
    private void warmCounselorCache(java.util.List<Counselor> counselors) {
        SessionCache cache = SessionCache.getInstance();
        for (Counselor c : counselors) {
            if (c.getId() != null) {
                cache.putSingleCounselor(c.getId(), c);
            }
            if (c.getUid() != null && !c.getUid().equals(c.getId())) {
                cache.putSingleCounselor(c.getUid(), c);
            }
        }
    }

    /**
     * Pre-fetches available slots for every counselor in the background immediately
     * after the counselor list is loaded. Slots are stored under both the Auth UID
     * and the Firestore doc ID so that BookingActivity cache lookups always hit
     * regardless of which key was used when the slots were originally created.
     *
     * <p>Already-cached counselors are skipped. Slot cache is invalidated by
     * {@link BookingActivity} after a successful booking.</p>
     */
    private void warmSlotCache(java.util.List<Counselor> counselors) {
        AvailabilityRepository repo = new AvailabilityRepository();
        for (Counselor c : counselors) {
            String uid   = c.getUid();
            String docId = c.getId();
            // Primary key: Auth UID (standard slot path). Fall back to doc ID if uid absent.
            String primaryKey  = (uid != null && !uid.isEmpty()) ? uid : docId;
            String fallbackKey = (uid != null && !uid.isEmpty()
                    && docId != null && !docId.equals(uid)) ? docId : null;

            if (primaryKey == null) continue;

            // Skip counselors whose slots are already cached under the primary key
            if (SessionCache.getInstance().getSlots(primaryKey) != null) continue;

            // Effectively-final copies for lambdas
            String pKey = primaryKey;
            String fKey = fallbackKey;

            repo.getAvailableSlotsForCounselor(pKey,
                    new AvailabilityRepository.OnSlotsLoadedCallback() {
                        @Override
                        public void onSuccess(java.util.List<TimeSlot> slots) {
                            if (!slots.isEmpty()) {
                                // Slots found on the primary path — cache under both keys
                                SessionCache.getInstance().putSlots(pKey, slots);
                                if (fKey != null) SessionCache.getInstance().putSlots(fKey, slots);
                            } else if (fKey != null) {
                                // Primary path empty — try the doc-ID fallback path
                                repo.getAvailableSlotsForCounselor(fKey,
                                        new AvailabilityRepository.OnSlotsLoadedCallback() {
                                            @Override
                                            public void onSuccess(java.util.List<TimeSlot> fbSlots) {
                                                // Cache under both keys (even if fbSlots is empty)
                                                // so we don't repeat the double-fetch next time.
                                                SessionCache.getInstance().putSlots(pKey, fbSlots);
                                                SessionCache.getInstance().putSlots(fKey, fbSlots);
                                            }
                                            @Override
                                            public void onFailure(Exception e) { /* silent */ }
                                        });
                            } else {
                                // No fallback and primary returned empty — cache the empty result
                                // so we don't re-fetch on every home-screen load.
                                SessionCache.getInstance().putSlots(pKey, slots);
                            }
                        }
                        @Override
                        public void onFailure(Exception e) {
                            // Silent — BookingActivity will fall back to a live fetch
                            android.util.Log.w("SlotWarm",
                                    "slot pre-fetch failed for " + pKey + ": " + e.getMessage());
                        }
                    });
        }
    }

    private void applyStudentName(Student student) {
        String display = (student.getPreferredName() != null && !student.getPreferredName().isEmpty())
                ? student.getPreferredName() : student.getName();
        welcomeNameText.setText(display);
    }

    private void redirectCounselorSessionIfNeeded() {
        userRepository.getCurrentUserRole(new UserRepository.OnRoleFetchedCallback() {
            @Override
            public void onSuccess(String role) {
                if (UserRole.COUNSELOR.equals(role)) {
                    startActivity(new Intent(StudentHomeActivity.this,
                            CounselorDashboardActivity.class));
                    finish();
                }
            }

            @Override
            public void onFailure(Exception e) {
                // Keep the current screen; login routing handles hard failures.
            }
        });
    }

    private void activateDiscreetMode() {
        isOverlayActive = true;
        // Mirror the real header name into the overlay
        TextView overlayName = privacyOverlay.findViewById(R.id.overlayUserName);
        if (overlayName != null) {
            CharSequence name = welcomeNameText.getText();
            overlayName.setText((name != null && name.length() > 0) ? name : "Student");
        }
        privacyOverlay.setVisibility(View.VISIBLE);
    }

    // -------------------------------------------------------------------------
    // Home specialist chip filtering
    // -------------------------------------------------------------------------

    private void buildHomeChips(android.widget.LinearLayout container) {
        int dp8 = Math.round(8 * getResources().getDisplayMetrics().density);
        int dp14 = Math.round(14 * getResources().getDisplayMetrics().density);
        int dp32 = Math.round(32 * getResources().getDisplayMetrics().density);

        // "All" chip first
        android.widget.LinearLayout.LayoutParams lp =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, dp32);
        lp.setMarginEnd(dp8);

        android.widget.TextView allChip = makeHomeChip("All", true, dp14);
        allChip.setLayoutParams(lp);
        activeChip = allChip;
        allChip.setOnClickListener(v -> { setActiveHomeChip(container, allChip); applyHomeChipFilter(null); });
        container.addView(allChip);

        for (String tag : SpecializationTags.ALL_TAGS) {
            android.widget.LinearLayout.LayoutParams lp2 =
                    new android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, dp32);
            lp2.setMarginEnd(dp8);
            android.widget.TextView chip = makeHomeChip(tag, false, dp14);
            chip.setLayoutParams(lp2);
            chip.setOnClickListener(v -> { setActiveHomeChip(container, chip); applyHomeChipFilter(tag); });
            container.addView(chip);
        }
    }

    private android.widget.TextView makeHomeChip(String label, boolean active, int hPad) {
        android.widget.TextView tv = new android.widget.TextView(this);
        tv.setText(label);
        tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12);
        tv.setGravity(android.view.Gravity.CENTER);
        tv.setPadding(hPad, 0, hPad, 0);
        tv.setBackgroundResource(active ? R.drawable.chip_active : R.drawable.chip_inactive);
        tv.setTextColor(active ? 0xFFFFFFFF : 0xFF8B6BAE);
        return tv;
    }

    private void setActiveHomeChip(android.widget.LinearLayout container,
                                   android.widget.TextView selected) {
        for (int i = 0; i < container.getChildCount(); i++) {
            android.widget.TextView chip = (android.widget.TextView) container.getChildAt(i);
            boolean isActive = chip == selected;
            chip.setBackgroundResource(isActive ? R.drawable.chip_active : R.drawable.chip_inactive);
            chip.setTextColor(isActive ? 0xFFFFFFFF : 0xFF8B6BAE);
        }
        activeChip = selected;
    }

    /** Refreshes the specialist RecyclerView using the currently active chip. */
    private void refreshSpecialistList() {
        String activeFilter = (activeChip != null
                && !activeChip.getText().toString().equals("All"))
                ? activeChip.getText().toString() : null;
        applyHomeChipFilter(activeFilter);
    }

    private void applyHomeChipFilter(String specialization) {
        java.util.List<Counselor> filtered = new java.util.ArrayList<>();
        for (Counselor c : allCounselors) {
            if (specialization == null) {
                filtered.add(c);
            } else {
                java.util.List<String> tags = c.getSpecializations();
                if (tags != null) {
                    for (String t : tags) {
                        if (t.equalsIgnoreCase(specialization)) { filtered.add(c); break; }
                    }
                }
            }
        }
        homeSpecialistAdapter.setData(filtered);
        textNoSpecialists.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // -------------------------------------------------------------------------
    // Exit confirmation
    // -------------------------------------------------------------------------

    private void showLogoutDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_exit);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.85),
                    WindowManager.LayoutParams.WRAP_CONTENT);
            window.setGravity(android.view.Gravity.CENTER);
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
            window.setDimAmount(0.55f);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }

        MaterialButton btnLeave = dialog.findViewById(R.id.btnExitConfirm);
        MaterialButton btnStay  = dialog.findViewById(R.id.btnExitCancel);

        btnLeave.setText("Yes, log out");
        btnStay.setText("Stay");

        btnLeave.setOnClickListener(v -> {
            dialog.dismiss();
            SessionCache.getInstance().clearAll();
            mAuth.signOut();
            Intent intent = new Intent(StudentHomeActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finishAffinity();
        });
        btnStay.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showExitDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_exit);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.85),
                    WindowManager.LayoutParams.WRAP_CONTENT);
            window.setGravity(android.view.Gravity.CENTER);
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
            window.setDimAmount(0.55f);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }

        MaterialButton btnLeave = dialog.findViewById(R.id.btnExitConfirm);
        MaterialButton btnStay  = dialog.findViewById(R.id.btnExitCancel);

        btnLeave.setOnClickListener(v -> {
            dialog.dismiss();
            finishAffinity();
        });
        btnStay.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private boolean firstResume = true;

    @Override
    protected void onResume() {
        super.onResume();
        AppToast.consumePending(this);
        // Skip the very first resume — onCreate already loaded the data.
        // On subsequent resumes (returning from BookingActivity etc.) refresh.
        if (firstResume) {
            firstResume = false;
        } else if (mAuth.getCurrentUser() != null) {
            fetchUpcomingSession();
            if (!feedbackDismissed) checkForPendingFeedback();
            loadIntakeAndWaitlistSummary(mAuth.getCurrentUser().getUid());
        }
    }

    private void loadIntakeAndWaitlistSummary(String studentId) {
        intakeAssessmentRepository.getLatestForStudent(studentId,
                new IntakeAssessmentRepository.OnAssessmentLoadedCallback() {
                    @Override
                    public void onSuccess(IntakeAssessment assessment) {
                        showLatestMatch(assessment);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        textLatestMatchSubtitle.setText(R.string.latest_match_empty);
                        cardLatestMatch.setOnClickListener(v ->
                                startActivity(new Intent(StudentHomeActivity.this, QuizActivity.class)));
                    }
                });

        waitlistRepository.getActiveWaitlistForStudent(studentId,
                new WaitlistRepository.OnWaitlistLoadedCallback() {
                    @Override
                    public void onSuccess(List<WaitlistEntry> entries) {
                        if (entries.isEmpty()) {
                            textWaitlistStatusSubtitle.setText(R.string.waitlist_status_empty);
                        } else {
                            textWaitlistStatusSubtitle.setText(
                                    getString(R.string.waitlist_status_subtitle, entries.size()));
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        textWaitlistStatusSubtitle.setText(R.string.waitlist_status_empty);
                    }
                });
    }

    private void showLatestMatch(IntakeAssessment assessment) {
        String counselorName = assessment.getMatchedCounselorName();
        if (counselorName == null || counselorName.isEmpty()) {
            counselorName = getString(R.string.no_counselors_available);
        }
        textLatestMatchSubtitle.setText(getString(R.string.latest_match_subtitle, counselorName));
        String matchedCounselorId = assessment.getMatchedCounselorId();
        cardLatestMatch.setOnClickListener(v -> {
            if (matchedCounselorId == null || matchedCounselorId.isEmpty()) {
                startActivity(new Intent(StudentHomeActivity.this, CounselorListActivity.class));
                return;
            }
            openMatchedCounselor(matchedCounselorId, assessment.getId());
        });
    }

    private void openMatchedCounselor(String matchedCounselorId, String assessmentId) {
        Counselor cached = SessionCache.getInstance().getSingleCounselor(matchedCounselorId);
        if (cached != null) {
            launchCounselorProfile(cached, assessmentId);
            return;
        }
        new CounselorRepository().getCounselor(matchedCounselorId,
                new CounselorRepository.OnCounselorFetchedCallback() {
                    @Override
                    public void onSuccess(Counselor counselor) {
                        SessionCache.getInstance().putSingleCounselor(matchedCounselorId, counselor);
                        launchCounselorProfile(counselor, assessmentId);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        startActivity(new Intent(StudentHomeActivity.this, CounselorListActivity.class));
                    }
                });
    }

    private void launchCounselorProfile(Counselor counselor, String assessmentId) {
        Intent intent = new Intent(StudentHomeActivity.this, CounselorProfileActivity.class);
        intent.putExtra("COUNSELOR_ID", counselor.getId());
        String slotId = counselor.getUid() != null ? counselor.getUid() : counselor.getId();
        intent.putExtra("SLOT_COUNSELOR_ID", slotId);
        intent.putExtra("COUNSELOR_NAME", counselor.getName());
        intent.putExtra(QuizActivity.EXTRA_ASSESSMENT_ID, assessmentId);
        startActivity(intent);
    }
}
