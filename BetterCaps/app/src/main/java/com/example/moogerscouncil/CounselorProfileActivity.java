package com.example.moogerscouncil;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

/**
 * Full counselor profile view for students.
 * Receives a COUNSELOR_ID extra and fetches the profile through
 * {@link CounselorRepository}.
 */
public class CounselorProfileActivity extends AppCompatActivity {

    private TextView textCounselorName;
    private TextView textLanguage;
    private TextView textGender;
    private TextView textBio;
    private TextView textOnLeaveMessage;
    private CardView cardOnLeave;
    private com.google.android.material.button.MaterialButton buttonSeeReferral;
    private ChipGroup chipGroupSpecializations;
    private Button buttonBookAppointment;
    private com.google.android.material.button.MaterialButton buttonJoinWaitlist;

    private CounselorRepository counselorRepository;
    private AvailabilityRepository availabilityRepository;
    private String counselorId;
    private String slotCounselorId;
    private String counselorName;
    private String assessmentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_counselor_profile);

        counselorId = getIntent().getStringExtra("COUNSELOR_ID");
        slotCounselorId = getIntent().getStringExtra("SLOT_COUNSELOR_ID");
        if (slotCounselorId == null) slotCounselorId = counselorId;
        counselorName = getIntent().getStringExtra("COUNSELOR_NAME");
        assessmentId = getIntent().getStringExtra(QuizActivity.EXTRA_ASSESSMENT_ID);

        if (counselorId == null) {
            AppToast.show(this, getString(R.string.error_loading_profile), AppToast.LENGTH_SHORT);
            finish();
            return;
        }

        counselorRepository = new CounselorRepository();
        availabilityRepository = new AvailabilityRepository();

        textCounselorName = findViewById(R.id.textCounselorName);
        textLanguage = findViewById(R.id.textLanguage);
        textGender = findViewById(R.id.textGender);
        textBio = findViewById(R.id.textBio);
        textOnLeaveMessage = findViewById(R.id.textOnLeaveMessage);
        cardOnLeave = findViewById(R.id.cardOnLeave);
        buttonSeeReferral = findViewById(R.id.buttonSeeReferral);
        chipGroupSpecializations = findViewById(R.id.chipGroupSpecializations);
        buttonBookAppointment = findViewById(R.id.buttonBookAppointment);
        buttonJoinWaitlist = findViewById(R.id.buttonJoinWaitlist);

        ((ImageButton) findViewById(R.id.buttonBack)).setOnClickListener(v -> finish());

        if (counselorName != null) {
            textCounselorName.setText(counselorName);
        }

        loadProfile();
    }

    private void loadProfile() {
        Counselor cached = SessionCache.getInstance().getSingleCounselor(counselorId);
        if (cached != null) {
            populateUI(cached);
        }

        counselorRepository.getCounselor(counselorId,
                new CounselorRepository.OnCounselorFetchedCallback() {
                    @Override
                    public void onSuccess(Counselor counselor) {
                        SessionCache.getInstance().putSingleCounselor(counselorId, counselor);
                        populateUI(counselor);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        if (cached == null) {
                            AppToast.show(CounselorProfileActivity.this,
                                    getString(R.string.error_loading_profile),
                                    AppToast.LENGTH_LONG);
                            finish();
                        }
                    }
                });
    }

    private void populateUI(Counselor counselor) {
        if (counselor.getName() != null) {
            textCounselorName.setText(counselor.getName());
        }

        String lang = counselor.getLanguage();
        textLanguage.setText(lang != null && !lang.isEmpty()
                ? getString(R.string.label_language) + ": " + lang
                : getString(R.string.label_language) + ": -");

        String gender = counselor.getGender();
        textGender.setText(gender != null && !gender.isEmpty()
                ? getString(R.string.label_gender) + ": " + gender
                : getString(R.string.label_gender) + ": -");

        String bio = counselor.getBio();
        textBio.setText(bio != null && !bio.isEmpty() ? bio : "-");

        chipGroupSpecializations.removeAllViews();
        List<String> specs = counselor.getSpecializations();
        if (specs != null && !specs.isEmpty()) {
            for (int i = 0; i < specs.size(); i++) {
                if (i > 0) {
                    Chip dot = new Chip(CounselorProfileActivity.this);
                    dot.setText("•");
                    dot.setCheckable(false);
                    dot.setClickable(false);
                    dot.setChipBackgroundColor(ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));
                    dot.setTextColor(Color.parseColor("#C96B8E"));
                    dot.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14);
                    dot.setChipStrokeWidth(0f);
                    dot.setEnsureMinTouchTargetSize(false);
                    chipGroupSpecializations.addView(dot);
                }
                Chip chip = new Chip(CounselorProfileActivity.this);
                chip.setText(specs.get(i));
                chip.setCheckable(false);
                chip.setClickable(false);
                chip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#F8D7E3")));
                chip.setTextColor(Color.parseColor("#C96B8E"));
                chip.setChipStrokeWidth(0f);
                chipGroupSpecializations.addView(chip);
            }
        }

        boolean isOnLeave = Boolean.TRUE.equals(counselor.getOnLeave());
        String waitlistReason = isOnLeave
                ? getString(R.string.waitlist_reason_preferred_counselor)
                : getString(R.string.waitlist_reason_no_slots);
        buttonJoinWaitlist.setVisibility(View.VISIBLE);
        buttonJoinWaitlist.setEnabled(true);
        buttonJoinWaitlist.setOnClickListener(v ->
                openWaitlistRequest(slotCounselorId, assessmentId));

        if (isOnLeave) {
            cardOnLeave.setVisibility(View.VISIBLE);
            String leaveMsg = counselor.getOnLeaveMessage();
            textOnLeaveMessage.setText(leaveMsg != null ? leaveMsg : "");
            buttonBookAppointment.setEnabled(false);
            buttonBookAppointment.setText(R.string.currently_unavailable);

            String referralId = counselor.getReferralCounselorId();
            if (referralId != null && !referralId.isEmpty()) {
                buttonSeeReferral.setVisibility(View.VISIBLE);
                buttonSeeReferral.setOnClickListener(v -> {
                    Counselor referral = SessionCache.getInstance().getSingleCounselor(referralId);
                    String referralSlotId = (referral != null && referral.getUid() != null)
                            ? referral.getUid() : referralId;
                    Intent intent = new Intent(CounselorProfileActivity.this,
                            CounselorProfileActivity.class);
                    intent.putExtra("COUNSELOR_ID", referralId);
                    intent.putExtra("SLOT_COUNSELOR_ID", referralSlotId);
                    intent.putExtra(QuizActivity.EXTRA_ASSESSMENT_ID, assessmentId);
                    startActivity(intent);
                });
            } else {
                buttonSeeReferral.setVisibility(View.GONE);
            }
        } else {
            cardOnLeave.setVisibility(View.GONE);
            buttonSeeReferral.setVisibility(View.GONE);
            buttonBookAppointment.setEnabled(true);
            buttonBookAppointment.setText(R.string.button_book_appointment);
            buttonBookAppointment.setOnClickListener(v -> {
                Intent intent = new Intent(CounselorProfileActivity.this, BookingActivity.class);
                intent.putExtra("counselorId", slotCounselorId);
                intent.putExtra("counselorDocId", counselorId);
                intent.putExtra("counselorName", counselor.getName());
                intent.putExtra(QuizActivity.EXTRA_ASSESSMENT_ID, assessmentId);
                startActivity(intent);
            });
            checkSlotAvailability();
        }
    }

    private void checkSlotAvailability() {
        availabilityRepository.hasAvailableSlots(slotCounselorId,
                new AvailabilityRepository.OnAvailabilityCheckCallback() {
                    @Override
                    public void onSuccess(boolean hasAvailableSlots) {
                        if (hasAvailableSlots) {
                            buttonBookAppointment.setEnabled(true);
                            buttonBookAppointment.setText(R.string.button_book_appointment);
                        } else if (counselorId != null && !counselorId.equals(slotCounselorId)) {
                            checkFallbackSlotAvailability();
                        } else {
                            showNoSlotsState();
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        if (counselorId != null && !counselorId.equals(slotCounselorId)) {
                            checkFallbackSlotAvailability();
                        } else {
                            showNoSlotsState();
                        }
                    }
                });
    }

    private void checkFallbackSlotAvailability() {
        availabilityRepository.hasAvailableSlots(counselorId,
                new AvailabilityRepository.OnAvailabilityCheckCallback() {
                    @Override
                    public void onSuccess(boolean hasAvailableSlots) {
                        if (hasAvailableSlots) {
                            buttonBookAppointment.setEnabled(true);
                            buttonBookAppointment.setText(R.string.button_book_appointment);
                        } else {
                            showNoSlotsState();
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        showNoSlotsState();
                    }
                });
    }

    private void showNoSlotsState() {
        buttonBookAppointment.setEnabled(false);
        buttonBookAppointment.setText(R.string.no_slots_available);
        buttonJoinWaitlist.setVisibility(View.VISIBLE);
        buttonJoinWaitlist.setEnabled(true);
    }

    private void openWaitlistRequest(String waitlistCounselorId, String assessmentId) {
        Intent intent = new Intent(this, WaitlistRequestActivity.class);
        intent.putExtra(WaitlistRequestActivity.EXTRA_COUNSELOR_ID, waitlistCounselorId);
        intent.putExtra(WaitlistRequestActivity.EXTRA_ASSESSMENT_ID, assessmentId);
        startActivity(intent);
    }
}
