package com.example.moogerscouncil;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GenerateSlotsActivity extends AppCompatActivity {

    private static final int MAX_BREAKS = 3;

    private CustomCalendarView calendarView;
    private LinearLayout layoutConfig;
    private TextView textSelectedDate;
    private MaterialButton buttonStartTime;
    private MaterialButton buttonEndTime;
    private RadioGroup radioGroupDuration;
    private RecyclerView recyclerBreaks;
    private TextView textNoBreaks;
    private MaterialButton buttonGenerate;
    private MaterialButton buttonAddBreak;
    private FrameLayout progressOverlay;

    private final LinkedHashSet<String> selectedDates = new LinkedHashSet<>();
    private int startMinutes = -1;
    private int endMinutes   = -1;

    private final List<int[]> breaks = new ArrayList<>();
    private BreakAdapter breakAdapter;

    private List<TimeSlot> existingSlots = new ArrayList<>();

    private String counselorId;
    private AvailabilityRepository availabilityRepository;
    private AvailabilitySettingsRepository settingsRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_slots);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        counselorId          = FirebaseAuth.getInstance().getCurrentUser().getUid();
        availabilityRepository = new AvailabilityRepository();
        settingsRepository     = new AvailabilitySettingsRepository();

        calendarView       = findViewById(R.id.calendarView);
        layoutConfig       = findViewById(R.id.layoutConfig);
        textSelectedDate   = findViewById(R.id.textSelectedDate);
        buttonStartTime    = findViewById(R.id.buttonStartTime);
        buttonEndTime      = findViewById(R.id.buttonEndTime);
        radioGroupDuration = findViewById(R.id.radioGroupDuration);
        recyclerBreaks     = findViewById(R.id.recyclerBreaks);
        textNoBreaks       = findViewById(R.id.textNoBreaks);
        buttonGenerate     = findViewById(R.id.buttonGenerate);
        buttonAddBreak     = findViewById(R.id.buttonAddBreak);
        progressOverlay    = findViewById(R.id.progressOverlay);

        breakAdapter = new BreakAdapter(breaks, this::onRemoveBreak);
        recyclerBreaks.setLayoutManager(new LinearLayoutManager(this));
        recyclerBreaks.setAdapter(breakAdapter);
        recyclerBreaks.setNestedScrollingEnabled(false);

        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());
        calendarView.setMinDate(System.currentTimeMillis());
        calendarView.setOnDateClickListener(this::onDateTapped);
        buttonStartTime.setOnClickListener(v -> pickTime(true));
        buttonEndTime.setOnClickListener(v -> pickTime(false));
        buttonAddBreak.setOnClickListener(v -> showAddBreakDialog());
        buttonGenerate.setOnClickListener(v -> onGenerateTapped());

        loadExistingSlotDates();
    }

    // ── Data loading ───────────────────────────────────────────────────────────

    private void loadExistingSlotDates() {
        availabilityRepository.getSlotsForCounselor(counselorId,
                new AvailabilityRepository.OnSlotsLoadedCallback() {
                    @Override
                    public void onSuccess(List<TimeSlot> slots) {
                        existingSlots = slots;
                        HashSet<String> dates = new HashSet<>();
                        for (TimeSlot s : slots) dates.add(s.getDate());
                        calendarView.setHighlightedDates(dates);
                    }
                    @Override
                    public void onFailure(Exception e) {}
                });
    }

    // ── Date selection (multi-select toggle) ───────────────────────────────────

    private void onDateTapped(String date) {
        if (selectedDates.contains(date)) {
            selectedDates.remove(date);
        } else {
            selectedDates.add(date);
        }
        calendarView.setSelectedDates(new HashSet<>(selectedDates));

        if (selectedDates.isEmpty()) {
            layoutConfig.setVisibility(View.GONE);
        } else {
            if (selectedDates.size() == 1) {
                textSelectedDate.setText(formatDate(selectedDates.iterator().next()));
            } else {
                textSelectedDate.setText(selectedDates.size() + " dates selected");
            }
            layoutConfig.setVisibility(View.VISIBLE);
        }
        updateGenerateButton();
    }

    // ── Time pickers ───────────────────────────────────────────────────────────

    private void pickTime(boolean isStart) {
        Calendar now = Calendar.getInstance();
        new TimePickerDialog(this, (view, hour, minute) -> {
            int totalMins = hour * 60 + minute;
            String label = String.format(Locale.US, "%02d:%02d", hour, minute);
            if (isStart) {
                startMinutes = totalMins;
                buttonStartTime.setText(label);
                buttonStartTime.setTextColor(0xFF212121);
            } else {
                endMinutes = totalMins;
                buttonEndTime.setText(label);
                buttonEndTime.setTextColor(0xFF212121);
            }
            updateGenerateButton();
        }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show();
    }

    // ── UI state helpers ───────────────────────────────────────────────────────

    private void updateGenerateButton() {
        // Enable even when start==end so the toast fires on tap
        buttonGenerate.setEnabled(
                !selectedDates.isEmpty() && startMinutes >= 0 && endMinutes >= startMinutes);
    }

    private void updateAddBreakButton() {
        boolean canAdd = breaks.size() < MAX_BREAKS;
        buttonAddBreak.setEnabled(canAdd);
        buttonAddBreak.setAlpha(canAdd ? 1f : 0.4f);
    }

    // ── Add break dialog ───────────────────────────────────────────────────────

    private void showAddBreakDialog() {
        final int[] breakStart = {-1};
        final int[] breakEnd   = {-1};

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_break, null);
        MaterialButton btnStart = view.findViewById(R.id.btnBreakStart);
        MaterialButton btnEnd   = view.findViewById(R.id.btnBreakEnd);

        Calendar now = Calendar.getInstance();

        btnStart.setOnClickListener(v -> new TimePickerDialog(this, (tp, h, m) -> {
            breakStart[0] = h * 60 + m;
            btnStart.setText(String.format(Locale.US, "%02d:%02d", h, m));
            btnStart.setTextColor(0xFF212121);
        }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show());

        btnEnd.setOnClickListener(v -> new TimePickerDialog(this, (tp, h, m) -> {
            breakEnd[0] = h * 60 + m;
            btnEnd.setText(String.format(Locale.US, "%02d:%02d", h, m));
            btnEnd.setTextColor(0xFF212121);
        }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Add Break")
                .setView(view)
                .setPositiveButton("Add", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    if (breakStart[0] < 0 || breakEnd[0] < 0) {
                        AppToast.show(this, "Select both start and end times",
                                AppToast.LENGTH_SHORT);
                        return;
                    }
                    if (breakEnd[0] <= breakStart[0]) {
                        AppToast.show(this, "Break end must be after start",
                                AppToast.LENGTH_SHORT);
                        return;
                    }
                    // Break must fall within work hours (validated only when hours are set)
                    if (startMinutes >= 0 && endMinutes > startMinutes) {
                        if (breakStart[0] < startMinutes || breakEnd[0] > endMinutes) {
                            AppToast.show(this,
                                    "Break must be within work hours ("
                                    + minsToLabel(startMinutes) + " – "
                                    + minsToLabel(endMinutes) + ")",
                                    AppToast.LENGTH_LONG);
                            return;
                        }
                    }
                    // Break must not overlap any existing break
                    for (int[] existing : breaks) {
                        if (breakStart[0] < existing[1] && breakEnd[0] > existing[0]) {
                            AppToast.show(this, "Break overlaps with an existing break",
                                    AppToast.LENGTH_SHORT);
                            return;
                        }
                    }
                    breaks.add(new int[]{breakStart[0], breakEnd[0]});
                    Collections.sort(breaks, (a, b) -> a[0] - b[0]);
                    breakAdapter.notifyDataSetChanged();
                    textNoBreaks.setVisibility(View.GONE);
                    recyclerBreaks.setVisibility(View.VISIBLE);
                    updateAddBreakButton();
                    dialog.dismiss();
                }));

        dialog.show();
    }

    private void onRemoveBreak(int position) {
        breaks.remove(position);
        breakAdapter.notifyItemRemoved(position);
        if (breaks.isEmpty()) {
            textNoBreaks.setVisibility(View.VISIBLE);
            recyclerBreaks.setVisibility(View.GONE);
        }
        updateAddBreakButton();
    }

    // ── Generate ───────────────────────────────────────────────────────────────

    private void onGenerateTapped() {
        if (selectedDates.isEmpty() || startMinutes < 0) return;

        if (startMinutes == endMinutes) {
            AppToast.show(this, "Start and end time cannot be the same", AppToast.LENGTH_SHORT);
            return;
        }
        if (endMinutes < startMinutes) {
            AppToast.show(this, "End time must be after start time", AppToast.LENGTH_SHORT);
            return;
        }

        int duration = getSelectedDuration();
        progressOverlay.setVisibility(View.VISIBLE);
        buttonGenerate.setEnabled(false);

        settingsRepository.getSettings(counselorId,
                new AvailabilitySettingsRepository.OnSettingsLoadedCallback() {
                    @Override
                    public void onSuccess(AvailabilitySettings settings) {
                        progressOverlay.setVisibility(View.GONE);
                        updateGenerateButton();
                        computeAndConfirm(duration, settings.getBufferMinutes());
                    }
                    @Override
                    public void onFailure(Exception e) {
                        progressOverlay.setVisibility(View.GONE);
                        updateGenerateButton();
                        computeAndConfirm(duration, 0);
                    }
                });
    }

    private void computeAndConfirm(int duration, int bufferMins) {
        List<int[]> sortedBreaks = new ArrayList<>(breaks);
        Collections.sort(sortedBreaks, (a, b) -> a[0] - b[0]);

        Map<String, List<String>> slotsPerDate = new HashMap<>();
        int totalSlots = 0;

        for (String date : selectedDates) {
            List<TimeSlot> existingForDate = new ArrayList<>();
            for (TimeSlot ts : existingSlots) {
                if (date.equals(ts.getDate())) existingForDate.add(ts);
            }
            List<String> times = computeSlotTimesForDate(
                    startMinutes, endMinutes, duration, sortedBreaks, existingForDate, bufferMins);
            if (!times.isEmpty()) {
                slotsPerDate.put(date, times);
                totalSlots += times.size();
            }
        }

        if (slotsPerDate.isEmpty()) {
            AppToast.show(this, "No new slots fit within the selected hours and breaks",
                    AppToast.LENGTH_LONG);
            return;
        }

        showGenerateConfirmDialog(slotsPerDate, totalSlots);
    }

    static List<String> computeSlotTimesForDate(int start, int end, int duration,
            List<int[]> sortedBreaks, List<TimeSlot> existingForDate, int bufferMins) {
        // Build blocked intervals: [existingStart, existingStart + duration + bufferMins)
        List<int[]> blocked = new ArrayList<>();
        for (TimeSlot ts : existingForDate) {
            int[] parts = parseTime(ts.getTime());
            if (parts == null) continue;
            int slotStart = parts[0] * 60 + parts[1];
            blocked.add(new int[]{slotStart, slotStart + duration + bufferMins});
        }

        List<String> times = new ArrayList<>();
        int current = start;
        while (current + duration <= end) {
            int slotEnd = current + duration;

            // Jump past any break that overlaps this candidate slot
            int[] overlappingBreak = null;
            for (int[] br : sortedBreaks) {
                if (current < br[1] && slotEnd > br[0]) {
                    overlappingBreak = br;
                    break;
                }
            }
            if (overlappingBreak != null) {
                current = overlappingBreak[1];
                continue;
            }

            // Skip candidate if it conflicts with an already-existing slot (+buffer)
            int[] conflict = null;
            for (int[] blk : blocked) {
                if (current < blk[1] && slotEnd > blk[0]) {
                    conflict = blk;
                    break;
                }
            }
            if (conflict != null) {
                current = conflict[1];
                continue;
            }

            times.add(String.format(Locale.US, "%02d:%02d", current / 60, current % 60));
            current += duration;
        }
        return times;
    }

    // ── Confirmation dialog ────────────────────────────────────────────────────

    private void showGenerateConfirmDialog(Map<String, List<String>> slotsPerDate,
                                           int totalSlots) {
        android.app.Dialog dialog = new android.app.Dialog(this);
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

        ImageView icon = dialog.findViewById(R.id.dialogIcon);
        icon.setImageResource(R.drawable.ic_nav_calendar);
        icon.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFF0F5")));
        icon.setColorFilter(Color.parseColor("#C96B8E"));

        ((TextView) dialog.findViewById(R.id.dialogTitle)).setText("Generate Slots?");

        String bodyText;
        if (slotsPerDate.size() == 1) {
            String date = slotsPerDate.keySet().iterator().next();
            List<String> times = slotsPerDate.get(date);
            bodyText = "This will create " + totalSlots
                    + " slot" + (totalSlots == 1 ? "" : "s")
                    + " on " + formatDate(date)
                    + " from " + times.get(0)
                    + " to " + times.get(times.size() - 1) + ".";
        } else {
            bodyText = "This will create " + totalSlots
                    + " slot" + (totalSlots == 1 ? "" : "s")
                    + " across " + slotsPerDate.size() + " days.";
        }
        ((TextView) dialog.findViewById(R.id.dialogBody)).setText(bodyText);

        MaterialButton btnConfirm = dialog.findViewById(R.id.btnConfirm);
        btnConfirm.setText("Generate");
        btnConfirm.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#C96B8E")));
        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            doGenerateSlots(slotsPerDate, totalSlots);
        });
        dialog.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void doGenerateSlots(Map<String, List<String>> slotsPerDate, int totalSlots) {
        progressOverlay.setVisibility(View.VISIBLE);
        buttonGenerate.setEnabled(false);

        availabilityRepository.addSlotsBatchMultiDay(counselorId, slotsPerDate,
                new AvailabilityRepository.OnSlotActionCallback() {
                    @Override
                    public void onSuccess() {
                        SessionCache.getInstance().invalidateSlots(counselorId);
                        AppToast.show(GenerateSlotsActivity.this,
                                totalSlots + " slot" + (totalSlots == 1 ? "" : "s") + " created",
                                AppToast.LENGTH_SHORT);
                        finish();
                    }
                    @Override
                    public void onFailure(Exception e) {
                        progressOverlay.setVisibility(View.GONE);
                        updateGenerateButton();
                        AppToast.show(GenerateSlotsActivity.this,
                                "Failed to create slots", AppToast.LENGTH_SHORT);
                    }
                });
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private int getSelectedDuration() {
        int checked = radioGroupDuration.getCheckedRadioButtonId();
        if (checked == R.id.radio45) return 45;
        if (checked == R.id.radio60) return 60;
        return 30;
    }

    static int[] parseTime(String time) {
        if (time == null) return null;
        String[] parts = time.split(":");
        if (parts.length != 2) return null;
        try {
            return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String minsToLabel(int mins) {
        return String.format(Locale.US, "%02d:%02d", mins / 60, mins % 60);
    }

    private String formatDate(String raw) {
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(raw);
            return new SimpleDateFormat("EEE, MMM d", Locale.US).format(d);
        } catch (ParseException e) {
            return raw;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Break list adapter
    // ─────────────────────────────────────────────────────────────────────────

    interface OnRemoveListener {
        void onRemove(int position);
    }

    private static class BreakAdapter extends RecyclerView.Adapter<BreakAdapter.BreakVH> {

        private final List<int[]> list;
        private final OnRemoveListener listener;

        BreakAdapter(List<int[]> list, OnRemoveListener listener) {
            this.list     = list;
            this.listener = listener;
        }

        @NonNull @Override
        public BreakVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_break, parent, false);
            return new BreakVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull BreakVH holder, int position) {
            int[] b = list.get(position);
            holder.textTime.setText(String.format(Locale.US, "%02d:%02d – %02d:%02d",
                    b[0] / 60, b[0] % 60, b[1] / 60, b[1] % 60));
            holder.btnRemove.setOnClickListener(v -> {
                int pos = holder.getBindingAdapterPosition();
                if (pos >= 0) listener.onRemove(pos);
            });
        }

        @Override public int getItemCount() { return list.size(); }

        static class BreakVH extends RecyclerView.ViewHolder {
            final TextView    textTime;
            final ImageButton btnRemove;
            BreakVH(View v) {
                super(v);
                textTime  = v.findViewById(R.id.textBreakTime);
                btnRemove = v.findViewById(R.id.buttonRemoveBreak);
            }
        }
    }
}
