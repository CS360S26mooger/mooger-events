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

import java.util.List;

/**
 * Multi-step questionnaire that guides students through a brief
 * assessment and recommends a counselor match.
 *
 * <p>Launched from {@link StudentHomeActivity} via the "Find My Match" card.</p>
 */
public class QuizActivity extends AppCompatActivity {

    private static final int TOTAL_QUESTIONS = 3;

    private int currentStep = 0;
    private final String[] answers = new String[TOTAL_QUESTIONS];

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

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
            showResult();
        }
    }

    private void showResult() {
        layoutQuestion.setVisibility(View.GONE);
        layoutResult.setVisibility(View.VISIBLE);
        progressBar.setProgress(TOTAL_QUESTIONS);

        textResultTitle.setText("We found your match!");

        // Build summary from answers
        String summary = "Based on your concern about \""
                + answers[0] + "\" and your preference for \""
                + answers[2] + "\", we think this counselor is a great fit.";
        textResultSummary.setText(summary);

        // Fetch the counselor to show their name and route to profile
        CounselorRepository repo = new CounselorRepository();
        repo.getAllCounselors(new CounselorRepository.OnCounselorsLoadedCallback() {
            @Override
            public void onSuccess(List<Counselor> counselors) {
                // Find the first real counselor (one with a name)
                Counselor match = null;
                for (Counselor c : counselors) {
                    if (c.getName() != null) {
                        match = c;
                        break;
                    }
                }
                if (match == null && !counselors.isEmpty()) {
                    match = counselors.get(0);
                }

                if (match != null) {
                    textResultSubtitle.setText(match.getName());
                    final Counselor finalMatch = match;

                    btnViewProfile.setOnClickListener(v -> {
                        Intent intent = new Intent(QuizActivity.this,
                                CounselorProfileActivity.class);
                        intent.putExtra("COUNSELOR_ID", finalMatch.getId());
                        String slotId = finalMatch.getUid() != null
                                ? finalMatch.getUid() : finalMatch.getId();
                        intent.putExtra("SLOT_COUNSELOR_ID", slotId);
                        intent.putExtra("COUNSELOR_NAME", finalMatch.getName());
                        startActivity(intent);
                        finish();
                    });
                } else {
                    textResultSubtitle.setText("No counselors available");
                    btnViewProfile.setEnabled(false);
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(QuizActivity.this,
                        getString(R.string.error_loading_counselors),
                        Toast.LENGTH_SHORT).show();
                textResultSubtitle.setText("Unable to load");
                btnViewProfile.setEnabled(false);
            }
        });

        // "Browse All Counselors" always available
        btnBrowseAll.setOnClickListener(v -> {
            startActivity(new Intent(QuizActivity.this, CounselorListActivity.class));
            finish();
        });
    }
}
