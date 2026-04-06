package com.example.moogerscouncil;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Login screen. Handles student/counselor/admin role selection
 * and Firebase email authentication.
 */
public class LoginActivity extends AppCompatActivity {

    private static final int ANIM_DURATION_MS = 200;

    private TextInputEditText emailField, passwordField;
    private MaterialButton loginButton, btnStudent, btnCounselor, btnAdmin;
    private TextView registerLink;
    private FirebaseAuth mAuth;
    private String selectedRole = "Student";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        // If already logged in, skip to home
        if (mAuth.getCurrentUser() != null) {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
            return;
        }

        emailField    = findViewById(R.id.emailField);
        passwordField = findViewById(R.id.passwordField);
        loginButton   = findViewById(R.id.loginButton);
        registerLink  = findViewById(R.id.registerLink);
        btnStudent    = findViewById(R.id.btnStudent);
        btnCounselor  = findViewById(R.id.btnCounselor);
        btnAdmin      = findViewById(R.id.btnAdmin);

        // Apply initial selection to Student immediately (no animation on first load)
        applySelectedStyle(btnStudent, true);

        // Role selection with animated color transitions
        btnStudent.setOnClickListener(v -> selectRole("Student"));
        btnCounselor.setOnClickListener(v -> selectRole("Counselor"));
        btnAdmin.setOnClickListener(v -> selectRole("Admin"));

        loginButton.setOnClickListener(v -> {
            String email    = emailField.getText().toString().trim();
            String password = passwordField.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener(result -> {
                        Toast.makeText(this, "Welcome!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, HomeActivity.class));
                        finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Login failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );
        });

        registerLink.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class))
        );
    }

    /**
     * Deselects all role buttons, then animates the newly selected one to blue.
     */
    private void selectRole(String role) {
        if (role.equals(selectedRole)) return;
        selectedRole = role;

        loginButton.setText("Log In as " + role);

        // Animate every button to its correct state
        animateRoleButton(btnStudent,   "Student".equals(role));
        animateRoleButton(btnCounselor, "Counselor".equals(role));
        animateRoleButton(btnAdmin,     "Admin".equals(role));
    }

    /**
     * Smoothly interpolates stroke color and text color between selected (blue) and
     * unselected (gray) states using a ValueAnimator.
     */
    private void animateRoleButton(MaterialButton button, boolean selected) {
        int colorBlue  = ContextCompat.getColor(this, R.color.primary_blue);
        int colorGray  = ContextCompat.getColor(this, R.color.text_gray);
        int colorStroke = ContextCompat.getColor(this, R.color.card_stroke);

        // Determine start colors from the button's current tint lists
        int currentTextColor   = button.getCurrentTextColor();
        int currentStrokeColor = button.getStrokeColor() != null
                ? button.getStrokeColor().getDefaultColor()
                : colorStroke;

        int targetTextColor   = selected ? colorBlue : colorGray;
        int targetStrokeColor = selected ? colorBlue : colorStroke;

        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(ANIM_DURATION_MS);
        animator.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());

        int currentIconTint = button.getIconTint() != null
                ? button.getIconTint().getDefaultColor()
                : colorGray;
        int targetIconTint = selected ? colorBlue : colorGray;

        ArgbEvaluator evaluator = new ArgbEvaluator();
        animator.addUpdateListener(anim -> {
            float fraction = (float) anim.getAnimatedValue();

            int textColor   = (int) evaluator.evaluate(fraction, currentTextColor, targetTextColor);
            int strokeColor = (int) evaluator.evaluate(fraction, currentStrokeColor, targetStrokeColor);
            int iconTint    = (int) evaluator.evaluate(fraction, currentIconTint, targetIconTint);

            button.setTextColor(textColor);
            button.setStrokeColor(ColorStateList.valueOf(strokeColor));
            button.setIconTint(ColorStateList.valueOf(iconTint));
        });

        animator.start();
    }

    /**
     * Instantly applies selected or unselected styling without animation
     * (used for the initial state on screen load).
     */
    private void applySelectedStyle(MaterialButton button, boolean selected) {
        int colorBlue   = ContextCompat.getColor(this, R.color.primary_blue);
        int colorGray   = ContextCompat.getColor(this, R.color.text_gray);
        int colorStroke = ContextCompat.getColor(this, R.color.card_stroke);

        button.setTextColor(selected ? colorBlue : colorGray);
        button.setStrokeColor(ColorStateList.valueOf(selected ? colorBlue : colorStroke));
        button.setIconTint(ColorStateList.valueOf(selected ? colorBlue : colorGray));
    }
}
