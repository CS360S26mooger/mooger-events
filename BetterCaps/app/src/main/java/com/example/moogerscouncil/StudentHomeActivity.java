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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Student home dashboard shown after successful login or registration.
 * Displays upcoming sessions, crisis support, and navigation.
 * Pulled from MoogersCouncil, adapted to use BetterCaps UserRepository.
 */
public class StudentHomeActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private android.os.Handler privacyHandler = new android.os.Handler();
    private Runnable privacyRunnable;
    private TextView counselorNameText, counselorRoleText, welcomeNameText;
    private String originalName, originalRole;
    private boolean isMasked = false;
    private boolean isOverlayActive = false;
    private View privacyOverlay;
    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_home);

        mAuth = FirebaseAuth.getInstance();
        userRepository = new UserRepository();

        counselorNameText = findViewById(R.id.upcomingCounselorName);
        counselorRoleText = findViewById(R.id.upcomingCounselorRole);
        welcomeNameText = findViewById(R.id.welcomeName);
        privacyOverlay = findViewById(R.id.privacyOverlay);

        // Redirect to login if not authenticated
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Fetch display name via UserRepository; prefer preferredName if set
        userRepository.getCurrentUser(new UserRepository.OnUserFetchedCallback() {
            @Override
            public void onSuccess(Student student) {
                String display = (student.getPreferredName() != null && !student.getPreferredName().isEmpty())
                        ? student.getPreferredName()
                        : student.getName();
                welcomeNameText.setText(display);
            }
            @Override
            public void onFailure(Exception e) {
                welcomeNameText.setText("Student");
            }
        });

        originalName = counselorNameText.getText().toString();
        originalRole = counselorRoleText.getText().toString();

        setupPrivacyTimer();

        // Eye-slash button inside the fake app overlay — only way to return to real app
        ImageButton overlayExitBtn = privacyOverlay.findViewById(R.id.overlayExitBtn);
        if (overlayExitBtn == null) throw new IllegalStateException("overlayExitBtn missing from privacy overlay layout");
        overlayExitBtn.setOnClickListener(v -> {
            isOverlayActive = false;
            privacyOverlay.setVisibility(View.GONE);
            unmaskPII();
            resetPrivacyTimer();
        });

        // Crisis support banner
        CardView crisisBanner = findViewById(R.id.crisisBanner);
        crisisBanner.setOnClickListener(v -> {
            resetPrivacyTimer();
            new AlertDialog.Builder(this)
                    .setTitle("🚨 Crisis Support")
                    .setMessage("Umang helpline: 0317-4288665\nRozan Counseling: 051-2890505\nLUMS CAPS: 042-35608000")
                    .setPositiveButton("Call Now", null)
                    .setNegativeButton("Dismiss", null)
                    .show();
        });

        // Find My Match → QuizActivity
        CardView findMatchCard = findViewById(R.id.findMatchCard);
        findMatchCard.setOnClickListener(v -> {
            resetPrivacyTimer();
            startActivity(new Intent(this, QuizActivity.class));
        });

        // AI Chat → placeholder
        CardView aiChatCard = findViewById(R.id.aiChatCard);
        aiChatCard.setOnClickListener(v -> {
            resetPrivacyTimer();
            Toast.makeText(this, "AI Chat coming soon!", Toast.LENGTH_SHORT).show();
        });

        // Slide-to-cancel SeekBar
        SeekBar slideToCancelSlider = findViewById(R.id.slideToCancelSlider);
        slideToCancelSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}
            @Override public void onStartTrackingTouch(SeekBar seekBar) { resetPrivacyTimer(); }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (seekBar.getProgress() >= 95) {
                    handleCancellation();
                } else {
                    seekBar.setProgress(0);
                }
            }
        });

        // Post-session feedback
        findViewById(R.id.btnPostSessionFeedback).setOnClickListener(v -> {
            resetPrivacyTimer();
            showFeedbackDialog();
        });

        // Bottom navigation
        findViewById(R.id.navCalendar).setOnClickListener(v ->
                startActivity(new Intent(this, CalendarActivity.class)));

        findViewById(R.id.navHistory).setOnClickListener(v ->
                startActivity(new Intent(this, HistoryActivity.class)));

        ImageButton navLogout = findViewById(R.id.navLogout);
        navLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        // Eye button in real top bar → activate discreet mode (fake event app)
        ImageButton discreetBtn = findViewById(R.id.discreetModeBtn);
        discreetBtn.setOnClickListener(v -> activateDiscreetMode());
    }

    private void showFeedbackDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_feedback, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        RatingBar ratingBar = dialogView.findViewById(R.id.feedbackRating);
        EditText commentEdit = dialogView.findViewById(R.id.feedbackComment);
        Button submitBtn = dialogView.findViewById(R.id.btnSubmitFeedback);
        submitBtn.setOnClickListener(v -> {
            if (ratingBar.getRating() == 0) {
                Toast.makeText(this, "Please provide a rating", Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, "Thank you for your feedback!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        dialog.show();
    }

    private void handleCancellation() {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Appointment?")
                .setMessage("Are you sure you want to release this slot?")
                .setPositiveButton("Yes, Cancel", (dialog, which) -> {
                    Toast.makeText(this, "Appointment Cancelled", Toast.LENGTH_SHORT).show();
                    findViewById(R.id.slideToCancelSlider).setVisibility(View.GONE);
                    counselorNameText.setText("No upcoming session");
                    counselorRoleText.setText("Book an appointment below");
                    findViewById(R.id.sessionTimeRow).setVisibility(View.GONE);
                })
                .setNegativeButton("No", (dialog, which) ->
                        ((SeekBar) findViewById(R.id.slideToCancelSlider)).setProgress(0))
                .show();
    }

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
        if (isMasked) return;
        originalName = counselorNameText.getText().toString();
        originalRole = counselorRoleText.getText().toString();
        counselorNameText.setText("••••••••••••");
        counselorRoleText.setText("••••••••••••");
        isMasked = true;
        activateDiscreetMode();
    }

    private void unmaskPII() {
        if (!isMasked) return;
        counselorNameText.setText(originalName);
        counselorRoleText.setText(originalRole);
        isMasked = false;
    }

    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent ev) {
        // Don't reset the inactivity timer while the fake-app overlay is showing —
        // only the eye-slash button inside the overlay can dismiss it.
        if (!isOverlayActive) {
            resetPrivacyTimer();
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onPause() {
        super.onPause();
        privacyHandler.removeCallbacks(privacyRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        resetPrivacyTimer();
    }
}
