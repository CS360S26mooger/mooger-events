package com.example.moogerscouncil;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Full-screen session note editor.
 * Four labelled section cards replace the old single-textarea popup.
 * Opened from both the dashboard appointment card and the student profile screen.
 * Handles create and upsert transparently — checks Firestore for an existing note on open.
 */
public class SessionNoteActivity extends AppCompatActivity {

    public static final String EXTRA_APPOINTMENT_ID   = "APPOINTMENT_ID";
    public static final String EXTRA_COUNSELOR_ID     = "COUNSELOR_ID";
    public static final String EXTRA_STUDENT_ID       = "STUDENT_ID";
    public static final String EXTRA_APPOINTMENT_DATE = "APPOINTMENT_DATE";
    public static final String EXTRA_APPOINTMENT_TIME = "APPOINTMENT_TIME";

    private static final String SEC_CONCERN       = "[CONCERN]";
    private static final String SEC_SUMMARY       = "[SUMMARY]";
    private static final String SEC_INTERVENTIONS = "[INTERVENTIONS]";
    private static final String SEC_PLAN          = "[PLAN]";

    private TextInputEditText editConcern, editSummary, editInterventions, editPlan;
    private ImageButton buttonDelete;
    private String appointmentId, counselorId, studentId;
    private String appointmentDate, appointmentTime;
    private String existingNoteId;
    private String selectedTemplateKey = NoteTemplate.GENERAL_SESSION;

    /** Full factory — pass appointment date + time so the saved note shows session context. */
    public static Intent newIntent(Context ctx, String appointmentId, String counselorId,
                                   String studentId, String date, String time) {
        return new Intent(ctx, SessionNoteActivity.class)
                .putExtra(EXTRA_APPOINTMENT_ID, appointmentId)
                .putExtra(EXTRA_COUNSELOR_ID, counselorId)
                .putExtra(EXTRA_STUDENT_ID, studentId)
                .putExtra(EXTRA_APPOINTMENT_DATE, date)
                .putExtra(EXTRA_APPOINTMENT_TIME, time);
    }

    /** Overload for callers that don't have appointment date/time handy. */
    public static Intent newIntent(Context ctx,
                                   String appointmentId, String counselorId, String studentId) {
        return newIntent(ctx, appointmentId, counselorId, studentId, null, null);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_note);

        appointmentId   = getIntent().getStringExtra(EXTRA_APPOINTMENT_ID);
        counselorId     = getIntent().getStringExtra(EXTRA_COUNSELOR_ID);
        studentId       = getIntent().getStringExtra(EXTRA_STUDENT_ID);
        appointmentDate = getIntent().getStringExtra(EXTRA_APPOINTMENT_DATE);
        appointmentTime = getIntent().getStringExtra(EXTRA_APPOINTMENT_TIME);

        ((ImageButton) findViewById(R.id.buttonBack)).setOnClickListener(v -> finish());

        buttonDelete = findViewById(R.id.buttonDeleteNote);
        buttonDelete.setOnClickListener(v -> confirmDelete());

        editConcern       = findViewById(R.id.editNoteConcern);
        editSummary       = findViewById(R.id.editNoteSummary);
        editInterventions = findViewById(R.id.editNoteInterventions);
        editPlan          = findViewById(R.id.editNotePlan);

        buildTemplateChips();

        ((MaterialButton) findViewById(R.id.buttonSaveNote))
                .setOnClickListener(v -> saveNote());

        // Pre-load any existing note for this appointment (upsert support)
        if (appointmentId != null && counselorId != null && studentId != null) {
            new SessionNoteRepository().getNoteForAppointment(counselorId, studentId, appointmentId,
                    new SessionNoteRepository.OnSingleNoteCallback() {
                        @Override
                        public void onSuccess(SessionNote note) {
                            if (note != null) {
                                existingNoteId = note.getId();
                                populateFromNote(note);
                                buttonDelete.setVisibility(View.VISIBLE);
                            }
                        }
                        @Override
                        public void onFailure(Exception e) { /* silent — allow fresh note */ }
                    });
        }
    }

    private void confirmDelete() {
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
        ((ImageView) dialog.findViewById(R.id.dialogIcon))
                .setImageResource(R.drawable.ic_trash);
        ((ImageView) dialog.findViewById(R.id.dialogIcon))
                .setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFF0F5")));
        ((ImageView) dialog.findViewById(R.id.dialogIcon))
                .setColorFilter(Color.parseColor("#C96B8E"));
        ((TextView) dialog.findViewById(R.id.dialogTitle)).setText("Delete note");
        ((TextView) dialog.findViewById(R.id.dialogBody))
                .setText("This session note will be permanently deleted. This cannot be undone.");
        com.google.android.material.button.MaterialButton btnConfirm =
                dialog.findViewById(R.id.btnConfirm);
        btnConfirm.setText("Yes, delete");
        btnConfirm.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#D32F2F")));
        btnConfirm.setOnClickListener(v -> { dialog.dismiss(); deleteNote(); });
        dialog.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void deleteNote() {
        if (existingNoteId == null) return;
        new SessionNoteRepository().deleteNote(counselorId, studentId, existingNoteId,
                new SessionNoteRepository.OnNoteActionCallback() {
                    @Override
                    public void onSuccess(String noteId) {
                        AppToast.show(SessionNoteActivity.this,
                                R.string.note_deleted, AppToast.LENGTH_SHORT);
                        finish();
                    }
                    @Override
                    public void onFailure(Exception e) {
                        AppToast.show(SessionNoteActivity.this,
                                R.string.note_save_error, AppToast.LENGTH_LONG);
                    }
                });
    }

    private void buildTemplateChips() {
        ChipGroup chips = findViewById(R.id.chipGroupNoteTemplates);
        ColorStateList chipBg = new ColorStateList(
                new int[][] { new int[] { android.R.attr.state_checked }, new int[] {} },
                new int[] { Color.parseColor("#FFE8F5"), Color.WHITE });
        ColorStateList chipText = new ColorStateList(
                new int[][] { new int[] { android.R.attr.state_checked }, new int[] {} },
                new int[] { Color.parseColor("#C96B8E"), Color.parseColor("#8A8A9A") });
        ColorStateList chipStroke = ColorStateList.valueOf(Color.parseColor("#F0C8DC"));

        for (String key : NoteTemplate.ALL_KEYS) {
            Chip chip = new Chip(this);
            chip.setText(NoteTemplate.getDisplayName(key));
            chip.setCheckable(true);
            chip.setChecked(key.equals(selectedTemplateKey));
            chip.setChipBackgroundColor(chipBg);
            chip.setTextColor(chipText);
            chip.setChipStrokeColor(chipStroke);
            chip.setChipStrokeWidth(getResources().getDisplayMetrics().density * 1.2f);
            chip.setRippleColor(ColorStateList.valueOf(Color.parseColor("#FFE8F5")));
            chip.setOnClickListener(v -> applyTemplate(key));
            chips.addView(chip);
        }
    }

    private void applyTemplate(String key) {
        selectedTemplateKey = key;
        editConcern.setText(NoteTemplate.getConcernText(key));
        editSummary.setText(NoteTemplate.getSummaryText(key));
        editInterventions.setText(NoteTemplate.getInterventionsText(key));
        editPlan.setText(NoteTemplate.getPlanText(key));
    }

    private void populateFromNote(SessionNote note) {
        if (note.getTemplateKey() != null) selectedTemplateKey = note.getTemplateKey();
        String text = note.getNoteText();
        if (text == null) return;
        if (text.contains(SEC_CONCERN)) {
            editConcern.setText(extractSection(text, SEC_CONCERN, SEC_SUMMARY));
            editSummary.setText(extractSection(text, SEC_SUMMARY, SEC_INTERVENTIONS));
            editInterventions.setText(extractSection(text, SEC_INTERVENTIONS, SEC_PLAN));
            editPlan.setText(extractSection(text, SEC_PLAN, null));
        } else {
            // Legacy single-textarea note — display in the summary field
            editSummary.setText(text);
        }
    }

    private String extractSection(String text, String marker, String nextMarker) {
        int start = text.indexOf(marker);
        if (start < 0) return "";
        start += marker.length();
        if (start < text.length() && text.charAt(start) == '\n') start++;
        int end = nextMarker != null ? text.indexOf(nextMarker, start) : text.length();
        if (end < 0) end = text.length();
        String section = text.substring(start, end);
        if (section.endsWith("\n\n")) section = section.substring(0, section.length() - 2);
        return section.trim();
    }

    private void saveNote() {
        String concern       = text(editConcern);
        String summary       = text(editSummary);
        String interventions = text(editInterventions);
        String plan          = text(editPlan);

        if (concern.isEmpty() && summary.isEmpty() && interventions.isEmpty() && plan.isEmpty()) {
            AppToast.show(this, R.string.note_empty_error, AppToast.LENGTH_SHORT);
            return;
        }

        String combined = buildNoteText(concern, summary, interventions, plan);
        SessionNoteRepository repo = new SessionNoteRepository();
        SessionNoteRepository.OnNoteActionCallback cb = new SessionNoteRepository.OnNoteActionCallback() {
            @Override
            public void onSuccess(String noteId) {
                AppToast.show(SessionNoteActivity.this,
                        existingNoteId != null ? R.string.note_updated : R.string.note_saved,
                        AppToast.LENGTH_SHORT);
                finish();
            }
            @Override
            public void onFailure(Exception e) {
                AppToast.show(SessionNoteActivity.this, R.string.note_save_error, AppToast.LENGTH_LONG);
            }
        };

        if (existingNoteId != null) {
            repo.updateNote(counselorId, studentId, existingNoteId, combined, selectedTemplateKey, cb);
        } else {
            SessionNote note = new SessionNote(
                    appointmentId, counselorId, studentId, selectedTemplateKey, combined);
            note.setAppointmentDate(appointmentDate);
            note.setAppointmentTime(appointmentTime);
            repo.saveNote(note, cb);
        }
    }

    private String buildNoteText(String concern, String summary,
                                  String interventions, String plan) {
        StringBuilder sb = new StringBuilder();
        if (!concern.isEmpty())       appendSection(sb, SEC_CONCERN, concern);
        if (!summary.isEmpty())       appendSection(sb, SEC_SUMMARY, summary);
        if (!interventions.isEmpty()) appendSection(sb, SEC_INTERVENTIONS, interventions);
        if (!plan.isEmpty())          appendSection(sb, SEC_PLAN, plan);
        return sb.toString().trim();
    }

    private void appendSection(StringBuilder sb, String marker, String value) {
        sb.append(marker).append('\n').append(value).append("\n\n");
    }

    private String text(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }
}
