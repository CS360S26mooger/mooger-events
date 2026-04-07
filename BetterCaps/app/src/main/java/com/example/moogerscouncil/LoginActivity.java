/*
 * LoginActivity.java
 * Role: Login screen. Handles role selection (Student / Counselor / Admin),
 *       Firebase email authentication, and role-based routing to the correct home screen.
 *       Auto-redirects to the correct home screen if a live Firebase Auth session exists.
 *
 * Pattern: Uses UserRepository (Repository pattern) for post-login role lookup.
 *          Never accesses FirebaseFirestore directly.
 *
 * Part of the BetterCAPS counseling platform.
 */
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
 * and Firebase email authentication, then routes to the correct home screen
 * based on the user's {@code role} field in Firestore.
 */
public class LoginActivity extends AppCompatActivity {

    private static final int ANIM_DURATION_MS = 200;

    private TextInputEditText emailField;
    private TextInputEditText passwordField;
    private MaterialButton loginButton;
    private MaterialButton btnStudent;
    private MaterialButton btnCounselor;
    private MaterialButton btnAdmin;
    private TextView registerLink;

    private FirebaseAuth mAuth;
    private UserRepository userRepository;
    private String selectedRole = UserRole.STUDENT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        userRepository = new UserRepository();

        // If already logged in, skip login UI and route based on stored role
        if (mAuth.getCurrentUser() != null) {
            routeToHome();
            return;
        }

        setContentView(R.layout.activity_login);

        emailField    = findViewById(R.id.emailField);
        passwordField = findViewById(R.id.passwordField);
        loginButton   = findViewById(R.id.loginButton);
        registerLink  = findViewById(R.id.registerLink);
        btnStudent    = findViewById(R.id.btnStudent);
        btnCounselor  = findViewById(R.id.btnCounselor);
        btnAdmin      = findViewById(R.id.btnAdmin);

        // Apply initial selection to Student immediately (no animation on first load)
        applySelectedStyle(btnStudent, true);

        btnStudent.setOnClickListener(v -> selectRole(UserRole.STUDENT, "Student"));
        btnCounselor.setOnClickListener(v -> selectRole(UserRole.COUNSELOR, "Counselor"));
        btnAdmin.setOnClickListener(v -> selectRole(UserRole.ADMIN, "Admin"));

        loginButton.setOnClickListener(v -> attemptLogin());

        registerLink.setOnClickListener(v -> {
            Intent intent = new Intent(this, RegisterActivity.class);
            intent.putExtra("selected_role", selectedRole);
            startActivity(intent);
        });
    }

    /**
     * Reads email and password from the form fields and signs in with Firebase Auth.
     * On success, fetches the user's role from Firestore and routes to the correct screen.
     */
    private void attemptLogin() {
        String email    = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_login_failed), Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> routeToHome())
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                getString(R.string.error_login_failed) + " " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
    }

    /**
     * Fetches the current user's role from Firestore and starts the appropriate
     * home Activity, finishing this Activity so the back stack does not return here.
     */
    private void routeToHome() {
        userRepository.getCurrentUserRole(new UserRepository.OnRoleFetchedCallback() {
            @Override
            public void onSuccess(String role) {
                Intent intent;
                if (UserRole.COUNSELOR.equals(role)) {
                    // CounselorDashboardActivity will be created in Sprint 4.
                    // Fall back to StudentHomeActivity as a placeholder.
                    intent = new Intent(LoginActivity.this, StudentHomeActivity.class);
                } else {
                    intent = new Intent(LoginActivity.this, StudentHomeActivity.class);
                }
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(LoginActivity.this,
                        getString(R.string.error_fetching_role), Toast.LENGTH_LONG).show();
                // If the login screen was not inflated yet (auto-redirect path),
                // inflate it now so the user can try again.
                if (emailField == null) {
                    setContentView(R.layout.activity_login);
                    emailField    = findViewById(R.id.emailField);
                    passwordField = findViewById(R.id.passwordField);
                    loginButton   = findViewById(R.id.loginButton);
                    registerLink  = findViewById(R.id.registerLink);
                    btnStudent    = findViewById(R.id.btnStudent);
                    btnCounselor  = findViewById(R.id.btnCounselor);
                    btnAdmin      = findViewById(R.id.btnAdmin);
                    applySelectedStyle(btnStudent, true);
                    loginButton.setOnClickListener(v2 -> attemptLogin());
                    registerLink.setOnClickListener(v2 -> {
                        Intent i = new Intent(LoginActivity.this, RegisterActivity.class);
                        i.putExtra("selected_role", selectedRole);
                        startActivity(i);
                    });
                    btnStudent.setOnClickListener(v2 -> selectRole(UserRole.STUDENT, "Student"));
                    btnCounselor.setOnClickListener(v2 -> selectRole(UserRole.COUNSELOR, "Counselor"));
                    btnAdmin.setOnClickListener(v2 -> selectRole(UserRole.ADMIN, "Admin"));
                }
            }
        });
    }

    /**
     * Updates the selected role and animates the role-selector buttons accordingly.
     *
     * @param roleConstant the {@link UserRole} constant string to store.
     * @param displayLabel the label shown in the login button text.
     */
    private void selectRole(String roleConstant, String displayLabel) {
        if (roleConstant.equals(selectedRole)) return;
        selectedRole = roleConstant;

        loginButton.setText("Log In as " + displayLabel);

        animateRoleButton(btnStudent,   UserRole.STUDENT.equals(roleConstant));
        animateRoleButton(btnCounselor, UserRole.COUNSELOR.equals(roleConstant));
        animateRoleButton(btnAdmin,     UserRole.ADMIN.equals(roleConstant));
    }

    /**
     * Smoothly interpolates stroke color, text color, and icon tint between
     * selected (blue) and unselected (gray) states.
     *
     * @param button   the role button to animate.
     * @param selected {@code true} if this button should appear selected.
     */
    private void animateRoleButton(MaterialButton button, boolean selected) {
        int colorBlue   = ContextCompat.getColor(this, R.color.primary_blue);
        int colorGray   = ContextCompat.getColor(this, R.color.text_gray);
        int colorStroke = ContextCompat.getColor(this, R.color.card_stroke);

        int currentTextColor   = button.getCurrentTextColor();
        int currentStrokeColor = button.getStrokeColor() != null
                ? button.getStrokeColor().getDefaultColor()
                : colorStroke;
        int currentIconTint    = button.getIconTint() != null
                ? button.getIconTint().getDefaultColor()
                : colorGray;

        int targetTextColor   = selected ? colorBlue : colorGray;
        int targetStrokeColor = selected ? colorBlue : colorStroke;
        int targetIconTint    = selected ? colorBlue : colorGray;

        ArgbEvaluator evaluator = new ArgbEvaluator();
        ValueAnimator animator  = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(ANIM_DURATION_MS);
        animator.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
        animator.addUpdateListener(anim -> {
            float fraction  = (float) anim.getAnimatedValue();
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
     * Instantly applies selected or unselected styling without animation.
     * Used for the initial state on screen load.
     *
     * @param button   the button to style.
     * @param selected {@code true} for blue selected style; {@code false} for gray.
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
