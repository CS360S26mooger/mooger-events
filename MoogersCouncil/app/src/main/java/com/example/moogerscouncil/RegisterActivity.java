package com.example.moogerscouncil;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

/**
 * Activity for registering new student users.
 * Authenticates via Firebase and creates a user profile in Firestore.
 */
public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText nameField, emailField, passwordField;
    private MaterialButton registerButton;
    private TextView loginLink;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        nameField = findViewById(R.id.nameField);
        emailField = findViewById(R.id.emailField);
        passwordField = findViewById(R.id.passwordField);
        registerButton = findViewById(R.id.registerButton);
        loginLink = findViewById(R.id.loginLink);
        progressBar = findViewById(R.id.progressBar);

        registerButton.setOnClickListener(v -> handleRegistration());

        loginLink.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void handleRegistration() {
        String name = nameField.getText().toString().trim();
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        registerButton.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser().getUid();
                    saveUserToFirestore(uid, name, email);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    registerButton.setEnabled(true);
                    Toast.makeText(this, "Registration failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void saveUserToFirestore(String uid, String name, String email) {
        Map<String, Object> user = new HashMap<>();
        user.put("uid", uid);
        user.put("name", name);
        user.put("email", email);
        user.put("role", "Student"); // Default role

        db.collection("users").document(uid)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Account created!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, HomeActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    registerButton.setEnabled(true);
                    Toast.makeText(this, "Failed to save profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}