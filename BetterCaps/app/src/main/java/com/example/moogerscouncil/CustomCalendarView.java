package com.example.moogerscouncil;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Fully custom calendar widget.
 *
 * Supports:
 *  - Month navigation (< / >)
 *  - Today: filled purple circle, white bold text
 *  - Past dates: dim grey text
 *  - Future dates: bold dark text
 *  - Highlighted dates (appointments / available slots): hollow pastel-pink ring
 *  - Selected date: filled pastel-pink circle (overrides highlight if both active, ring still shows)
 *  - setMinDate(long): disables and dims past dates (used on booking screen)
 */
public class CustomCalendarView extends LinearLayout {

    public interface OnDateClickListener {
        void onDateClick(String date); // "yyyy-MM-dd"
    }

    private Calendar displayMonth;
    private Set<String> highlightedDates = new HashSet<>();
    private Set<String> selectedDates    = new HashSet<>(); // multi-select support
    private String selectedDate;
    private String todayStr;
    private long minDateMillis = 0;
    private OnDateClickListener listener;

    private TextView monthYearLabel;
    private LinearLayout gridContainer;

    public CustomCalendarView(Context context) {
        super(context);
        init();
    }

    public CustomCalendarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CustomCalendarView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        setOrientation(VERTICAL);
        setPadding(dp(12), dp(10), dp(12), dp(10));

        displayMonth = Calendar.getInstance();
        todayStr     = toDateStr(displayMonth);
        selectedDate = todayStr;

        buildNavRow();
        buildDayHeaders();

        gridContainer = new LinearLayout(getContext());
        gridContainer.setOrientation(VERTICAL);
        addView(gridContainer, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        rebuildGrid();
    }

    // ── Layout builders ────────────────────────────────────────────────────────

    private void buildNavRow() {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, 0, 0, dp(6));

        TextView prev = navArrow("‹");
        monthYearLabel = new TextView(getContext());
        monthYearLabel.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        monthYearLabel.setGravity(Gravity.CENTER);
        monthYearLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        monthYearLabel.setTypeface(null, Typeface.BOLD);
        monthYearLabel.setTextColor(Color.parseColor("#1A1A2E"));

        TextView next = navArrow("›");

        prev.setOnClickListener(v -> { displayMonth.add(Calendar.MONTH, -1); rebuildGrid(); });
        next.setOnClickListener(v -> { displayMonth.add(Calendar.MONTH, 1);  rebuildGrid(); });

        row.addView(prev);
        row.addView(monthYearLabel);
        row.addView(next);
        addView(row, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(44)));
    }

    private TextView navArrow(String symbol) {
        TextView tv = new TextView(getContext());
        tv.setLayoutParams(new LinearLayout.LayoutParams(dp(40), dp(40)));
        tv.setGravity(Gravity.CENTER);
        tv.setText(symbol);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
        tv.setTextColor(Color.parseColor("#8B6BAE"));
        tv.setTypeface(null, Typeface.BOLD);
        return tv;
    }

    private void buildDayHeaders() {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(HORIZONTAL);
        for (String d : new String[]{"M", "T", "W", "T", "F", "S", "S"}) {
            TextView tv = new TextView(getContext());
            tv.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.MATCH_PARENT, 1f));
            tv.setGravity(Gravity.CENTER);
            tv.setText(d);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            tv.setTextColor(Color.parseColor("#BBBBBB"));
            tv.setTypeface(null, Typeface.BOLD);
            row.addView(tv);
        }
        addView(row, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(28)));
    }

    // ── Grid ───────────────────────────────────────────────────────────────────

    private void rebuildGrid() {
        monthYearLabel.setText(
                new SimpleDateFormat("MMMM yyyy", Locale.US).format(displayMonth.getTime()));

        gridContainer.removeAllViews();

        Calendar cal = (Calendar) displayMonth.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        // Convert Sunday=1..Saturday=7 to Monday-first offset (Mon=0, ..., Sun=6)
        int startOffset = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        int day = 1 - startOffset;
        for (int row = 0; row < 6; row++) {
            if (day > daysInMonth) break;

            LinearLayout rowLayout = new LinearLayout(getContext());
            rowLayout.setOrientation(HORIZONTAL);
            for (int col = 0; col < 7; col++) {
                rowLayout.addView(buildCell(day, daysInMonth),
                        new LinearLayout.LayoutParams(0, dp(44), 1f));
                day++;
            }
            gridContainer.addView(rowLayout,
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, dp(44)));
        }
    }

    private FrameLayout buildCell(int day, int daysInMonth) {
        FrameLayout cell = new FrameLayout(getContext());

        if (day < 1 || day > daysInMonth) return cell; // padding cell

        Calendar dayCal = (Calendar) displayMonth.clone();
        dayCal.set(Calendar.DAY_OF_MONTH, day);
        String dateStr = toDateStr(dayCal);

        boolean isToday       = dateStr.equals(todayStr);
        boolean isHighlight   = highlightedDates.contains(dateStr);
        boolean isSelected    = dateStr.equals(selectedDate) || selectedDates.contains(dateStr);
        boolean isPast        = dateStr.compareTo(todayStr) < 0;
        boolean isDisabled    = minDateMillis > 0 && isPast;

        // Fixed 36×36 dp inner circle so it stays round on all screen sizes
        TextView dayView = new TextView(getContext());
        FrameLayout.LayoutParams innerLp = new FrameLayout.LayoutParams(dp(36), dp(36));
        innerLp.gravity = Gravity.CENTER;
        dayView.setLayoutParams(innerLp);
        dayView.setText(String.valueOf(day));
        dayView.setGravity(Gravity.CENTER);
        dayView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);

        if (isToday) {
            dayView.setTextColor(Color.WHITE);
            dayView.setTypeface(null, Typeface.BOLD);
        } else if (isPast || isDisabled) {
            dayView.setTextColor(Color.parseColor("#C8C8C8"));
            dayView.setTypeface(null, Typeface.NORMAL);
        } else {
            dayView.setTextColor(Color.parseColor("#1A1A2E"));
            dayView.setTypeface(null, Typeface.BOLD);
        }

        dayView.setBackground(buildBg(isToday, isHighlight, isSelected));
        cell.addView(dayView);

        if (!isDisabled) {
            final String ds = dateStr;
            cell.setOnClickListener(v -> {
                selectedDate = ds;
                rebuildGrid();
                if (listener != null) listener.onDateClick(ds);
            });
        }

        return cell;
    }

    // ── Background drawables ───────────────────────────────────────────────────

    private Drawable buildBg(boolean isToday, boolean isHighlight, boolean isSelected) {
        GradientDrawable fill = null;
        if (isToday) {
            fill = solidOval(Color.parseColor("#8B6BAE")); // filled purple
        } else if (isSelected) {
            fill = solidOval(Color.parseColor("#F5E6F0")); // filled pastel pink
        }

        if (!isHighlight) return fill; // may be null → transparent

        // Hollow pink ring
        GradientDrawable ring = ringOval(Color.parseColor("#C96B8E"), dp(2));
        if (fill != null) {
            return new LayerDrawable(new Drawable[]{fill, ring});
        }
        return ring;
    }

    private GradientDrawable solidOval(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(color);
        return d;
    }

    private GradientDrawable ringOval(int strokeColor, int strokePx) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(Color.TRANSPARENT);
        d.setStroke(strokePx, strokeColor);
        return d;
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    /** Marks these dates with a hollow pink ring decoration. */
    public void setHighlightedDates(Set<String> dates) {
        highlightedDates = dates != null ? dates : new HashSet<>();
        rebuildGrid();
    }

    /** Fills these dates with the selection colour (multi-select support). */
    public void setSelectedDates(Set<String> dates) {
        selectedDates = dates != null ? dates : new HashSet<>();
        rebuildGrid();
    }

    public void setOnDateClickListener(OnDateClickListener l) {
        listener = l;
    }

    /** Disables (dims, non-clickable) all dates strictly before today. */
    public void setMinDate(long millis) {
        minDateMillis = millis;
        rebuildGrid();
    }

    /** Returns the currently selected date as "yyyy-MM-dd". */
    public String getSelectedDate() {
        return selectedDate;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private String toDateStr(Calendar cal) {
        return String.format(Locale.US, "%04d-%02d-%02d",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH));
    }

    private int dp(int value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics()));
    }
}
