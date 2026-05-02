/*
 * QuizActivity.java
 * Role: Multi-step questionnaire that guides students through a brief assessment
 *       and recommends a counselor match. Currently all paths lead to the single
 *       counselor on the platform. The question structure is extensible for future
 *       matching when more counselors join.
 *
 * Flow: 3 questions → result screen → CounselorProfileActivity.
 *
 * Design pattern: Repository pattern (CounselorRepository for result lookup).
 * Part of the BetterCAPS counseling platform.
 */
package com.example.moogerscouncil;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

/**
 * Multi-step questionnaire that guides students through a brief
 * assessment and recommends a counselor match.
 *
 * <p>Launched from {@link StudentHomeActivity} via the "Find My Match" card.</p>
 */
public class QuizActivity extends AppCompatActivity {

    private static final int TOTAL_QUESTIONS = 3;
    public static final String EXTRA_ASSESSMENT_ID = "ASSESSMENT_ID";
    public static final String EXTRA_MATCHED_COUNSELOR_ID = "COUNSELOR_ID";
    public static final String EXTRA_MATCH_REASON = "MATCH_REASON";

    private int currentStep = 0;
    private final String[] answers = new String[TOTAL_QUESTIONS];
    private String savedAssessmentId;
    private String matchReason;

    // Question data: each row = {prompt, option1, option2, option3, option4}
    private final String[][] questions = {
        {"What's been on your mind lately?",
         "Emotional / Personal",
         "Academic / Career",
         "Relationships",
         "Not sure / General"},
        {"How long have you been dealing with this?",
         "Just started (less than a week)",
         "A few weeks",
         "More than a month",
         "On and off for a while"},
        {"What kind of support are you looking for?",
         "Someone to talk to and listen",
         "Practical strategies and coping tools",
         "Help understanding my feelings",
         "Crisis or urgent support"}
    };

    private TextView textQuestionNumber;
    private TextView textQuestion;
    private ProgressBar progressBar;
    private MaterialButton btnOption1, btnOption2, btnOption3, btnOption4;
    private ImageButton btnBack;

    // Result screen views
    private View layoutQuestion;
    private View layoutResult;
    private TextView textResultTitle;
    private TextView textResultSubtitle;
    private TextView textResultSummary;
    private MaterialButton btnViewProfile;
    private MaterialButton btnBrowseAll;
    private IntakeAssessmentRepository intakeRepository;
    private CounselorRepository counselorRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);
        intakeRepository = new IntakeAssessmentRepository();
        counselorRepository = new CounselorRepository();

        // Question views
        layoutQuestion = findViewById(R.id.layoutQuestion);
        textQuestionNumber = findViewById(R.id.textQuestionNumber);
        textQuestion = findViewById(R.id.textQuestion);
        progressBar = findViewById(R.id.progressQuiz);
        btnOption1 = findViewById(R.id.btnOption1);
        btnOption2 = findViewById(R.id.btnOption2);
        btnOption3 = findViewById(R.id.btnOption3);
        btnOption4 = findViewById(R.id.btnOption4);
        btnBack = findViewById(R.id.btnBack);

        // Result views
        layoutResult = findViewById(R.id.layoutResult);
        textResultTitle = findViewById(R.id.textResultTitle);
        textResultSubtitle = findViewById(R.id.textResultSubtitle);
        textResultSummary = findViewById(R.id.textResultSummary);
        btnViewProfile = findViewById(R.id.btnViewProfile);
        btnBrowseAll = findViewById(R.id.btnBrowseAll);

        progressBar.setMax(TOTAL_QUESTIONS);

        // Wire option buttons — all share the same listener
        View.OnClickListener optionListener = v -> {
            MaterialButton btn = (MaterialButton) v;
            answers[currentStep] = btn.getText().toString();
            advanceStep();
        };
        btnOption1.setOnClickListener(optionListener);
        btnOption2.setOnClickListener(optionListener);
        btnOption3.setOnClickListener(optionListener);
        btnOption4.setOnClickListener(optionListener);

        btnBack.setOnClickListener(v -> {
            if (currentStep > 0) {
                currentStep--;
                displayQuestion(currentStep);
            } else {
                finish();
            }
        });

        displayQuestion(0);
    }

    private void displayQuestion(int step) {
        layoutQuestion.setVisibility(View.VISIBLE);
        layoutResult.setVisibility(View.GONE);

        textQuestionNumber.setText("Step " + (step + 1) + " of " + TOTAL_QUESTIONS);
        textQuestion.setText(questions[step][0]);
        btnOption1.setText(questions[step][1]);
        btnOption2.setText(questions[step][2]);
        btnOption3.setText(questions[step][3]);
        btnOption4.setText(questions[step][4]);

        progressBar.setProgress(step);

        // Show back arrow only after first question
        btnBack.setVisibility(step > 0 ? View.VISIBLE : View.INVISIBLE);
    }

    private void advanceStep() {
        currentStep++;
        if (currentStep < TOTAL_QUESTIONS) {
            displayQuestion(currentStep);
        } else {
            saveAssessmentAndMatch();
        }
    }

    private void saveAssessmentAndMatch() {
        layoutQuestion.setVisibility(View.GONE);
        layoutResult.setVisibility(View.VISIBLE);
        progressBar.setProgress(TOTAL_QUESTIONS);
        btnViewProfile.setEnabled(false);
        btnBrowseAll.setEnabled(false);
        textResultTitle.setText(R.string.quiz_saving_assessment);
        textResultSubtitle.setText("");
        textResultSummary.setText("");

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, R.string.error_login_required, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String urgencyLevel = urgencyFromAnswer(answers[2]);
        List<String> tags = IntakeMatcher.tagsForAnswers(answers[0], answers[2], urgencyLevel);
        IntakeAssessment assessment = new IntakeAssessment(
                user.getUid(),
                answers[0],
                answers[1],
                answers[2],
                urgencyLevel,
                tags);
        matchReason = getString(R.string.quiz_match_reason);

        intakeRepository.saveAssessment(assessment,
                new IntakeAssessmentRepository.OnAssessmentSavedCallback() {
            @Override
            public void onSuccess(String assessmentId) {
                savedAssessmentId = assessmentId;
                assessment.setId(assessmentId);
                findCounselorMatch(assessment);
            }

            @Override
            public void onFailure(Exception e) {
                textResultTitle.setText(R.string.assessment_error_save);
                btnBrowseAll.setEnabled(true);
                Toast.makeText(QuizActivity.this,
                        R.string.assessment_error_save,
                        Toast.LENGTH_LONG).show();
            }
        });

        btnBrowseAll.setOnClickListener(v -> {
            startActivity(new Intent(QuizActivity.this, CounselorListActivity.class));
            finish();
        });
    }

    private void findCounselorMatch(IntakeAssessment assessment) {
        textResultTitle.setText(R.string.quiz_matching_counselor);
        counselorRepository.getAllCounselors(new CounselorRepository.OnCounselorsLoadedCallback() {
            @Override
            public void onSuccess(List<Counselor> counselors) {
                Counselor match = IntakeMatcher.findBestCounselor(counselors, assessment);
                if (match == null) {
                    showNoMatch();
                    return;
                }

                assessment.setMatchedCounselorId(match.getId());
                assessment.setMatchedCounselorName(match.getName());
                Counselor finalMatch = match;
                intakeRepository.updateMatchedCounselor(savedAssessmentId,
                        finalMatch.getId(),
                        finalMatch.getName(),
                        new IntakeAssessmentRepository.OnAssessmentSavedCallback() {
                            @Override
                            public void onSuccess(String assessmentId) {
                                showResult(finalMatch, assessment);
                            }

                            @Override
                            public void onFailure(Exception e) {
                                showResult(finalMatch, assessment);
                            }
                        });
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(QuizActivity.this,
                        getString(R.string.error_loading_counselors),
                        Toast.LENGTH_SHORT).show();
                textResultTitle.setText(R.string.error_loading_counselors);
                textResultSubtitle.setText("");
                btnViewProfile.setEnabled(false);
                btnBrowseAll.setEnabled(true);
            }
        });
    }

    private void showResult(Counselor match, IntakeAssessment assessment) {
        textResultTitle.setText(R.string.quiz_match_title);
        textResultSubtitle.setText(match.getName());
        textResultSummary.setText(buildMatchSummary(assessment));
        btnBrowseAll.setEnabled(true);
        btnViewProfile.setEnabled(true);
        btnViewProfile.setOnClickListener(v -> {
            Intent intent = new Intent(QuizActivity.this, CounselorProfileActivity.class);
            intent.putExtra(EXTRA_MATCHED_COUNSELOR_ID, match.getId());
            String slotId = match.getUid() != null ? match.getUid() : match.getId();
            intent.putExtra("SLOT_COUNSELOR_ID", slotId);
            intent.putExtra("COUNSELOR_NAME", match.getName());
            intent.putExtra(EXTRA_ASSESSMENT_ID, savedAssessmentId);
            intent.putExtra(EXTRA_MATCH_REASON, matchReason);
            startActivity(intent);
            finish();
        });
    }

    private void showNoMatch() {
        textResultTitle.setText(R.string.no_counselors_available);
        textResultSubtitle.setText("");
        textResultSummary.setText(R.string.quiz_browse_fallback);
        btnViewProfile.setEnabled(false);
        btnBrowseAll.setEnabled(true);
    }

    private String buildMatchSummary(IntakeAssessment assessment) {
        return getString(R.string.quiz_match_summary,
                assessment.getPrimaryConcern(),
                assessment.getSupportType());
    }

    private String urgencyFromAnswer(String supportType) {
        if (supportType != null && supportType.toLowerCase().contains("crisis")) {
            return IntakeAssessment.URGENCY_CRISIS;
        }
        if (supportType != null && supportType.toLowerCase().contains("practical")) {
            return IntakeAssessment.URGENCY_MEDIUM;
        }
        return IntakeAssessment.URGENCY_LOW;
    }
}
