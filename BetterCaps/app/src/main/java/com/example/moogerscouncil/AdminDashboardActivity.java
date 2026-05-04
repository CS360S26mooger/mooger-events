/*
 * AdminDashboardActivity.java
 * Role: Admin landing screen with two tabs: Reminders and Crisis Alerts.
 *       Reminders tab: reminder summary, pending count, generate records, settings nav.
 *       Crisis Alerts tab: live list of unresolved escalations with "Mark Resolved" actions.
 *
 * Design pattern: Repository pattern (data layer); TabLayout manual tab switching.
 * Part of the BetterCAPS counseling platform.
 */
package com.example.moogerscouncil;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

/**
 * Admin dashboard for managing reminder configuration and monitoring escalations.
 *
 * <p>Tab 0 — Reminders: summary card + "Generate Reminder Records" idempotent action.</p>
 * <p>Tab 1 — Crisis Alerts: unresolved escalations from {@link CrisisEscalationRepository}.</p>
 */
public class AdminDashboardActivity extends AppCompatActivity {

    private ReminderRepository reminderRepository;
    private ReminderScheduler reminderScheduler;
    private CrisisEscalationRepository crisisRepository;
    private CrisisAlertAdapter crisisAlertAdapter;

    private TextView textReminderSummary;
    private TextView textPendingCount;
    private MaterialButton btnGenerateReminderRecords;
    private ProgressBar progressGenerate;
    private View tabReminders;
    private View tabCrisisAlerts;
    private TextView textCrisisAlertsEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        reminderRepository = new ReminderRepository();
        reminderScheduler  = new ReminderScheduler();
        crisisRepository   = new CrisisEscalationRepository();

        textReminderSummary        = findViewById(R.id.textReminderSummary);
        textPendingCount           = findViewById(R.id.textPendingCount);
        btnGenerateReminderRecords = findViewById(R.id.btnGenerateReminderRecords);
        progressGenerate           = findViewById(R.id.progressGenerate);
        tabReminders               = findViewById(R.id.tabReminders);
        tabCrisisAlerts            = findViewById(R.id.tabCrisisAlerts);
        textCrisisAlertsEmpty      = findViewById(R.id.textCrisisAlertsEmpty);

        // Tab switching
        TabLayout tabLayout = findViewById(R.id.tabLayoutAdmin);
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_reminders));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_crisis_alerts));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                showTab(tab.getPosition());
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Reminders tab wiring
        findViewById(R.id.btnReminderSettings).setOnClickListener(v ->
                startActivity(new Intent(this, ReminderSettingsActivity.class)));
        btnGenerateReminderRecords.setOnClickListener(v -> generateReminderRecords());

        // Crisis Alerts RecyclerView
        RecyclerView recyclerCrisis = findViewById(R.id.recyclerCrisisAlerts);
        crisisAlertAdapter = new CrisisAlertAdapter();
        recyclerCrisis.setLayoutManager(new LinearLayoutManager(this));
        recyclerCrisis.setAdapter(crisisAlertAdapter);

        // Logout — same confirmation dialog as counselor/student screens
        findViewById(R.id.btnAdminLogout).setOnClickListener(v -> showLogoutConfirmation());

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            AppToast.show(this, R.string.error_login_required, AppToast.LENGTH_SHORT);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadReminderSummary();
        loadPendingCount();
        loadCrisisAlerts();
    }

    // ── Tab switching ─────────────────────────────────────────────────────────

    private void showTab(int position) {
        tabReminders.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
        tabCrisisAlerts.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
    }

    // ── Reminders tab ─────────────────────────────────────────────────────────

    private void loadReminderSummary() {
        textReminderSummary.setText(R.string.reminder_summary_placeholder);
        reminderRepository.getSettings(new ReminderRepository.OnSettingsLoadedCallback() {
            @Override
            public void onSuccess(ReminderSettings settings) {
                textReminderSummary.setText(getString(
                        R.string.reminder_summary_format,
                        settings.isEnabled24Hour()
                                ? getString(R.string.enabled) : getString(R.string.disabled),
                        settings.isEnabled1Hour()
                                ? getString(R.string.enabled) : getString(R.string.disabled)));
            }

            @Override
            public void onFailure(Exception e) {
                textReminderSummary.setText(R.string.error_loading_reminder_settings);
            }
        });
    }

    private void loadPendingCount() {
        if (textPendingCount == null) return;
        textPendingCount.setText(R.string.reminder_pending_loading);
        reminderRepository.getPendingReminderCount(new ReminderRepository.OnCountCallback() {
            @Override
            public void onSuccess(int count) {
                textPendingCount.setText(getResources().getQuantityString(
                        R.plurals.reminder_pending_count, count, count));
            }

            @Override
            public void onFailure(Exception e) {
                textPendingCount.setText(R.string.reminder_pending_unknown);
            }
        });
    }

    private void generateReminderRecords() {
        setGenerateUiLoading(true);
        reminderScheduler.generateReminderRecords(new ReminderScheduler.OnSchedulerCallback() {
            @Override
            public void onSuccess(int recordsCreated) {
                setGenerateUiLoading(false);
                String msg = recordsCreated == 0
                        ? getString(R.string.reminder_records_up_to_date)
                        : getString(R.string.reminder_records_generated, recordsCreated);
                AppToast.show(AdminDashboardActivity.this, msg, AppToast.LENGTH_LONG);
                loadPendingCount();
            }

            @Override
            public void onFailure(Exception e) {
                setGenerateUiLoading(false);
                AppToast.show(AdminDashboardActivity.this,
                        R.string.error_generating_reminder_records, AppToast.LENGTH_LONG);
            }
        });
    }

    private void setGenerateUiLoading(boolean loading) {
        btnGenerateReminderRecords.setEnabled(!loading);
        if (progressGenerate != null) {
            progressGenerate.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    private void showLogoutConfirmation() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_exit);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.88f);
            params.gravity = Gravity.CENTER;
            window.setAttributes(params);
        }

        MaterialButton btnConfirm = dialog.findViewById(R.id.btnExitConfirm);
        MaterialButton btnCancel  = dialog.findViewById(R.id.btnExitCancel);
        btnConfirm.setText(R.string.logout_confirm_yes);
        btnCancel.setText(R.string.button_cancel);

        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            SessionCache.getInstance().clearAll();
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finishAffinity();
        });
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // ── Crisis Alerts tab ─────────────────────────────────────────────────────

    private void loadCrisisAlerts() {
        crisisRepository.getUnresolvedEscalations(
                new CrisisEscalationRepository.OnEscalationsLoadedCallback() {
                    @Override
                    public void onSuccess(List<CrisisEscalation> escalations) {
                        crisisAlertAdapter.setData(escalations);
                        textCrisisAlertsEmpty.setVisibility(
                                escalations.isEmpty() ? View.VISIBLE : View.GONE);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        AppToast.show(AdminDashboardActivity.this,
                                R.string.crisis_alerts_load_error, AppToast.LENGTH_SHORT);
                    }
                });
    }
}
