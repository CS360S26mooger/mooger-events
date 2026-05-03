package com.example.moogerscouncil;

import android.os.Bundle;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Counselor-facing scheduling preferences screen for buffer time and calendar export.
 */
public class AvailabilitySettingsActivity extends AppCompatActivity {

    private String counselorId;
    private AvailabilitySettingsRepository repository;
    private RadioGroup radioBufferGroup;
    private RadioGroup radioCalendarProviderGroup;
    private SwitchMaterial switchCalendarExport;
    private SwitchMaterial switchIcsExport;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_availability_settings);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }

        counselorId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        repository = new AvailabilitySettingsRepository();

        radioBufferGroup = findViewById(R.id.radioBufferGroup);
        radioCalendarProviderGroup = findViewById(R.id.radioCalendarProviderGroup);
        switchCalendarExport = findViewById(R.id.switchCalendarExport);
        switchIcsExport = findViewById(R.id.switchIcsExport);
        MaterialButton buttonSave = findViewById(R.id.buttonSaveAvailabilitySettings);

        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());
        buttonSave.setOnClickListener(v -> saveSettings());
        loadSettings();
    }

    private void loadSettings() {
        AvailabilitySettings cached = SessionCache.getInstance().getSettings(counselorId);
        if (cached != null) {
            applySettings(cached);
            return;
        }

        repository.getSettings(counselorId, new AvailabilitySettingsRepository.OnSettingsLoadedCallback() {
            @Override
            public void onSuccess(AvailabilitySettings settings) {
                SessionCache.getInstance().putSettings(counselorId, settings);
                applySettings(settings);
            }

            @Override
            public void onFailure(Exception e) {
                applyBufferSelection(0);
            }
        });
    }

    private void applySettings(AvailabilitySettings settings) {
        applyBufferSelection(settings.getBufferMinutes());
        switchCalendarExport.setChecked(settings.isExternalCalendarEnabled());
        switchIcsExport.setChecked(settings.isExportIcsEnabled());
        applyProviderSelection(settings.getCalendarProvider());
    }

    private void saveSettings() {
        AvailabilitySettings settings = new AvailabilitySettings(counselorId, selectedBuffer());
        settings.setExternalCalendarEnabled(switchCalendarExport.isChecked());
        settings.setExportIcsEnabled(switchIcsExport.isChecked());
        settings.setCalendarProvider(selectedProvider());
        repository.saveSettings(settings, new AvailabilitySettingsRepository.OnSettingsSavedCallback() {
            @Override
            public void onSuccess() {
                SessionCache.getInstance().putSettings(counselorId, settings);
                AppToast.show(AvailabilitySettingsActivity.this,
                        R.string.settings_saved,
                        AppToast.LENGTH_SHORT);
            }

            @Override
            public void onFailure(Exception e) {
                AppToast.show(AvailabilitySettingsActivity.this,
                        R.string.settings_save_error,
                        AppToast.LENGTH_LONG);
            }
        });
    }

    private int selectedBuffer() {
        int checkedId = radioBufferGroup.getCheckedRadioButtonId();
        if (checkedId == R.id.radioBuffer10) return 10;
        if (checkedId == R.id.radioBuffer15) return 15;
        if (checkedId == R.id.radioBuffer30) return 30;
        return 0;
    }

    private String selectedProvider() {
        int checkedId = radioCalendarProviderGroup.getCheckedRadioButtonId();
        if (checkedId == R.id.radioProviderGoogle) return AvailabilitySettings.PROVIDER_GOOGLE;
        if (checkedId == R.id.radioProviderOutlook) return AvailabilitySettings.PROVIDER_OUTLOOK;
        if (checkedId == R.id.radioProviderDevice) return AvailabilitySettings.PROVIDER_DEVICE;
        return AvailabilitySettings.PROVIDER_NONE;
    }

    private void applyBufferSelection(int bufferMinutes) {
        if (bufferMinutes == 10) radioBufferGroup.check(R.id.radioBuffer10);
        else if (bufferMinutes == 15) radioBufferGroup.check(R.id.radioBuffer15);
        else if (bufferMinutes == 30) radioBufferGroup.check(R.id.radioBuffer30);
        else radioBufferGroup.check(R.id.radioBuffer0);
    }

    private void applyProviderSelection(String provider) {
        if (AvailabilitySettings.PROVIDER_GOOGLE.equals(provider)) {
            radioCalendarProviderGroup.check(R.id.radioProviderGoogle);
        } else if (AvailabilitySettings.PROVIDER_OUTLOOK.equals(provider)) {
            radioCalendarProviderGroup.check(R.id.radioProviderOutlook);
        } else if (AvailabilitySettings.PROVIDER_DEVICE.equals(provider)) {
            radioCalendarProviderGroup.check(R.id.radioProviderDevice);
        } else {
            radioCalendarProviderGroup.check(R.id.radioProviderNone);
        }
    }
}
