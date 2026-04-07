package com.example.moogerscouncil;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RatingBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
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
    private android.os.Handler privacyHandler = new android.os.Handler();
    private Runnable privacyRunnable;
    private TextView counselorNameText, counselorRoleText, welcomeNameText;
    private TextView upcomingSessionDate, upcomingSessionTime;
    private String originalName, originalRole;
    private boolean isMasked = false;
    private boolean isOverlayActive = false;
    private boolean hasSession = false;
    private View privacyOverlay;
    private UserRepository userRepository;

    // Upcoming session
    private Appointment upcomingAppointment;
    private View sessionTimeRow;
    private View sliderContainer;
    private SeekBar slideToCancelSlider;
    private View slideFill;

    // Feedback
    private FeedbackRepository feedbackRepository;
    private AppointmentRepository appointmentRepository;
    private CardView cardFeedbackPrompt;
    private TextView textFeedbackPromptSubtitle;
    private MaterialButton buttonGiveFeedback;
    private MaterialButton buttonDismissFeedback;
    private Appointment pendingFeedbackAppointment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_home);

        mAuth = FirebaseAuth.getInstance();
        userRepository = new UserRepository();
        feedbackRepository = new FeedbackRepository();
        appointmentRepository = new AppointmentRepository();

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

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Fetch display name
        userRepository.getCurrentUser(new UserRepository.OnUserFetchedCallback() {
            @Override
            public void onSuccess(Student student) {
                String display = (student.getPreferredName() != null && !student.getPreferredName().isEmpty())
                        ? student.getPreferredName() : student.getName();
                welcomeNameText.setText(display);
            }
            @Override
            public void onFailure(Exception e) { welcomeNameText.setText("Student"); }
        });

        setupPrivacyTimer();

        // Eye-slash button inside the fake app overlay
        ImageButton overlayExitBtn = privacyOverlay.findViewById(R.id.overlayExitBtn);
        if (overlayExitBtn == null) throw new IllegalStateException("overlayExitBtn missing");
        overlayExitBtn.setOnClickListener(v -> {
            isOverlayActive = false;
            privacyOverlay.setVisibility(View.GONE);
            unmaskPII();
            resetPrivacyTimer();
        });

        // Crisis banner
        CardView crisisBanner = findViewById(R.id.crisisBanner);
        crisisBanner.setOnClickListener(v -> {
            resetPrivacyTimer();
            EmergencyDialogFragment.newInstance().show(getSupportFragmentManager(), "emergency");
        });

        // Find My Match
        CardView findMatchCard = findViewById(R.id.findMatchCard);
        findMatchCard.setOnClickListener(v -> {
            resetPrivacyTimer();
            startActivity(new Intent(this, QuizActivity.class));
        });

        // AI Chat placeholder
        CardView aiChatCard = findViewById(R.id.aiChatCard);
        aiChatCard.setOnClickListener(v -> {
            resetPrivacyTimer();
            Toast.makeText(this, "AI Chat coming soon!", Toast.LENGTH_SHORT).show();
        });

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
            @Override public void onStartTrackingTouch(SeekBar seekBar) { resetPrivacyTimer(); }
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
            resetPrivacyTimer();
            if (pendingFeedbackAppointment != null) {
                showFeedbackDialog(pendingFeedbackAppointment.getId());
            } else {
                Toast.makeText(this, getString(R.string.no_pending_feedback),
                        Toast.LENGTH_SHORT).show();
            }
        });

        // Find a Specialist section
        View.OnClickListener openDirectory = v ->
                startActivity(new Intent(this, CounselorListActivity.class));
        findViewById(R.id.searchBarHome).setOnClickListener(openDirectory);
        findViewById(R.id.counselorCard1).setOnClickListener(openDirectory);
        findViewById(R.id.counselorCard2).setOnClickListener(openDirectory);

        // Bottom navigation
        findViewById(R.id.navCalendar).setOnClickListener(v ->
                startActivity(new Intent(this, CalendarActivity.class)));
        findViewById(R.id.navHistory).setOnClickListener(v ->
                startActivity(new Intent(this, HistoryActivity.class)));

        View navLogout = findViewById(R.id.navLogout);
        navLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        // Eye button → discreet mode
        ImageButton discreetBtn = findViewById(R.id.discreetModeBtn);
        discreetBtn.setOnClickListener(v -> activateDiscreetMode());

        // Load upcoming session and feedback prompt on first open
        fetchUpcomingSession();
        checkForPendingFeedback();
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

        appointmentRepository.getAppointmentsForStudent(studentId,
                new AppointmentRepository.OnAppointmentsLoadedCallback() {
                    @Override
                    public void onSuccess(List<Appointment> appointments) {
                        // Find the earliest CONFIRMED appointment on or after today
                        Appointment next = null;
                        for (Appointment a : appointments) {
                            if (!"CONFIRMED".equals(a.getStatus())) continue;
                            if (a.getDate() == null) continue;
                            if (a.getDate().compareTo(today) >= 0) {
                                if (next == null || a.getDate().compareTo(next.getDate()) < 0) {
                                    next = a;
                                }
                            }
                        }
                        upcomingAppointment = next;
                        if (next != null) {
                            populateSessionCard(next);
                        } else {
                            clearSessionCard();
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(StudentHomeActivity.this,
                                "Session load failed: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                        clearSessionCard();
                    }
                });
    }

    /**
     * Populates the session card with the appointment details and looks up
     * the counselor name for display.
     */
    private void populateSessionCard(Appointment appointment) {
        upcomingSessionDate.setText("📅  " + appointment.getDate());
        upcomingSessionTime.setText("🕑  " + appointment.getTime());
        sessionTimeRow.setVisibility(View.VISIBLE);
        sliderContainer.setVisibility(View.VISIBLE);

        // Placeholder while counselor name loads
        counselorNameText.setText("Loading…");
        counselorRoleText.setText("Session on " + appointment.getDate());
        hasSession = true;

        new CounselorRepository().getCounselor(appointment.getCounselorId(),
                new CounselorRepository.OnCounselorFetchedCallback() {
                    @Override
                    public void onSuccess(Counselor counselor) {
                        String name = counselor.getName() != null ? counselor.getName() : "Your Counselor";
                        counselorNameText.setText(name);
                        counselorRoleText.setText(appointment.getTime() + " · " + appointment.getDate());
                        originalName = name;
                        originalRole = counselorRoleText.getText().toString();
                    }
                    @Override
                    public void onFailure(Exception e) {
                        counselorNameText.setText("Your Counselor");
                    }
                });
    }

    /** Resets the session card to the empty / no-session state. */
    private void clearSessionCard() {
        upcomingAppointment = null;
        hasSession = false;
        counselorNameText.setText("No upcoming session");
        counselorRoleText.setText("Book an appointment below");
        sessionTimeRow.setVisibility(View.GONE);
        sliderContainer.setVisibility(View.GONE);
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
        if (upcomingAppointment == null) {
            resetSlider(42 * getResources().getDisplayMetrics().density);
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Cancel Appointment?")
                .setMessage("Are you sure you want to release this slot? This cannot be undone.")
                .setPositiveButton("Yes, Cancel", (dialog, which) -> {
                    String apptId = upcomingAppointment.getId();
                    String slotId = upcomingAppointment.getSlotId();

                    appointmentRepository.cancelAppointment(apptId, slotId,
                            new AppointmentRepository.OnStatusUpdateCallback() {
                                @Override
                                public void onSuccess() {
                                    Toast.makeText(StudentHomeActivity.this,
                                            "Appointment cancelled.", Toast.LENGTH_SHORT).show();
                                    clearSessionCard();
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    Toast.makeText(StudentHomeActivity.this,
                                            "Could not cancel. Please try again.",
                                            Toast.LENGTH_SHORT).show();
                                    resetSlider(42 * getResources().getDisplayMetrics().density);
                                }
                            });
                })
                .setNegativeButton("No", (dialog, which) ->
                        resetSlider(42 * getResources().getDisplayMetrics().density))
                .show();
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
            cardFeedbackPrompt.setVisibility(View.GONE);
            pendingFeedbackAppointment = null;
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
                Toast.makeText(this, getString(R.string.error_rating_required),
                        Toast.LENGTH_SHORT).show();
                return;
            }
            String comment = commentEdit.getText() != null
                    ? commentEdit.getText().toString().trim() : "";
            FeedbackService feedback = new FeedbackService(appointmentId, rating, comment);
            feedbackRepository.submitFeedback(feedback,
                    new FeedbackRepository.OnFeedbackSubmittedCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(StudentHomeActivity.this,
                                    getString(R.string.feedback_submitted),
                                    Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            cardFeedbackPrompt.setVisibility(View.GONE);
                            pendingFeedbackAppointment = null;
                        }
                        @Override
                        public void onFailure(Exception e) {
                            Toast.makeText(StudentHomeActivity.this,
                                    getString(R.string.error_feedback_submission),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        });
        dialog.show();
    }

    // -------------------------------------------------------------------------
    // Privacy / discreet mode
    // -------------------------------------------------------------------------

    private void setupPrivacyTimer() {
        privacyRunnable = this::maskPII;
        resetPrivacyTimer();
    }

    private void activateDiscreetMode() {
        isOverlayActive = true;
        privacyOverlay.setVisibility(View.VISIBLE);
        privacyHandler.removeCallbacks(privacyRunnable);
    }

    private void resetPrivacyTimer() {
        privacyHandler.removeCallbacks(privacyRunnable);
        privacyHandler.postDelayed(privacyRunnable, 5000);
    }

    private void maskPII() {
        if (isMasked || !hasSession) return;
        originalName = counselorNameText.getText().toString();
        originalRole = counselorRoleText.getText().toString();
        counselorNameText.setText("••••••••••••");
        counselorRoleText.setText("••••••••••••");
        isMasked = true;
    }

    private void unmaskPII() {
        if (!isMasked) return;
        counselorNameText.setText(originalName);
        counselorRoleText.setText(originalRole);
        isMasked = false;
    }

    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent ev) {
        if (!isOverlayActive) resetPrivacyTimer();
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onPause() {
        super.onPause();
        privacyHandler.removeCallbacks(privacyRunnable);
    }

    private boolean firstResume = true;

    @Override
    protected void onResume() {
        super.onResume();
        resetPrivacyTimer();
        // Skip the very first resume — onCreate already loaded the data.
        // On subsequent resumes (returning from BookingActivity etc.) refresh.
        if (firstResume) {
            firstResume = false;
        } else if (mAuth.getCurrentUser() != null) {
            fetchUpcomingSession();
            checkForPendingFeedback();
        }
    }
}
