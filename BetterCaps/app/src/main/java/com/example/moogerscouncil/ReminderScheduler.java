package com.example.moogerscouncil;

import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Prototype-safe reminder scheduler.
 * Creates in-app reminder records when called; it does not run in the background.
 */
public class ReminderScheduler {
    static final String TYPE_24_HOUR = "24_HOUR";
    static final String TYPE_1_HOUR = "1_HOUR";

    private static final long ONE_HOUR_MS = 60L * 60L * 1000L;
    private static final long TWENTY_FOUR_HOURS_MS = 24L * ONE_HOUR_MS;
    private static final long WINDOW_MS = 10L * 60L * 1000L;

    private final ReminderRepository reminderRepository;
    private final AppointmentRepository appointmentRepository;

    public ReminderScheduler() {
        this(new ReminderRepository(), new AppointmentRepository());
    }

    ReminderScheduler(ReminderRepository reminderRepository,
                      AppointmentRepository appointmentRepository) {
        this.reminderRepository = reminderRepository;
        this.appointmentRepository = appointmentRepository;
    }

    public interface OnSchedulerCallback {
        void onSuccess(int recordsCreated);
        void onFailure(Exception e);
    }

    public void generateReminderRecords(OnSchedulerCallback callback) {
        reminderRepository.getSettings(new ReminderRepository.OnSettingsLoadedCallback() {
            @Override
            public void onSuccess(ReminderSettings settings) {
                appointmentRepository.getUpcomingConfirmedAppointments(
                        new AppointmentRepository.OnAppointmentsLoadedCallback() {
                            @Override
                            public void onSuccess(List<Appointment> appointments) {
                                createRecords(settings, appointments, callback);
                            }

                            @Override
                            public void onFailure(Exception e) {
                                callback.onFailure(e);
                            }
                        });
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    private void createRecords(ReminderSettings settings, List<Appointment> appointments,
                               OnSchedulerCallback callback) {
        if (appointments == null || appointments.isEmpty()) {
            callback.onSuccess(0);
            return;
        }

        int[] pending = {0};
        int[] created = {0};
        boolean[] failed = {false};
        long now = System.currentTimeMillis();

        for (Appointment appointment : appointments) {
            Date startsAt = parseStartsAt(appointment);
            if (startsAt == null) continue;
            if (settings.isEnabled24Hour()
                    && shouldCreateReminder(now, startsAt.getTime(), TWENTY_FOUR_HOURS_MS)) {
                pending[0]++;
                createRecord(appointment, TYPE_24_HOUR, settings.getMessage24Hour(),
                        startsAt.getTime() - TWENTY_FOUR_HOURS_MS, pending, created, failed, callback);
            }
            if (settings.isEnabled1Hour()
                    && shouldCreateReminder(now, startsAt.getTime(), ONE_HOUR_MS)) {
                pending[0]++;
                createRecord(appointment, TYPE_1_HOUR, settings.getMessage1Hour(),
                        startsAt.getTime() - ONE_HOUR_MS, pending, created, failed, callback);
            }
        }

        if (pending[0] == 0) {
            callback.onSuccess(0);
        }
    }

    static boolean shouldCreateReminder(long nowMs, long appointmentStartMs, long offsetMs) {
        long scheduledMs = appointmentStartMs - offsetMs;
        // Valid window: from the scheduled reminder time up until the session itself starts.
        // This means "we are inside the reminder period" — idempotency is handled by
        // createReminderRecordIfMissing, so multiple admin button taps won't duplicate.
        return nowMs >= scheduledMs && nowMs < appointmentStartMs;
    }

    private void createRecord(Appointment appointment, String type, String message,
                              long scheduledMs, int[] pending, int[] created,
                              boolean[] failed, OnSchedulerCallback callback) {
        reminderRepository.createReminderRecordIfMissing(
                appointment,
                type,
                message,
                new Timestamp(new Date(scheduledMs)),
                new ReminderRepository.OnReminderRecordCallback() {
                    @Override
                    public void onCreated(boolean wasCreated) {
                        if (wasCreated) created[0]++;
                        finishOne(pending, created, failed, callback);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        if (!failed[0]) {
                            failed[0] = true;
                            callback.onFailure(e);
                        }
                    }
                });
    }

    private void finishOne(int[] pending, int[] created, boolean[] failed,
                           OnSchedulerCallback callback) {
        pending[0]--;
        if (pending[0] == 0 && !failed[0]) {
            callback.onSuccess(created[0]);
        }
    }

    private static Date parseStartsAt(Appointment appointment) {
        if (appointment == null || appointment.getDate() == null || appointment.getTime() == null) {
            return null;
        }
        String normalized = normalizeTime(appointment.getTime());
        if (normalized == null) return null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
            sdf.setLenient(false);
            return sdf.parse(appointment.getDate().trim() + " " + normalized);
        } catch (Exception e) {
            return null;
        }
    }

    private static String normalizeTime(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        String trimmed = raw.trim();
        if (trimmed.matches("\\d{1,2}:\\d{2}")) {
            String[] parts = trimmed.split(":");
            return String.format(Locale.US, "%02d:%02d",
                    Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        }
        try {
            SimpleDateFormat input = new SimpleDateFormat("h:mm a", Locale.US);
            input.setLenient(false);
            return new SimpleDateFormat("HH:mm", Locale.US).format(input.parse(trimmed));
        } catch (Exception e) {
            return null;
        }
    }
}
