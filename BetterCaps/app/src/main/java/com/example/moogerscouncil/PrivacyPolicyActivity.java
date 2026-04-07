/*
 * PrivacyPolicyActivity.java
 * Role: Mandatory one-time privacy policy gate shown after registration.
 *       The user must tap "I Agree" to proceed to the correct home screen.
 *
 * Navigation: RegisterActivity → PrivacyPolicyActivity → StudentHomeActivity
 *             (or CounselorDashboardActivity when that screen exists in Sprint 4)
 *
 * Part of the BetterCAPS counseling platform.
 */
package com.example.moogerscouncil;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

/**
 * Displays the BetterCAPS privacy policy and requires the user to acknowledge it
 * before proceeding to the application. This screen is shown exactly once —
 * immediately after a successful registration.
 *
 * <p>The activity receives a {@code "role"} Intent extra to determine which
 * home screen to route to after the user agrees.
 */
public class PrivacyPolicyActivity extends AppCompatActivity {

    /** Intent extra key for the user's role string. */
    public static final String EXTRA_ROLE = "role";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_policy);

        String role = getIntent().getStringExtra(EXTRA_ROLE);

        TextView titleView = findViewById(R.id.privacyTitle);
        MaterialButton agreeButton = findViewById(R.id.buttonAgree);

        titleView.setText(R.string.title_privacy_policy);

        agreeButton.setOnClickListener(v -> routeToHome(role));
    }

    /**
     * Routes the user to the appropriate home screen based on their role,
     * then finishes this activity so the back stack does not return here.
     *
     * @param role the role string from the Intent extra (see {@link UserRole}).
     */
    private void routeToHome(String role) {
        Intent intent;
        if (UserRole.COUNSELOR.equals(role)) {
            // CounselorDashboardActivity will be created in Sprint 4.
            // For now, fall back to StudentHomeActivity as a safe placeholder.
            intent = new Intent(this, StudentHomeActivity.class);
        } else {
            intent = new Intent(this, StudentHomeActivity.class);
        }
        startActivity(intent);
        finish();
    }
}
