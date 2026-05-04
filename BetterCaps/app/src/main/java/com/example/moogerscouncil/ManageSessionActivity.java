/*
 * ManageSessionActivity.java
 * Role: Single focused screen for all per-appointment counselor actions.
 *       Replaces the cluttered inline card buttons from AppointmentAdapter.
 *       Shows the full session note list and provides Messages, View Profile,
 *       Mark No-Show, Mark Attended, and Crisis Escalation (one-time guard).
 *
 * Design pattern: Repository pattern (data layer); OnEscalationSavedListener callback.
 * Part of the BetterCAPS counseling platform — Sprint 11 counselor UX declutter.
 */
package com.example.moogerscouncil;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * All-in-one session management hub for a single counselor appointment.
 * Opened by tapping an appointment card in the counselor dashboard.
 */
public class ManageSessionActivity extends AppCompatActivity
        implements CrisisEscalationDialogFragment.OnEscalationSavedListener {

    public static final String EXTRA_APPOINTMENT_ID    = "APPOINTMENT_ID";
    public static final String EXTRA_STUDENT_ID        = "STUDENT_ID";
    public static final String EXTRA_COUNSELOR_ID      = "COUNSELOR_ID";
    public static final String EXTRA_DATE              = "DATE";
    public static final String EXTRA_TIME              = "TIME";
    public static final String EXTRA_STATUS            = "STATUS";
    public static final String EXTRA_STUDENT_NAME      = "STUDENT_NAME";
    public static final String EXTRA_CRISIS_ID         = "CRISIS_ESCALATION_ID";
    public static final String EXTRA_RETURNING_STUDENT = "RETURNING_STUDENT";

    private String appointmentId;
    private String studentId;
    private String counselorId;
    private String date;
    private String time;
    private String status;
    private String crisisEscalationId;

    private MaterialButton btnCrisis;

    private final SessionNoteRepository noteRepository = new SessionNoteRepository();
    private final AppointmentRepository appointmentRepository = new AppointmentRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_session);

        appointmentId     = getIntent().getStringExtra(EXTRA_APPOINTMENT_ID);
        studentId         = getIntent().getStringExtra(EXTRA_STUDENT_ID);
        counselorId       = getIntent().getStringExtra(EXTRA_COUNSELOR_ID);
        date              = getIntent().getStringExtra(EXTRA_DATE);
        time              = getIntent().getStringExtra(EXTRA_TIME);
        status            = getIntent().getStringExtra(EXTRA_STATUS);
        crisisEscalationId = getIntent().getStringExtra(EXTRA_CRISIS_ID);
        boolean returningStudent = getIntent().getBooleanExtra(EXTRA_RETURNING_STUDENT, false);
        String studentName = getIntent().getStringExtra(EXTRA_STUDENT_NAME);

        // Header
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Info card
        TextView textStudentName   = findViewById(R.id.textManageStudentName);
        TextView textStatusBadge   = findViewById(R.id.textManageStatusBadge);
        TextView textDate          = findViewById(R.id.textManageDate);
        TextView textTime          = findViewById(R.id.textManageTime);
        TextView textReturning     = findViewById(R.id.textManageReturningBadge);

        textStudentName.setText(studentName != null ? studentName : getString(R.string.unknown_student));
        textDate.setText(formatDate(date));
        textTime.setText(time != null ? time : "");
        textReturning.setVisibility(returningStudent ? View.VISIBLE : View.GONE);
        applyStatusBadge(textStatusBadge, status);

        // Notes
        loadNotes();

        // Action buttons
        btnCrisis = findViewById(R.id.btnManageCrisis);
        MaterialButton btnMessages     = findViewById(R.id.btnManageMessages);
        MaterialButton btnProfile      = findViewById(R.id.btnManageViewProfile);
        MaterialButton btnNoShow       = findViewById(R.id.btnManageNoShow);
        MaterialButton btnMarkAttended = findViewById(R.id.btnManageMarkAttended);

        btnMessages.setOnClickListener(v -> openMessages());
        btnProfile.setOnClickListener(v -> openStudentProfile());

        // No-Show: only for CONFIRMED within window; hidden for other statuses
        boolean isConfirmed  = "CONFIRMED".equals(status);
        boolean isCancelled  = "CANCELLED".equals(status);
        boolean isNoShow     = "NO_SHOW".equals(status);
        boolean isCompleted  = "COMPLETED".equals(status);

        btnNoShow.setVisibility(isConfirmed ? View.VISIBLE : View.GONE);
        btnNoShow.setOnClickListener(v -> {
            if (!AppointmentAdapter.isNoShowWindowOpen(date, time)) {
                showInfoDialog(R.string.noshow_too_early_title, R.string.noshow_too_early_body);
            } else {
                confirmNoShow();
            }
        });

        // Mark Attended: CONFIRMED + slot has passed
        boolean slotPassed = hasSlotPassed(date, time);
        btnMarkAttended.setVisibility(isConfirmed && slotPassed ? View.VISIBLE : View.GONE);
        btnMarkAttended.setOnClickListener(v -> confirmMarkAttended(btnMarkAttended));

        // Crisis: disabled if already escalated
        boolean alreadyEscalated = crisisEscalationId != null && !crisisEscalationId.isEmpty();
        boolean crisisAllowed = !isCancelled && !isCompleted && !isNoShow;
        if (!crisisAllowed) {
            btnCrisis.setVisibility(View.GONE);
        } else if (alreadyEscalated) {
            applyCrisisFiledState();
        } else {
            btnCrisis.setOnClickListener(v -> openCrisisDialog());
        }

        // Messages button: hidden for cancelled
        btnMessages.setVisibility(isCancelled ? View.GONE : View.VISIBLE);
    }

    // ── Notes ─────────────────────────────────────────────────────────────────

    private void loadNotes() {
        RecyclerView recycler = findViewById(R.id.recyclerManageNotes);
        TextView textEmpty    = findViewById(R.id.textManageNotesEmpty);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setNestedScrollingEnabled(false);

        if (appointmentId == null) {
            textEmpty.setVisibility(View.VISIBLE);
            return;
        }
        noteRepository.getNotesForCounselorStudent(counselorId, studentId,
                new SessionNoteRepository.OnNotesLoadedCallback() {
                    @Override
                    public void onSuccess(List<SessionNote> notes) {
                        if (notes.isEmpty()) {
                            textEmpty.setVisibility(View.VISIBLE);
                            recycler.setVisibility(View.GONE);
                        } else {
                            textEmpty.setVisibility(View.GONE);
                            recycler.setVisibility(View.VISIBLE);
                            recycler.setAdapter(new SessionNoteHistoryAdapter(notes));
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        textEmpty.setVisibility(View.VISIBLE);
                        textEmpty.setText(R.string.note_load_error);
                    }
                });
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void openMessages() {
        Intent intent = new Intent(this, MessageThreadActivity.class);
        intent.putExtra(MessageThreadActivity.EXTRA_STUDENT_ID, studentId);
        intent.putExtra(MessageThreadActivity.EXTRA_COUNSELOR_ID, counselorId);
        intent.putExtra(MessageThreadActivity.EXTRA_SESSION_DATE, date);
        intent.putExtra(MessageThreadActivity.EXTRA_SESSION_TIME, time);
        TextView nameView = findViewById(R.id.textManageStudentName);
        intent.putExtra(MessageThreadActivity.EXTRA_OTHER_NAME,
                nameView.getText() == null ? "" : nameView.getText().toString());
        startActivity(intent);
    }

    private void openStudentProfile() {
        Intent intent = new Intent(this, StudentProfileActivity.class);
        intent.putExtra(StudentProfileActivity.EXTRA_STUDENT_ID, studentId);
        intent.putExtra(StudentProfileActivity.EXTRA_APPOINTMENT_ID, appointmentId);
        intent.putExtra(StudentProfileActivity.EXTRA_COUNSELOR_ID, counselorId);
        intent.putExtra(StudentProfileActivity.EXTRA_APPOINTMENT_DATE, date);
        intent.putExtra(StudentProfileActivity.EXTRA_APPOINTMENT_TIME, time);
        startActivity(intent);
    }

    private void openCrisisDialog() {
        CrisisEscalationDialogFragment
                .newInstance(appointmentId, counselorId, studentId)
                .show(getSupportFragmentManager(), "crisis_escalation");
    }

    /** Called by CrisisEscalationDialogFragment on successful save. */
    @Override
    public void onEscalationSaved(String escalationId) {
        crisisEscalationId = escalationId;
        applyCrisisFiledState();
    }

    private void applyCrisisFiledState() {
        btnCrisis.setEnabled(false);
        btnCrisis.setText(R.string.crisis_escalation_filed);
        btnCrisis.setTextColor(Color.parseColor("#999999"));
        btnCrisis.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#DDDDDD")));
        btnCrisis.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F8F8F8")));
    }

    private void confirmNoShow() {
        showConfirmDialog(
                R.drawable.ic_sleeping,
                Color.parseColor("#FFF3E8"),
                Color.parseColor("#E8761A"),
                getString(R.string.mark_no_show),
                getString(R.string.noshow_too_early_body).isEmpty()
                        ? "This session will be recorded as a no-show." : getString(R.string.mark_no_show),
                getString(R.string.mark_no_show),
                Color.parseColor("#E8761A"),
                () -> doMarkNoShow());
    }

    private void doMarkNoShow() {
        appointmentRepository.markNoShowWithFollowUp(appointmentId,
                new AppointmentRepository.OnStatusUpdateCallback() {
                    @Override public void onSuccess() {
                        AppToast.show(ManageSessionActivity.this,
                                R.string.no_show_followup_created, AppToast.LENGTH_SHORT);
                        finish();
                    }
                    @Override public void onFailure(Exception e) {
                        AppToast.show(ManageSessionActivity.this,
                                R.string.error_update_status, AppToast.LENGTH_SHORT);
                    }
                });
    }

    private void confirmMarkAttended(MaterialButton btn) {
        showConfirmDialog(
                R.drawable.ic_nav_calendar,
                Color.parseColor("#E8F5E9"),
                Color.parseColor("#388E3C"),
                getString(R.string.mark_attended_confirm_title),
                getString(R.string.mark_attended_confirm_body),
                getString(R.string.mark_attended),
                Color.parseColor("#388E3C"),
                () -> {
                    btn.setEnabled(false);
                    appointmentRepository.updateAppointmentStatus(appointmentId, "COMPLETED",
                            new AppointmentRepository.OnStatusUpdateCallback() {
                                @Override public void onSuccess() {
                                    SessionCache.getInstance().invalidateAppointments();
                                    SessionCache.getInstance().invalidateCounselorAppointments();
                                    AppToast.show(ManageSessionActivity.this,
                                            R.string.appointment_marked_attended,
                                            AppToast.LENGTH_SHORT);
                                    finish();
                                }
                                @Override public void onFailure(Exception e) {
                                    btn.setEnabled(true);
                                    AppToast.show(ManageSessionActivity.this,
                                            R.string.error_marking_attended,
                                            AppToast.LENGTH_SHORT);
                                }
                            });
                });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void applyStatusBadge(TextView badge, String s) {
        if (s == null) return;
        String label;
        int textColor;
        int bgColor;
        switch (s) {
            case "CONFIRMED":
                label = getString(R.string.status_confirmed);
                textColor = Color.parseColor("#3D5AF1");
                bgColor   = Color.parseColor("#EDE7F6");
                break;
            case "COMPLETED":
                label = getString(R.string.status_completed);
                textColor = Color.parseColor("#388E3C");
                bgColor   = Color.parseColor("#E8F5E9");
                break;
            case "CANCELLED":
                label = getString(R.string.status_cancelled);
                textColor = Color.parseColor("#D32F2F");
                bgColor   = Color.parseColor("#FFEBEE");
                break;
            case "NO_SHOW":
                label = getString(R.string.status_no_show);
                textColor = Color.parseColor("#E8761A");
                bgColor   = Color.parseColor("#FFF3E8");
                break;
            default:
                label = s;
                textColor = Color.parseColor("#8A8A9A");
                bgColor   = Color.parseColor("#F2F4F8");
                break;
        }
        badge.setText(label);
        badge.setTextColor(textColor);
        ViewCompat.setBackgroundTintList(badge, ColorStateList.valueOf(bgColor));
    }

    private static boolean hasSlotPassed(String date, String time) {
        if (date == null || time == null) return false;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
            sdf.setLenient(false);
            Date slotStart = sdf.parse(date.trim() + " " + time.trim());
            return slotStart != null && System.currentTimeMillis() >= slotStart.getTime();
        } catch (Exception e) {
            return false;
        }
    }

    private static String formatDate(String raw) {
        if (raw == null || raw.isEmpty()) return "—";
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            in.setLenient(false);
            return new SimpleDateFormat("EEE, MMM d", Locale.US).format(in.parse(raw.trim()));
        } catch (Exception ignored) {}
        return raw;
    }

    // ── Dialog helpers ────────────────────────────────────────────────────────

    private void showInfoDialog(int titleRes, int bodyRes) {
        Dialog dialog = buildBaseDialog();
        ImageView icon = dialog.findViewById(R.id.dialogIcon);
        icon.setImageResource(R.drawable.ic_sleeping);
        icon.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFF3E8")));
        icon.setColorFilter(Color.parseColor("#E8761A"));
        ((TextView) dialog.findViewById(R.id.dialogTitle)).setText(titleRes);
        ((TextView) dialog.findViewById(R.id.dialogBody)).setText(bodyRes);
        MaterialButton btnConfirm = dialog.findViewById(R.id.btnConfirm);
        btnConfirm.setText("OK");
        btnConfirm.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E8761A")));
        btnConfirm.setOnClickListener(v -> dialog.dismiss());
        dialog.findViewById(R.id.btnCancel).setVisibility(View.GONE);
        dialog.show();
    }

    private void showConfirmDialog(int iconRes, int iconBg, int iconTint,
                                   String title, String body, String confirmLabel,
                                   int confirmColor, Runnable onConfirm) {
        Dialog dialog = buildBaseDialog();
        ImageView icon = dialog.findViewById(R.id.dialogIcon);
        icon.setImageResource(iconRes);
        icon.setBackgroundTintList(ColorStateList.valueOf(iconBg));
        icon.setColorFilter(iconTint);
        ((TextView) dialog.findViewById(R.id.dialogTitle)).setText(title);
        ((TextView) dialog.findViewById(R.id.dialogBody)).setText(body);
        MaterialButton btnConfirm = dialog.findViewById(R.id.btnConfirm);
        btnConfirm.setText(confirmLabel);
        btnConfirm.setBackgroundTintList(ColorStateList.valueOf(confirmColor));
        btnConfirm.setOnClickListener(v -> { dialog.dismiss(); onConfirm.run(); });
        dialog.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private Dialog buildBaseDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_confirm_action);
        Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawableResource(android.R.color.transparent);
            WindowManager.LayoutParams p = w.getAttributes();
            p.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.88f);
            p.gravity = Gravity.CENTER;
            w.setAttributes(p);
        }
        return dialog;
    }
}
