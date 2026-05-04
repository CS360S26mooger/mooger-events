/*
 * AppointmentAdapter.java
 * Role: RecyclerView adapter for the counselor's appointment list.
 *       Looks up student names via UserRepository, applies status badge styling
 *       (CONFIRMED/COMPLETED/CANCELLED/NO_SHOW), and wires the No-Show button
 *       to AppointmentRepository.updateAppointmentStatus().
 *
 * Design pattern: Adapter pattern (RecyclerView); Repository pattern for data ops.
 * Part of the BetterCAPS counseling platform.
 */
package com.example.moogerscouncil;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for the counselor's appointment dashboard cards.
 *
 * <p>Each card shows:
 * <ul>
 *   <li>Student name (async lookup via {@link UserRepository})</li>
 *   <li>Date and time</li>
 *   <li>Status badge (color-coded: green/grey/red)</li>
 *   <li>No-Show button wired to {@link AppointmentRepository}</li>
 * </ul>
 * </p>
 */
public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.ViewHolder> {

    private List<Appointment> appointments;
    private final Context context;
    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final SecureMessageRepository secureMessageRepository;

    /**
     * Creates a new adapter.
     *
     * @param context      Activity context for inflation and toasts.
     * @param appointments Initial appointment list (may be replaced via {@link #setData}).
     */
    public AppointmentAdapter(Context context, List<Appointment> appointments) {
        this.context = context;
        this.appointments = appointments;
        this.userRepository = new UserRepository();
        this.appointmentRepository = new AppointmentRepository();
        this.secureMessageRepository = new SecureMessageRepository();
    }

    /**
     * Replaces the current appointment list and notifies the adapter.
     * Called by {@link CounselorDashboardActivity} when the tab changes.
     *
     * @param newList The filtered appointment list for the current tab.
     */
    public void setData(List<Appointment> newList) {
        this.appointments = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_appointment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Appointment apt = appointments.get(position);

        // Async student name lookup
        holder.studentName.setText("Loading…");
        if (apt.getStudentId() != null) {
            userRepository.getUserName(apt.getStudentId(),
                    name -> holder.studentName.setText(name));
        } else {
            holder.studentName.setText("Unknown");
        }

        holder.sessionTime.setText(normalizeTime(apt.getTime()));
        holder.sessionDate.setText(formatDate(apt.getDate()));
        holder.returningBadge.setVisibility(apt.isReturningStudent() ? View.VISIBLE : View.GONE);

        // Status badge styling
        applyStatusBadge(holder, apt.getStatus());

        String status = apt.getStatus();
        boolean isActive = "CONFIRMED".equals(status);
        boolean isCancelled = "CANCELLED".equals(status);
        boolean isNoShow = "NO_SHOW".equals(status);
        holder.noShowButton.setVisibility(isActive ? View.VISIBLE : View.GONE);
        holder.notesButton.setVisibility(isCancelled ? View.GONE : View.VISIBLE);
        holder.crisisButton.setVisibility(
                isCancelled || "COMPLETED".equals(status) ? View.GONE : View.VISIBLE);
        boolean canMessage = !isCancelled && !isNoShow && apt.getId() != null
                && apt.getStudentId() != null && apt.getCounselorId() != null;
        holder.messageButton.setVisibility(canMessage ? View.VISIBLE : View.GONE);
        holder.messageButton.setText(R.string.messages);
        if (canMessage) {
            updateUnreadMessageLabel(apt, holder);
        }

        holder.noShowButton.setOnClickListener(v -> {
            if (!isNoShowWindowOpen(apt.getDate(), apt.getTime())) {
                showNoShowTooEarlyDialog();
            } else {
                confirmNoShow(apt, holder, position);
            }
        });
        holder.crisisButton.setOnClickListener(v -> showCrisisDialog(apt));
        holder.profileButton.setOnClickListener(v -> openStudentProfile(apt));
        holder.notesButton.setOnClickListener(v -> showNoteDialog(apt));
        holder.messageButton.setOnClickListener(v -> openMessageThread(apt, holder));
    }

    /**
     * Applies color-coded status badge styling to the appointment card.
     * CONFIRMED = green; COMPLETED = muted grey; CANCELLED / NO_SHOW = red with strikethrough.
     *
     * @param holder The ViewHolder for the current card.
     * @param status The appointment status string.
     */
    private void applyStatusBadge(ViewHolder holder, String status) {
        holder.itemView.setAlpha(1.0f);
        holder.studentName.setPaintFlags(
                holder.studentName.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);

        if (status == null) return;

        String label;
        int textColor;
        int bgColor;
        switch (status) {
            case "CONFIRMED":
                label     = context.getString(R.string.status_confirmed);
                textColor = Color.parseColor("#3D5AF1");
                bgColor   = Color.parseColor("#EDE7F6");
                break;
            case "COMPLETED":
                label     = context.getString(R.string.status_completed);
                textColor = Color.parseColor("#388E3C");
                bgColor   = Color.parseColor("#E8F5E9");
                holder.itemView.setAlpha(0.75f);
                break;
            case "CANCELLED":
                label     = context.getString(R.string.status_cancelled);
                textColor = Color.parseColor("#D32F2F");
                bgColor   = Color.parseColor("#FFEBEE");
                holder.studentName.setPaintFlags(
                        holder.studentName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                break;
            case "NO_SHOW":
                label     = context.getString(R.string.status_no_show);
                textColor = Color.parseColor("#E8761A");
                bgColor   = Color.parseColor("#FFF3E8");
                holder.itemView.setAlpha(0.75f);
                break;
            default:
                label     = status;
                textColor = Color.parseColor("#8A8A9A");
                bgColor   = Color.parseColor("#F2F4F8");
                break;
        }
        holder.sessionTopic.setText(label);
        holder.sessionTopic.setTextColor(textColor);
        ViewCompat.setBackgroundTintList(holder.sessionTopic,
                ColorStateList.valueOf(bgColor));
    }

    private void confirmNoShow(Appointment apt, ViewHolder holder, int position) {
        if (!"CONFIRMED".equals(apt.getStatus())) return;
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_confirm_action);
        Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawableResource(android.R.color.transparent);
            WindowManager.LayoutParams p = w.getAttributes();
            p.width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.88f);
            p.gravity = Gravity.CENTER;
            w.setAttributes(p);
        }
        ImageView icon = dialog.findViewById(R.id.dialogIcon);
        icon.setImageResource(R.drawable.ic_sleeping);
        icon.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFF3E8")));
        icon.setColorFilter(Color.parseColor("#E8761A"));
        ((TextView) dialog.findViewById(R.id.dialogTitle)).setText("Mark as No-Show?");
        ((TextView) dialog.findViewById(R.id.dialogBody))
                .setText("This session will be recorded as a no-show and removed from your active list.");
        com.google.android.material.button.MaterialButton btnConfirm =
                dialog.findViewById(R.id.btnConfirm);
        btnConfirm.setText("Yes, mark no-show");
        btnConfirm.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E8761A")));
        btnConfirm.setOnClickListener(v -> { dialog.dismiss(); markNoShow(apt, holder, position); });
        dialog.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showNoShowTooEarlyDialog() {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_confirm_action);
        Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawableResource(android.R.color.transparent);
            WindowManager.LayoutParams p = w.getAttributes();
            p.width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.88f);
            p.gravity = Gravity.CENTER;
            w.setAttributes(p);
        }
        ImageView icon = dialog.findViewById(R.id.dialogIcon);
        icon.setImageResource(R.drawable.ic_sleeping);
        icon.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFF3E8")));
        icon.setColorFilter(Color.parseColor("#E8761A"));
        ((TextView) dialog.findViewById(R.id.dialogTitle)).setText("Too Early");
        ((TextView) dialog.findViewById(R.id.dialogBody))
                .setText("A student can only be marked as a no-show 10 minutes after the session start time");
        com.google.android.material.button.MaterialButton btnConfirm =
                dialog.findViewById(R.id.btnConfirm);
        btnConfirm.setText("OK");
        btnConfirm.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E8761A")));
        btnConfirm.setOnClickListener(v -> dialog.dismiss());
        dialog.findViewById(R.id.btnCancel).setVisibility(View.GONE);
        dialog.show();
    }

    private void markNoShow(Appointment apt, ViewHolder holder, int position) {
        appointmentRepository.markNoShowWithFollowUp(apt.getId(),
                new AppointmentRepository.OnStatusUpdateCallback() {
                    @Override
                    public void onSuccess() {
                        apt.setStatus("NO_SHOW");
                        apt.setNoShowFollowUpRequired(true);
                        apt.setNoShowFollowUpStatus("PENDING");
                        int pos = holder.getBindingAdapterPosition();
                        if (pos != androidx.recyclerview.widget.RecyclerView.NO_ID) {
                            appointments.remove(pos);
                            notifyItemRemoved(pos);
                        }
                        SessionCache.getInstance().invalidateCounselorAppointments();
                        AppToast.show(context, context.getString(R.string.no_show_followup_created),
                                AppToast.LENGTH_SHORT);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        AppToast.show(context, R.string.error_update_status,
                                AppToast.LENGTH_SHORT);
                    }
                });
    }

    private void openStudentProfile(Appointment appointment) {
        Intent intent = new Intent(context, StudentProfileActivity.class);
        intent.putExtra(StudentProfileActivity.EXTRA_STUDENT_ID, appointment.getStudentId());
        intent.putExtra(StudentProfileActivity.EXTRA_APPOINTMENT_ID, appointment.getId());
        intent.putExtra(StudentProfileActivity.EXTRA_COUNSELOR_ID, appointment.getCounselorId());
        intent.putExtra(StudentProfileActivity.EXTRA_APPOINTMENT_DATE, appointment.getDate());
        intent.putExtra(StudentProfileActivity.EXTRA_APPOINTMENT_TIME, appointment.getTime());
        context.startActivity(intent);
    }

    private void openMessageThread(Appointment appointment, ViewHolder holder) {
        Intent intent = new Intent(context, MessageThreadActivity.class);
        intent.putExtra(MessageThreadActivity.EXTRA_APPOINTMENT_ID, appointment.getId());
        intent.putExtra(MessageThreadActivity.EXTRA_STUDENT_ID, appointment.getStudentId());
        intent.putExtra(MessageThreadActivity.EXTRA_COUNSELOR_ID, appointment.getCounselorId());
        intent.putExtra(MessageThreadActivity.EXTRA_OTHER_NAME,
                holder.studentName.getText() == null ? "" : holder.studentName.getText().toString());
        context.startActivity(intent);
    }

    private void updateUnreadMessageLabel(Appointment appointment, ViewHolder holder) {
        String currentUid = FirebaseAuth.getInstance().getCurrentUser() == null
                ? "" : FirebaseAuth.getInstance().getCurrentUser().getUid();
        secureMessageRepository.hasUnreadMessagesForAppointment(
                appointment.getId(),
                currentUid,
                new SecureMessageRepository.OnUnreadStatusCallback() {
                    @Override
                    public void onResult(boolean hasUnread) {
                        if (holder.getBindingAdapterPosition() == RecyclerView.NO_POSITION) return;
                        holder.messageButton.setText(hasUnread
                                ? context.getString(R.string.messages_new)
                                : context.getString(R.string.messages));
                    }

                    @Override
                    public void onFailure(Exception e) {
                        holder.messageButton.setText(R.string.messages);
                    }
                });
    }

    private void showCrisisDialog(Appointment appointment) {
        if (!(context instanceof FragmentActivity)) {
            AppToast.show(context, R.string.crisis_escalation_error, AppToast.LENGTH_SHORT);
            return;
        }
        CrisisEscalationDialogFragment
                .newInstance(appointment.getId(), appointment.getCounselorId(),
                        appointment.getStudentId())
                .show(((FragmentActivity) context).getSupportFragmentManager(),
                        "crisis_escalation");
    }

    /** Launches the full-screen SessionNoteActivity for this appointment. */
    private void showNoteDialog(Appointment appointment) {
        context.startActivity(SessionNoteActivity.newIntent(
                context,
                appointment.getId(),
                appointment.getCounselorId(),
                appointment.getStudentId(),
                appointment.getDate(),
                appointment.getTime()));
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

    /** Normalizes any stored time string to HH:mm (24-hour, zero-padded, no AM/PM). */
    private static String normalizeTime(String raw) {
        if (raw == null || raw.isEmpty()) return "—";
        // Already HH:mm or H:mm 24h — just zero-pad the hour
        if (raw.matches("\\d{1,2}:\\d{2}")) {
            String[] p = raw.split(":");
            return String.format(Locale.US, "%02d:%02d",
                    Integer.parseInt(p[0]), Integer.parseInt(p[1]));
        }
        // 12h with AM/PM e.g. "10:00 AM", "2:00 PM"
        try {
            SimpleDateFormat in = new SimpleDateFormat("h:mm a", Locale.US);
            in.setLenient(false);
            return new SimpleDateFormat("HH:mm", Locale.US).format(in.parse(raw.trim()));
        } catch (Exception ignored) {}
        return raw;
    }

    /**
     * Returns true if the current time is at least 10 minutes past the slot start time.
     * No-show marking is only allowed after this window has elapsed.
     */
    static boolean isNoShowWindowOpen(String date, String time) {
        if (date == null || time == null) return false;
        try {
            String normalized = normalizeTime(time);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
            sdf.setLenient(false);
            java.util.Date slotStart = sdf.parse(date.trim() + " " + normalized);
            if (slotStart == null) return false;
            long tenMinutesMs = 10 * 60 * 1000;
            return System.currentTimeMillis() >= slotStart.getTime() + tenMinutesMs;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public int getItemCount() {
        return appointments.size();
    }

    /** ViewHolder for a single appointment card. */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView studentName, sessionTopic, sessionTime, sessionDate, returningBadge;
        Button noShowButton, crisisButton, profileButton, notesButton, messageButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            studentName = itemView.findViewById(R.id.studentName);
            sessionTopic = itemView.findViewById(R.id.sessionTopic);
            sessionTime = itemView.findViewById(R.id.sessionTime);
            sessionDate = itemView.findViewById(R.id.sessionDate);
            returningBadge = itemView.findViewById(R.id.returningStudentBadge);
            noShowButton = itemView.findViewById(R.id.noShowButton);
            crisisButton = itemView.findViewById(R.id.crisisButton);
            profileButton = itemView.findViewById(R.id.profileButton);
            notesButton = itemView.findViewById(R.id.notesButton);
            messageButton = itemView.findViewById(R.id.btnMessage);
        }
    }
}
