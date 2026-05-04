package com.example.moogerscouncil;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Landing screen for admin-only Sprint 10 reminder configuration.
 */
public class AdminDashboardActivity extends AppCompatActivity {
    private ReminderRepository reminderRepository;
    private ReminderScheduler reminderScheduler;
    private TextView textReminderSummary;
    private MaterialButton btnGenerateReminderRecords;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        reminderRepository = new ReminderRepository();
        reminderScheduler = new ReminderScheduler();

        textReminderSummary = findViewById(R.id.textReminderSummary);
        btnGenerateReminderRecords = findViewById(R.id.btnGenerateReminderRecords);

        findViewById(R.id.btnReminderSettings).setOnClickListener(v ->
                startActivity(new Intent(this, ReminderSettingsActivity.class)));
        btnGenerateReminderRecords.setOnClickListener(v -> generateReminderRecords());
        findViewById(R.id.btnAdminLogout).setOnClickListener(v -> {
            SessionCache.getInstance().clearAll();
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finishAffinity();
        });

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            AppToast.show(this, R.string.error_login_required, AppToast.LENGTH_SHORT);
        }
        loadReminderSummary();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadReminderSummary();
    }

    private void loadReminderSummary() {
        textReminderSummary.setText(R.string.reminder_summary_placeholder);
        reminderRepository.getSettings(new ReminderRepository.OnSettingsLoadedCallback() {
            @Override
            public void onSuccess(ReminderSettings settings) {
                textReminderSummary.setText(getString(
                        R.string.reminder_summary_format,
                        settings.isEnabled24Hour() ? getString(R.string.enabled) : getString(R.string.disabled),
                        settings.isEnabled1Hour() ? getString(R.string.enabled) : getString(R.string.disabled)));
            }

            @Override
            public void onFailure(Exception e) {
                textReminderSummary.setText(R.string.error_loading_reminder_settings);
            }
        });
    }

    private void generateReminderRecords() {
        btnGenerateReminderRecords.setEnabled(false);
        btnGenerateReminderRecords.setVisibility(View.INVISIBLE);
        reminderScheduler.generateReminderRecords(new ReminderScheduler.OnSchedulerCallback() {
            @Override
            public void onSuccess(int recordsCreated) {
                btnGenerateReminderRecords.setEnabled(true);
                btnGenerateReminderRecords.setVisibility(View.VISIBLE);
                AppToast.show(AdminDashboardActivity.this,
                        getString(R.string.reminder_records_generated, recordsCreated),
                        AppToast.LENGTH_LONG);
            }

            @Override
            public void onFailure(Exception e) {
                btnGenerateReminderRecords.setEnabled(true);
                btnGenerateReminderRecords.setVisibility(View.VISIBLE);
                AppToast.show(AdminDashboardActivity.this,
                        R.string.error_generating_reminder_records,
                        AppToast.LENGTH_LONG);
            }
        });
    }
}
