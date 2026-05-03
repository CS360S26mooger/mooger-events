package com.example.moogerscouncil;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

/**
 * Counselor-facing student profile with latest intake and workflow actions.
 */
public class StudentProfileActivity extends AppCompatActivity {

    public static final String EXTRA_STUDENT_ID = "STUDENT_ID";
    public static final String EXTRA_APPOINTMENT_ID = "APPOINTMENT_ID";
    public static final String EXTRA_COUNSELOR_ID = "COUNSELOR_ID";

    private String studentId;
    private String appointmentId;
    private String counselorId;
    private TextView textStudentName;
    private TextView textPreferredName;
    private TextView textPronouns;
    private TextView textPrimaryConcern;
    private TextView textDuration;
    private TextView textSupportType;
    private TextView textUrgency;
    private TextView textIntakeEmpty;
    private TextView textSessionNotes;
    private ChipGroup chipIntakeTags;
    private String selectedTemplateKey = NoteTemplate.GENERAL_SESSION;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_profile);

        studentId = getIntent().getStringExtra(EXTRA_STUDENT_ID);
        appointmentId = getIntent().getStringExtra(EXTRA_APPOINTMENT_ID);
        counselorId = getIntent().getStringExtra(EXTRA_COUNSELOR_ID);

        if (studentId == null) {
            AppToast.show(this, R.string.error_loading_profile, AppToast.LENGTH_SHORT);
            finish();
            return;
        }

        ((ImageButton) findViewById(R.id.buttonBack)).setOnClickListener(v -> finish());
        textStudentName = findViewById(R.id.textStudentProfileName);
        textPreferredName = findViewById(R.id.textStudentPreferredName);
        textPronouns = findViewById(R.id.textStudentPronouns);
        textPrimaryConcern = findViewById(R.id.textIntakePrimaryConcern);
        textDuration = findViewById(R.id.textIntakeDuration);
        textSupportType = findViewById(R.id.textIntakeSupportType);
        textUrgency = findViewById(R.id.textIntakeUrgency);
        textIntakeEmpty = findViewById(R.id.textIntakeEmpty);
        textSessionNotes = findViewById(R.id.textSessionNotes);
        chipIntakeTags = findViewById(R.id.chipIntakeTags);

        findViewById(R.id.buttonViewHistory).setOnClickListener(v -> {
            Intent intent = new Intent(this, SessionHistoryActivity.class);
            intent.putExtra(EXTRA_STUDENT_ID, studentId);
            startActivity(intent);
        });
        findViewById(R.id.buttonAddSessionNote).setOnClickListener(v -> showNoteDialog());
        findViewById(R.id.buttonTriggerCrisis).setOnClickListener(v ->
                CrisisEscalationDialogFragment
                        .newInstance(appointmentId, counselorId, studentId)
                        .show(getSupportFragmentManager(), "crisis_escalation"));

        loadStudent();
        loadLatestIntake();
        loadSessionNotes();
    }

    private void loadStudent() {
        new UserRepository().getStudentById(studentId,
                new UserRepository.OnStudentFetchedCallback() {
                    @Override
                    public void onSuccess(Student student) {
                        textStudentName.setText(valueOrDash(student.getName()));
                        textPreferredName.setText(getString(R.string.profile_preferred_name,
                                valueOrDash(student.getPreferredName())));
                        textPronouns.setText(getString(R.string.profile_pronouns,
                                valueOrDash(student.getPronouns())));
                    }

                    @Override
                    public void onFailure(Exception e) {
                        textStudentName.setText(R.string.unknown_student);
                    }
                });
    }

    private void loadLatestIntake() {
        new IntakeAssessmentRepository().getLatestForStudent(studentId,
                new IntakeAssessmentRepository.OnAssessmentLoadedCallback() {
                    @Override
                    public void onSuccess(IntakeAssessment assessment) {
                        textIntakeEmpty.setVisibility(View.GONE);
                        textPrimaryConcern.setText(getString(R.string.intake_primary_concern,
                                valueOrDash(assessment.getPrimaryConcern())));
                        textDuration.setText(getString(R.string.intake_duration,
                                valueOrDash(assessment.getDuration())));
                        textSupportType.setText(getString(R.string.intake_support_type,
                                valueOrDash(assessment.getSupportType())));
                        textUrgency.setText(getString(R.string.intake_urgency,
                                valueOrDash(assessment.getUrgencyLevel())));
                        chipIntakeTags.removeAllViews();
                        if (assessment.getRecommendedSpecializations() != null) {
                            for (String tag : assessment.getRecommendedSpecializations()) {
                                Chip chip = new Chip(StudentProfileActivity.this);
                                chip.setText(tag);
                                chip.setCheckable(false);
                                chipIntakeTags.addView(chip);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        textIntakeEmpty.setVisibility(View.VISIBLE);
                        textIntakeEmpty.setText(R.string.no_intake_found);
                    }
                });
    }

    private void showNoteDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_session_note, null);
        ChipGroup templateChips = dialogView.findViewById(R.id.chipGroupNoteTemplates);
        EditText noteEdit = dialogView.findViewById(R.id.editSessionNote);
        selectedTemplateKey = NoteTemplate.GENERAL_SESSION;
        buildTemplateChips(templateChips, noteEdit);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setNegativeButton(R.string.button_cancel, null)
                .setPositiveButton(R.string.save_note, null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> saveNote(noteEdit, dialog)));
        dialog.show();
    }

    private void buildTemplateChips(ChipGroup templateChips, EditText noteEdit) {
        for (String key : NoteTemplate.ALL_KEYS) {
            Chip chip = new Chip(this);
            chip.setText(NoteTemplate.getDisplayName(key));
            chip.setCheckable(true);
            chip.setOnClickListener(v -> {
                selectedTemplateKey = key;
                noteEdit.setText(NoteTemplate.getTemplateText(key));
                noteEdit.setSelection(noteEdit.getText().length());
            });
            templateChips.addView(chip);
        }
    }

    private void saveNote(EditText noteEdit, AlertDialog dialog) {
        String text = noteEdit.getText() == null ? "" : noteEdit.getText().toString().trim();
        if (text.isEmpty()) {
            AppToast.show(this, R.string.note_text_required, AppToast.LENGTH_SHORT);
            return;
        }
        SessionNote note = new SessionNote(
                appointmentId, counselorId, studentId, selectedTemplateKey, text);
        new SessionNoteRepository().saveNote(note, new SessionNoteRepository.OnNoteActionCallback() {
            @Override
            public void onSuccess(String noteId) {
                AppToast.show(StudentProfileActivity.this, R.string.note_saved,
                        AppToast.LENGTH_SHORT);
                dialog.dismiss();
                loadSessionNotes();
            }

            @Override
            public void onFailure(Exception e) {
                AppToast.show(StudentProfileActivity.this, R.string.note_save_error,
                        AppToast.LENGTH_LONG);
            }
        });
    }

    private String valueOrDash(String value) {
        return value == null || value.isEmpty() ? "-" : value;
    }

    private void loadSessionNotes() {
        if (appointmentId == null || appointmentId.isEmpty()) {
            textSessionNotes.setText(R.string.no_session_notes);
            return;
        }
        new SessionNoteRepository().getNotesForAppointment(appointmentId,
                new SessionNoteRepository.OnNotesLoadedCallback() {
                    @Override
                    public void onSuccess(java.util.List<SessionNote> notes) {
                        if (notes.isEmpty()) {
                            textSessionNotes.setText(R.string.no_session_notes);
                            return;
                        }
                        StringBuilder builder = new StringBuilder();
                        for (SessionNote note : notes) {
                            if (builder.length() > 0) builder.append("\n\n");
                            builder.append(NoteTemplate.getDisplayName(note.getTemplateKey()))
                                    .append("\n")
                                    .append(valueOrDash(note.getNoteText()));
                        }
                        textSessionNotes.setText(builder.toString());
                    }

                    @Override
                    public void onFailure(Exception e) {
                        textSessionNotes.setText(R.string.no_session_notes);
                    }
                });
    }
}
