package com.example.moogerscouncil;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Admin form for editing global 24-hour and 1-hour reminder settings.
 */
public class ReminderSettingsActivity extends AppCompatActivity {
    private ReminderRepository reminderRepository;
    private SwitchMaterial switch24HourReminder;
    private SwitchMaterial switch1HourReminder;
    private TextInputEditText editMessage24Hour;
    private TextInputEditText editMessage1Hour;
    private MaterialButton btnSaveReminderSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder_settings);

        reminderRepository = new ReminderRepository();
        switch24HourReminder = findViewById(R.id.switch24HourReminder);
        switch1HourReminder = findViewById(R.id.switch1HourReminder);
        editMessage24Hour = findViewById(R.id.editMessage24Hour);
        editMessage1Hour = findViewById(R.id.editMessage1Hour);
        btnSaveReminderSettings = findViewById(R.id.btnSaveReminderSettings);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnSaveReminderSettings.setOnClickListener(v -> saveSettings());

        loadSettings();
    }

    private void loadSettings() {
        reminderRepository.getSettings(new ReminderRepository.OnSettingsLoadedCallback() {
            @Override
            public void onSuccess(ReminderSettings settings) {
                switch24HourReminder.setChecked(settings.isEnabled24Hour());
                switch1HourReminder.setChecked(settings.isEnabled1Hour());
                editMessage24Hour.setText(settings.getMessage24Hour());
                editMessage1Hour.setText(settings.getMessage1Hour());
            }

            @Override
            public void onFailure(Exception e) {
                AppToast.show(ReminderSettingsActivity.this,
                        R.string.error_loading_reminder_settings,
                        AppToast.LENGTH_LONG);
                ReminderSettings defaults = ReminderSettings.defaultSettings();
                switch24HourReminder.setChecked(defaults.isEnabled24Hour());
                switch1HourReminder.setChecked(defaults.isEnabled1Hour());
                editMessage24Hour.setText(defaults.getMessage24Hour());
                editMessage1Hour.setText(defaults.getMessage1Hour());
            }
        });
    }

    private void saveSettings() {
        String message24 = editMessage24Hour.getText() == null
                ? "" : editMessage24Hour.getText().toString().trim();
        String message1 = editMessage1Hour.getText() == null
                ? "" : editMessage1Hour.getText().toString().trim();

        if (message24.isEmpty()) {
            editMessage24Hour.setError(getString(R.string.error_required));
            return;
        }
        if (message1.isEmpty()) {
            editMessage1Hour.setError(getString(R.string.error_required));
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String updatedBy = user == null ? "" : user.getUid();
        ReminderSettings settings = new ReminderSettings(
                switch24HourReminder.isChecked(),
                switch1HourReminder.isChecked(),
                message24,
                message1,
                updatedBy);

        btnSaveReminderSettings.setEnabled(false);
        reminderRepository.saveSettings(settings, new ReminderRepository.OnReminderActionCallback() {
            @Override
            public void onSuccess() {
                btnSaveReminderSettings.setEnabled(true);
                AppToast.show(ReminderSettingsActivity.this,
                        R.string.reminder_settings_saved,
                        AppToast.LENGTH_SHORT);
            }

            @Override
            public void onFailure(Exception e) {
                btnSaveReminderSettings.setEnabled(true);
                AppToast.show(ReminderSettingsActivity.this,
                        R.string.error_saving_reminder_settings,
                        AppToast.LENGTH_LONG);
            }
        });
    }
}
