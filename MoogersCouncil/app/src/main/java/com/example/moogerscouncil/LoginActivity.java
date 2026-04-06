package com.example.moogerscouncil;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Login screen. Handles student/counselor/admin role selection
 * and Firebase email authentication.
 */
public class LoginActivity extends AppCompatActivity {

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

        // If already logged in, skip to dashboard or home based on role
        if (mAuth.getCurrentUser() != null) {
            checkUserRole(mAuth.getCurrentUser().getUid());
            return;
        }

        emailField = findViewById(R.id.emailField);
        passwordField = findViewById(R.id.passwordField);
        loginButton = findViewById(R.id.loginButton);
        registerLink = findViewById(R.id.registerLink);
        btnStudent = findViewById(R.id.btnStudent);
        btnCounselor = findViewById(R.id.btnCounselor);
        btnAdmin = findViewById(R.id.btnAdmin);

        // Role selection
        btnStudent.setOnClickListener(v -> {
            selectedRole = "Student";
            loginButton.setText("Log In as Student");
        });
        btnCounselor.setOnClickListener(v -> {
            selectedRole = "Counselor";
            loginButton.setText("Log In as Counselor");
        });
        btnAdmin.setOnClickListener(v -> {
            selectedRole = "Admin";
            loginButton.setText("Log In as Admin");
        });

        loginButton.setOnClickListener(v -> {
            String email = emailField.getText().toString().trim();
            String password = passwordField.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener(result -> {
                        String uid = result.getUser().getUid();
                        checkUserRole(uid);
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Login failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );
        });

        registerLink.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class))
        );
    }

    private void checkUserRole(String uid) {
        FirebaseFirestore.getInstance().collection("users").document(uid)
                .get()
                .addOnSuccessListener(result -> {
                    Toast.makeText(this, "Welcome!", Toast.LENGTH_SHORT).show();
                    if (selectedRole.equals("Counselor")) {
                        startActivity(new Intent(this, CounselorDashboardActivity.class));
                    } else {
                        startActivity(new Intent(this, HomeActivity.class));
                    }
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}