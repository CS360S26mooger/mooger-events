package com.example.moogerscouncil;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Counselor-facing student profile. Shows latest intake assessment and
 * a full timestamped history of all session notes for this student.
 */
public class StudentProfileActivity extends AppCompatActivity {

    public static final String EXTRA_STUDENT_ID       = "STUDENT_ID";
    public static final String EXTRA_APPOINTMENT_ID   = "APPOINTMENT_ID";
    public static final String EXTRA_COUNSELOR_ID     = "COUNSELOR_ID";
    public static final String EXTRA_APPOINTMENT_DATE = "APPOINTMENT_DATE";
    public static final String EXTRA_APPOINTMENT_TIME = "APPOINTMENT_TIME";

    private String studentId;
    private String appointmentId;
    private String counselorId;
    private String appointmentDate;
    private String appointmentTime;

    private TextView textStudentName;
    private TextView textPreferredName;
    private TextView textPronouns;
    private TextView textPrimaryConcern;
    private TextView textDuration;
    private TextView textSupportType;
    private TextView textUrgency;
    private TextView textIntakeEmpty;
    private TextView textNotesEmpty;
    private ChipGroup chipIntakeTags;
    private RecyclerView recyclerSessionNotes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_profile);

        studentId       = getIntent().getStringExtra(EXTRA_STUDENT_ID);
        appointmentId   = getIntent().getStringExtra(EXTRA_APPOINTMENT_ID);
        counselorId     = getIntent().getStringExtra(EXTRA_COUNSELOR_ID);
        appointmentDate = getIntent().getStringExtra(EXTRA_APPOINTMENT_DATE);
        appointmentTime = getIntent().getStringExtra(EXTRA_APPOINTMENT_TIME);

        if (studentId == null) {
            AppToast.show(this, R.string.error_loading_profile, AppToast.LENGTH_SHORT);
            finish();
            return;
        }

        ((ImageButton) findViewById(R.id.buttonBack)).setOnClickListener(v -> finish());

        textStudentName   = findViewById(R.id.textStudentProfileName);
        textPreferredName = findViewById(R.id.textStudentPreferredName);
        textPronouns      = findViewById(R.id.textStudentPronouns);
        textPrimaryConcern = findViewById(R.id.textIntakePrimaryConcern);
        textDuration      = findViewById(R.id.textIntakeDuration);
        textSupportType   = findViewById(R.id.textIntakeSupportType);
        textUrgency       = findViewById(R.id.textIntakeUrgency);
        textIntakeEmpty   = findViewById(R.id.textIntakeEmpty);
        textNotesEmpty    = findViewById(R.id.textNotesEmpty);
        chipIntakeTags    = findViewById(R.id.chipIntakeTags);

        recyclerSessionNotes = findViewById(R.id.recyclerSessionNotes);
        recyclerSessionNotes.setLayoutManager(new LinearLayoutManager(this));
        recyclerSessionNotes.setNestedScrollingEnabled(false);

        findViewById(R.id.buttonViewHistory).setOnClickListener(v -> {
            Intent intent = new Intent(this, SessionHistoryActivity.class);
            intent.putExtra(EXTRA_STUDENT_ID, studentId);
            startActivity(intent);
        });

        findViewById(R.id.buttonAddSessionNote).setOnClickListener(v ->
                startActivity(SessionNoteActivity.newIntent(
                        this, appointmentId, counselorId, studentId,
                        appointmentDate, appointmentTime)));

        findViewById(R.id.buttonTriggerCrisis).setOnClickListener(v ->
                CrisisEscalationDialogFragment
                        .newInstance(appointmentId, counselorId, studentId)
                        .show(getSupportFragmentManager(), "crisis_escalation"));

        loadStudent();
        loadLatestIntake();
    }

    @Override
    protected void onResume() {
        super.onResume();
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
                            ColorStateList tagBg = ColorStateList.valueOf(
                                    Color.parseColor("#FFE8F5"));
                            ColorStateList tagText = ColorStateList.valueOf(
                                    Color.parseColor("#C96B8E"));
                            ColorStateList tagStroke = ColorStateList.valueOf(
                                    Color.parseColor("#F0C8DC"));
                            float strokePx = getResources().getDisplayMetrics().density * 1.2f;
                            for (String tag : assessment.getRecommendedSpecializations()) {
                                Chip chip = new Chip(StudentProfileActivity.this);
                                chip.setText(tag);
                                chip.setCheckable(false);
                                chip.setChipBackgroundColor(tagBg);
                                chip.setTextColor(tagText);
                                chip.setChipStrokeColor(tagStroke);
                                chip.setChipStrokeWidth(strokePx);
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

    /** Loads ALL session notes for this student across all appointments, sorted newest first. */
    private void loadSessionNotes() {
        new SessionNoteRepository().getNotesForCounselorStudent(counselorId, studentId,
                new SessionNoteRepository.OnNotesLoadedCallback() {
                    @Override
                    public void onSuccess(List<SessionNote> notes) {
                        // Sort newest first by createdAt
                        List<SessionNote> sorted = new ArrayList<>(notes);
                        Collections.sort(sorted, (a, b) -> {
                            if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
                            if (a.getCreatedAt() == null) return 1;
                            if (b.getCreatedAt() == null) return -1;
                            return b.getCreatedAt().compareTo(a.getCreatedAt());
                        });

                        if (sorted.isEmpty()) {
                            textNotesEmpty.setVisibility(View.VISIBLE);
                            recyclerSessionNotes.setVisibility(View.GONE);
                        } else {
                            textNotesEmpty.setVisibility(View.GONE);
                            recyclerSessionNotes.setVisibility(View.VISIBLE);
                            recyclerSessionNotes.setAdapter(
                                    new SessionNoteHistoryAdapter(sorted));
                        }
                    }
                    @Override
                    public void onFailure(Exception e) {
                        textNotesEmpty.setVisibility(View.VISIBLE);
                        textNotesEmpty.setText(R.string.no_notes_for_student);
                        recyclerSessionNotes.setVisibility(View.GONE);
                    }
                });
    }

    private String valueOrDash(String value) {
        return value == null || value.isEmpty() ? "-" : value;
    }
}
