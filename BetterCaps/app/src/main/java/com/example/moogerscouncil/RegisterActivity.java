/*
 * RegisterActivity.java
 * Role: Full student/counselor registration screen.
 *       Collects name, email, preferred name, pronouns, and password,
 *       creates a Firebase Auth account, writes a Firestore user document,
 *       then navigates to PrivacyPolicyActivity.
 *
 * Flow: LoginActivity → RegisterActivity → PrivacyPolicyActivity → StudentHomeActivity
 *
 * Part of the BetterCAPS counseling platform.
 */
package com.example.moogerscouncil;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Registration screen. Handles form validation, Firebase Auth account creation,
 * and Firestore user document creation via {@link UserRepository}.
 *
 * <p>The user's selected role is received as an Intent extra from {@link LoginActivity}.
 * If no role is provided, it defaults to {@link UserRole#STUDENT}.
 */
public class RegisterActivity extends AppCompatActivity {

    // Layouts kept as fields so validateInputs() can call layout.setError() directly,
    // which avoids triggering the EditText's own compound-drawable error icon.
    private TextInputLayout layoutName;
    private TextInputLayout layoutEmail;
    private TextInputLayout layoutPassword;
    private TextInputLayout layoutConfirmPassword;

    private TextInputEditText editTextName;
    private TextInputEditText editTextEmail;
    private TextInputEditText editTextPreferredName;
    private TextInputEditText editTextPronouns;
    private TextInputEditText editTextPassword;
    private TextInputEditText editTextConfirmPassword;

    private FirebaseAuth mAuth;
    private UserRepository userRepository;
    private String selectedRole;

    /** Tint color for the required-field asterisk icon. */
    private static final int REQUIRED_TINT = 0xFFCC2222;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        userRepository = new UserRepository();

        selectedRole = getIntent().getStringExtra("selected_role");
        if (selectedRole == null) {
            selectedRole = UserRole.STUDENT;
        }

        layoutName            = findViewById(R.id.layoutName);
        layoutEmail           = findViewById(R.id.layoutEmail);
        layoutPassword        = findViewById(R.id.layoutPassword);
        layoutConfirmPassword = findViewById(R.id.layoutConfirmPassword);

        editTextName            = findViewById(R.id.editTextName);
        editTextEmail           = findViewById(R.id.editTextEmail);
        editTextPreferredName   = findViewById(R.id.editTextPreferredName);
        editTextPronouns        = findViewById(R.id.editTextPronouns);
        editTextPassword        = findViewById(R.id.editTextPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);

        // Asterisk disappears when text is typed, reappears when field is cleared.
        // Uses setStartIconDrawable(null) to fully collapse the slot so no gap remains.
        addAsteriskHintBehavior(editTextName,            layoutName);
        addAsteriskHintBehavior(editTextEmail,           layoutEmail);
        addAsteriskHintBehavior(editTextPassword,        layoutPassword);
        addAsteriskHintBehavior(editTextConfirmPassword, layoutConfirmPassword);

        MaterialButton buttonRegister = findViewById(R.id.buttonRegister);
        TextView linkLogin            = findViewById(R.id.linkLogin);
        View backArrow                = findViewById(R.id.backArrow);

        buttonRegister.setOnClickListener(v -> attemptRegistration());
        linkLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
        backArrow.setOnClickListener(v -> finish());
    }

    /**
     * Attaches a {@link TextWatcher} to {@code editText} that:
     * <ul>
     *   <li>Calls {@code layout.setStartIconDrawable(null)} when text is present —
     *       fully collapses the icon slot so no whitespace gap remains.</li>
     *   <li>Restores the required-field asterisk drawable when the field is empty again.</li>
     *   <li>Clears any active error on the layout when the user starts typing.</li>
     * </ul>
     *
     * @param editText the input field to observe.
     * @param layout   the surrounding TextInputLayout whose start icon is toggled.
     */
    private void addAsteriskHintBehavior(TextInputEditText editText, TextInputLayout layout) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 0) {
                    // Field is empty — restore asterisk with correct tint
                    layout.setStartIconDrawable(R.drawable.ic_required);
                    layout.setStartIconTintList(ColorStateList.valueOf(REQUIRED_TINT));
                } else {
                    // Field has content — fully remove icon slot (no gap)
                    layout.setStartIconDrawable(null);
                    // Also clear any stale error so the layout resets visually
                    layout.setError(null);
                }
            }
        });
    }

    /**
     * Validates form inputs locally, then proceeds to Firebase Auth account creation
     * if all fields pass. No network calls are made before validation.
     */
    private void attemptRegistration() {
        if (!validateInputs()) {
            return;
        }

        String name          = editTextName.getText().toString().trim();
        String email         = editTextEmail.getText().toString().trim();
        String preferredName = editTextPreferredName.getText().toString().trim();
        String pronouns      = editTextPronouns.getText().toString().trim();
        String password      = editTextPassword.getText().toString();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser().getUid();
                    Student student = new Student(uid, name, email, preferredName, pronouns);

                    if (!UserRole.STUDENT.equals(selectedRole)) {
                        student.setRole(selectedRole);
                    }

                    userRepository.createUser(student, new UserRepository.OnUserCreatedCallback() {
                        @Override
                        public void onSuccess() {
                            Intent intent = new Intent(RegisterActivity.this, PrivacyPolicyActivity.class);
                            intent.putExtra(PrivacyPolicyActivity.EXTRA_ROLE, selectedRole);
                            startActivity(intent);
                            finish();
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Toast.makeText(RegisterActivity.this,
                                    getString(R.string.error_firestore_write),
                                    Toast.LENGTH_LONG).show();
                            Intent intent = new Intent(RegisterActivity.this, PrivacyPolicyActivity.class);
                            intent.putExtra(PrivacyPolicyActivity.EXTRA_ROLE, selectedRole);
                            startActivity(intent);
                            finish();
                        }
                    });
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                getString(R.string.error_registration_failed) + " " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
    }

    /**
     * Validates all form fields synchronously before making any network calls.
     * Calls {@code layout.setError()} (not {@code editText.setError()}) so that
     * the TextInputLayout controls the error display and the EditText's own
     * compound-drawable error icon is never triggered.
     *
     * @return {@code true} if all fields pass validation; {@code false} otherwise.
     */
    private boolean validateInputs() {
        boolean valid = true;

        String name            = editTextName.getText().toString().trim();
        String email           = editTextEmail.getText().toString().trim();
        String password        = editTextPassword.getText().toString();
        String confirmPassword = editTextConfirmPassword.getText().toString();

        if (name.isEmpty()) {
            layoutName.setError(getString(R.string.error_name_required));
            valid = false;
        } else {
            layoutName.setError(null);
        }

        if (email.isEmpty()) {
            layoutEmail.setError(getString(R.string.error_email_required));
            valid = false;
        } else if (!email.endsWith("@lums.edu.pk")) {
            layoutEmail.setError(getString(R.string.error_email_domain));
            valid = false;
        } else {
            layoutEmail.setError(null);
        }

        if (password.length() < 6) {
            layoutPassword.setError(getString(R.string.error_password_length));
            valid = false;
        } else {
            layoutPassword.setError(null);
        }

        if (!password.equals(confirmPassword)) {
            layoutConfirmPassword.setError(getString(R.string.error_password_mismatch));
            valid = false;
        } else {
            layoutConfirmPassword.setError(null);
        }

        return valid;
    }
}
