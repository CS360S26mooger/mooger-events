package com.example.moogerscouncil;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.cardview.widget.CardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Home screen shown after successful login.
 * Displays upcoming sessions, crisis support, and navigation.
 */
public class HomeActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private android.os.Handler privacyHandler = new android.os.Handler();
    private Runnable privacyRunnable;
    private TextView counselorNameText, counselorRoleText;
    private String originalName, originalRole;
    private boolean isMasked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mAuth = FirebaseAuth.getInstance();
        counselorNameText = findViewById(R.id.upcomingCounselorName);
        counselorRoleText = findViewById(R.id.upcomingCounselorRole);

        // Redirect to login if not authenticated
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Initialize original values
        originalName = counselorNameText.getText().toString();
        originalRole = counselorRoleText.getText().toString();

        setupPrivacyTimer();

        // Crisis banner
        CardView crisisBanner = findViewById(R.id.crisisBanner);
        crisisBanner.setOnClickListener(v -> {
            resetPrivacyTimer();
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("🚨 Crisis Support")
                    .setMessage("Umang helpline: 0317-4288665\nRozan Counseling: 051-2890505\nLUMS CAPS: 042-35608000")
                    .setPositiveButton("Call Now", null)
                    .setNegativeButton("Dismiss", null)
                    .show();
        });

        // Find My Match → goes to counselor list for now
        CardView findMatchCard = findViewById(R.id.findMatchCard);
        findMatchCard.setOnClickListener(v -> {
            resetPrivacyTimer();
            startActivity(new Intent(this, CounselorListActivity.class));
        });

        // AI Chat card → placeholder
        CardView aiChatCard = findViewById(R.id.aiChatCard);
        aiChatCard.setOnClickListener(v -> {
            resetPrivacyTimer();
            Toast.makeText(this, "AI Chat coming soon!", Toast.LENGTH_SHORT).show();
        });

        // Slide to cancel (SeekBar)
        android:widget.SeekBar slideToCancelSlider = findViewById(R.id.slideToCancelSlider);
        slideToCancelSlider.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {}

            @Override
            public void onStartTrackingTouch(android.widget.SeekBar seekBar) {
                resetPrivacyTimer();
            }

            @Override
            public void onStopTrackingTouch(android.widget.SeekBar seekBar) {
                if (seekBar.getProgress() >= 95) {
                    // Fully slid
                    handleCancellation();
                } else {
                    // Snap back
                    seekBar.setProgress(0);
                }
            }
        });

        // Logout
        ImageButton navLogout = findViewById(R.id.navLogout);
        navLogout.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        // Discreet mode button (Manual toggle)
        ImageButton discreetBtn = findViewById(R.id.discreetModeBtn);
        discreetBtn.setOnClickListener(v -> {
            if (isMasked) unmaskPII(); else maskPII();
            resetPrivacyTimer();
        });
    }

    private void handleCancellation() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Cancel Appointment?")
                .setMessage("Are you sure you want to release this slot?")
                .setPositiveButton("Yes, Cancel", (dialog, which) -> {
                    Toast.makeText(this, "Appointment Cancelled", Toast.LENGTH_SHORT).show();
                    // In a real app, you would update Firestore here
                    findViewById(R.id.slideToCancelSlider).setVisibility(View.GONE);
                    counselorNameText.setText("No upcoming session");
                    counselorRoleText.setText("Book an appointment below");
                    findViewById(R.id.sessionTimeRow).setVisibility(View.GONE);
                })
                .setNegativeButton("No", (dialog, which) -> {
                    ((android.widget.SeekBar)findViewById(R.id.slideToCancelSlider)).setProgress(0);
                })
                .show();
    }

    private void setupPrivacyTimer() {
        privacyRunnable = this::maskPII;
        resetPrivacyTimer();
    }

    private void resetPrivacyTimer() {
        privacyHandler.removeCallbacks(privacyRunnable);
        if (isMasked) unmaskPII();
        privacyHandler.postDelayed(privacyRunnable, 5000); // 5 seconds
    }

    private void maskPII() {
        if (isMasked) return;
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
        resetPrivacyTimer();
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