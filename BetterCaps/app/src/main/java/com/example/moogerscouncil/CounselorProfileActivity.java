/*
 * CounselorProfileActivity.java
 * Role: Displays the full profile of a single counselor — bio, specialization
 *       tags, language, gender, and an on-leave notice if applicable. Entry
 *       point for booking: the "Book Appointment" button routes to BookingActivity.
 *
 * Design pattern: Repository pattern (reads via CounselorRepository).
 * Part of the BetterCAPS counseling platform.
 */
package com.example.moogerscouncil;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.List;

/**
 * Full counselor profile view for students.
 * Receives a {@code COUNSELOR_ID} string extra and fetches the profile
 * from Firestore via {@link CounselorRepository}.
 *
 * <p>Launched by {@link CounselorAdapter} when a student taps a counselor card.</p>
 */
public class CounselorProfileActivity extends AppCompatActivity {

    private TextView textCounselorName;
    private TextView textLanguage;
    private TextView textGender;
    private TextView textBio;
    private TextView textOnLeaveTitle;
    private TextView textOnLeaveMessage;
    private CardView cardOnLeave;
    private ChipGroup chipGroupSpecializations;
    private Button buttonBookAppointment;

    private CounselorRepository counselorRepository;
    private String counselorId;
    private String counselorName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_counselor_profile);

        counselorId = getIntent().getStringExtra("COUNSELOR_ID");
        counselorName = getIntent().getStringExtra("COUNSELOR_NAME");

        if (counselorId == null) {
            Toast.makeText(this, getString(R.string.error_loading_profile), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        counselorRepository = new CounselorRepository();

        textCounselorName = findViewById(R.id.textCounselorName);
        textLanguage = findViewById(R.id.textLanguage);
        textGender = findViewById(R.id.textGender);
        textBio = findViewById(R.id.textBio);
        textOnLeaveTitle = findViewById(R.id.textOnLeaveTitle);
        textOnLeaveMessage = findViewById(R.id.textOnLeaveMessage);
        cardOnLeave = findViewById(R.id.cardOnLeave);
        chipGroupSpecializations = findViewById(R.id.chipGroupSpecializations);
        buttonBookAppointment = findViewById(R.id.buttonBookAppointment);

        if (counselorName != null) {
            textCounselorName.setText(counselorName);
        }

        loadProfile();
    }

    /**
     * Fetches the counselor document from Firestore and populates all UI fields.
     */
    private void loadProfile() {
        counselorRepository.getCounselor(counselorId,
                new CounselorRepository.OnCounselorFetchedCallback() {
                    @Override
                    public void onSuccess(Counselor counselor) {
                        populateUI(counselor);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(CounselorProfileActivity.this,
                                getString(R.string.error_loading_profile),
                                Toast.LENGTH_LONG).show();
                        finish();
                    }
                });
    }

    /**
     * Populates all UI elements from the fetched {@link Counselor} object.
     *
     * @param counselor The fetched counselor profile.
     */
    private void populateUI(Counselor counselor) {
        // Name
        if (counselor.getName() != null) {
            textCounselorName.setText(counselor.getName());
        }

        // Language
        String lang = counselor.getLanguage();
        textLanguage.setText(lang != null && !lang.isEmpty()
                ? getString(R.string.label_language) + ": " + lang
                : getString(R.string.label_language) + ": —");

        // Gender
        String gender = counselor.getGender();
        textGender.setText(gender != null && !gender.isEmpty()
                ? getString(R.string.label_gender) + ": " + gender
                : getString(R.string.label_gender) + ": —");

        // Bio
        String bio = counselor.getBio();
        textBio.setText(bio != null && !bio.isEmpty() ? bio : "—");

        // Specialization chips (read-only)
        chipGroupSpecializations.removeAllViews();
        List<String> specs = counselor.getSpecializations();
        if (specs != null && !specs.isEmpty()) {
            for (String tag : specs) {
                Chip chip = new Chip(CounselorProfileActivity.this);
                chip.setText(tag);
                chip.setCheckable(false);
                chip.setClickable(false);
                chipGroupSpecializations.addView(chip);
            }
        }

        // On-leave notice
        boolean isOnLeave = Boolean.TRUE.equals(counselor.getOnLeave());
        if (isOnLeave) {
            cardOnLeave.setVisibility(View.VISIBLE);
            String leaveMsg = counselor.getOnLeaveMessage();
            textOnLeaveMessage.setText(leaveMsg != null ? leaveMsg : "");
            buttonBookAppointment.setEnabled(false);
            buttonBookAppointment.setText(R.string.currently_unavailable);
        } else {
            cardOnLeave.setVisibility(View.GONE);
            buttonBookAppointment.setEnabled(true);
            buttonBookAppointment.setText(R.string.button_book_appointment);
            buttonBookAppointment.setOnClickListener(v -> {
                Intent intent = new Intent(CounselorProfileActivity.this,
                        BookingActivity.class);
                intent.putExtra("counselorId", counselorId);
                intent.putExtra("counselorName", counselor.getName());
                startActivity(intent);
            });
        }
    }
}
