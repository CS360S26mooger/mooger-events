/*
 * StudentHomeActivity.java
 * Role: Main landing page for authenticated students.
 *       Sprint 1 version: displays a personalised welcome message and a logout button.
 *       Sprint 2 will add the "Find a Counselor" entry point.
 *       Sprint 4 will add the emergency FAB.
 *
 * Pattern: Uses UserRepository to fetch the student's preferred name on load.
 *
 * Part of the BetterCAPS counseling platform.
 */
package com.example.moogerscouncil;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Home screen for authenticated students. Fetches the student's preferred name
 * from Firestore and displays a personalised welcome header. Provides a
 * logout button that signs the user out and returns them to the login screen.
 */
public class StudentHomeActivity extends AppCompatActivity {

    private TextView welcomeNameView;
    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_home);

        userRepository = new UserRepository();

        welcomeNameView = findViewById(R.id.textPreferredName);
        MaterialButton logoutButton = findViewById(R.id.buttonLogout);

        loadStudentName();

        logoutButton.setOnClickListener(v -> logout());
    }

    /**
     * Fetches the current student's preferred name from Firestore and updates
     * the welcome header. Shows a Toast on failure but does not block the UI.
     */
    private void loadStudentName() {
        userRepository.getCurrentUser(new UserRepository.OnUserFetchedCallback() {
            @Override
            public void onSuccess(Student student) {
                String displayName = student.getPreferredName() != null
                        && !student.getPreferredName().isEmpty()
                        ? student.getPreferredName()
                        : student.getName();
                welcomeNameView.setText(displayName);
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(StudentHomeActivity.this,
                        getString(R.string.error_fetching_role), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Signs the user out of Firebase Auth and navigates back to {@link LoginActivity},
     * clearing the back stack so the user cannot return to this screen without logging in.
     */
    private void logout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
