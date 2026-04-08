/*
 * CounselorProfileEditActivity.java
 * Role: Allows a logged-in counselor to edit their profile — bio, language,
 *       gender, and specialization tags. Changes are persisted to Firestore
 *       via CounselorRepository. Entry point is the "Edit Profile" button on
 *       CounselorDashboardActivity.
 *
 * Design pattern: Repository pattern (all Firestore writes via CounselorRepository).
 * Part of the BetterCAPS counseling platform.
 */
package com.example.moogerscouncil;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity for counselors to edit their own profile.
 * Pre-fills all fields from Firestore and persists changes on save.
 *
 * <p>Launched from {@link CounselorDashboardActivity} via the Edit Profile button.</p>
 */
public class CounselorProfileEditActivity extends AppCompatActivity {

    private TextInputEditText editTextBio;
    private TextInputEditText editTextLanguage;
    private TextInputEditText editTextGender;
    private ChipGroup chipGroupSpecializations;
    private Button buttonSaveProfile;

    private SwitchMaterial switchOnLeave;
    private TextInputLayout layoutLeaveMessage;
    private TextInputLayout layoutReferralCounselor;
    private TextInputEditText editTextLeaveMessage;
    private AutoCompleteTextView dropdownReferral;

    private CounselorRepository counselorRepository;
    private String counselorId;
    private List<Counselor> allCounselors = new ArrayList<>();
    private String selectedReferralId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_counselor_profile_edit);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Prefer the Firestore doc ID passed from CounselorDashboardActivity.
        counselorId = getIntent().getStringExtra("COUNSELOR_DOC_ID");
        if (counselorId == null) {
            counselorId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        counselorRepository = new CounselorRepository();

        editTextBio = findViewById(R.id.editTextBio);
        editTextLanguage = findViewById(R.id.editTextLanguage);
        editTextGender = findViewById(R.id.editTextGender);
        chipGroupSpecializations = findViewById(R.id.chipGroupSpecializations);
        buttonSaveProfile = findViewById(R.id.buttonSaveProfile);

        switchOnLeave = findViewById(R.id.switchOnLeave);
        layoutLeaveMessage = findViewById(R.id.layoutLeaveMessage);
        layoutReferralCounselor = findViewById(R.id.layoutReferralCounselor);
        editTextLeaveMessage = findViewById(R.id.editTextLeaveMessage);
        dropdownReferral = findViewById(R.id.dropdownReferral);

        switchOnLeave.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int visibility = isChecked ? View.VISIBLE : View.GONE;
            layoutLeaveMessage.setVisibility(visibility);
            layoutReferralCounselor.setVisibility(visibility);
            if (isChecked) {
                loadReferralCounselors();
            }
        });

        buildSpecializationChips(null);
        loadCurrentProfile();

        buttonSaveProfile.setOnClickListener(v -> saveProfile());
    }

    /**
     * Builds the specialization chip group from {@link SpecializationTags#ALL_TAGS}.
     * Pre-checks chips that match the counselor's existing specializations.
     *
     * @param existingSpecializations The counselor's current tags, or null if not yet loaded.
     */
    private void buildSpecializationChips(List<String> existingSpecializations) {
        chipGroupSpecializations.removeAllViews();
        for (String tag : SpecializationTags.ALL_TAGS) {
            Chip chip = new Chip(this);
            chip.setText(tag);
            chip.setCheckable(true);
            chip.setChecked(existingSpecializations != null
                    && existingSpecializations.contains(tag));
            chipGroupSpecializations.addView(chip);
        }
    }

    /**
     * Fetches the current counselor profile from Firestore and pre-fills all fields.
     */
    private void loadCurrentProfile() {
        counselorRepository.getCounselor(counselorId,
                new CounselorRepository.OnCounselorFetchedCallback() {
                    @Override
                    public void onSuccess(Counselor counselor) {
                        if (counselor.getBio() != null) {
                            editTextBio.setText(counselor.getBio());
                        }
                        if (counselor.getLanguage() != null) {
                            editTextLanguage.setText(counselor.getLanguage());
                        }
                        if (counselor.getGender() != null) {
                            editTextGender.setText(counselor.getGender());
                        }
                        buildSpecializationChips(counselor.getSpecializations());

                        // Pre-fill on-leave state
                        if (Boolean.TRUE.equals(counselor.getOnLeave())) {
                            switchOnLeave.setChecked(true);
                            // Visibility is triggered by the listener above
                            if (counselor.getOnLeaveMessage() != null) {
                                editTextLeaveMessage.setText(counselor.getOnLeaveMessage());
                            }
                            // Pre-select referral after dropdown loads (handled in loadReferralCounselors)
                            selectedReferralId = counselor.getReferralCounselorId();
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        // Profile may not exist yet (first-time setup); chips stay unchecked.
                        Toast.makeText(CounselorProfileEditActivity.this,
                                getString(R.string.error_loading_profile),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Fetches all counselors from Firestore and populates the referral dropdown.
     * Excludes the current counselor from the list so they cannot refer to themselves.
     */
    private void loadReferralCounselors() {
        String currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        counselorRepository.getAllCounselors(
                new CounselorRepository.OnCounselorsLoadedCallback() {
                    @Override
                    public void onSuccess(List<Counselor> counselors) {
                        allCounselors = counselors;
                        List<String> names = new ArrayList<>();
                        names.add(getString(R.string.referral_none_option));
                        for (Counselor c : counselors) {
                            if (!currentUid.equals(c.getUid())) {
                                names.add(c.getName());
                            }
                        }
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                CounselorProfileEditActivity.this,
                                android.R.layout.simple_list_item_1, names);
                        dropdownReferral.setAdapter(adapter);

                        // Pre-select the saved referral if one exists
                        if (selectedReferralId != null) {
                            int adjustedPos = 0;
                            for (int i = 0; i < allCounselors.size(); i++) {
                                if (!currentUid.equals(allCounselors.get(i).getUid())) {
                                    adjustedPos++;
                                    if (selectedReferralId.equals(allCounselors.get(i).getId())) {
                                        dropdownReferral.setText(allCounselors.get(i).getName(), false);
                                        break;
                                    }
                                }
                            }
                        }

                        dropdownReferral.setOnItemClickListener((parent, view, pos, id) -> {
                            if (pos == 0) {
                                selectedReferralId = null;
                            } else {
                                int counselorPos = 0;
                                for (Counselor c : allCounselors) {
                                    if (!currentUid.equals(c.getUid())) {
                                        counselorPos++;
                                        if (counselorPos == pos) {
                                            selectedReferralId = c.getId();
                                            break;
                                        }
                                    }
                                }
                            }
                        });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(CounselorProfileEditActivity.this,
                                getString(R.string.error_loading_counselors),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Collects all form values, builds a {@link Counselor} object, and persists
     * it to Firestore via {@link CounselorRepository#updateCounselorProfile}.
     */
    private void saveProfile() {
        String bio = editTextBio.getText() != null
                ? editTextBio.getText().toString().trim() : "";
        String language = editTextLanguage.getText() != null
                ? editTextLanguage.getText().toString().trim() : "";
        String gender = editTextGender.getText() != null
                ? editTextGender.getText().toString().trim() : "";

        List<String> selectedTags = new ArrayList<>();
        for (int i = 0; i < chipGroupSpecializations.getChildCount(); i++) {
            Chip chip = (Chip) chipGroupSpecializations.getChildAt(i);
            if (chip.isChecked()) {
                selectedTags.add(chip.getText().toString());
            }
        }

        Counselor updatedCounselor = new Counselor();
        updatedCounselor.setUid(counselorId);
        updatedCounselor.setBio(bio);
        updatedCounselor.setLanguage(language);
        updatedCounselor.setGender(gender);
        updatedCounselor.setSpecializations(selectedTags);

        // On-leave fields
        boolean isOnLeave = switchOnLeave.isChecked();
        updatedCounselor.setOnLeave(isOnLeave);
        if (isOnLeave) {
            String leaveMsg = editTextLeaveMessage.getText() != null
                    ? editTextLeaveMessage.getText().toString().trim() : "";
            updatedCounselor.setOnLeaveMessage(leaveMsg);
            updatedCounselor.setReferralCounselorId(selectedReferralId);
        } else {
            updatedCounselor.setOnLeaveMessage(null);
            updatedCounselor.setReferralCounselorId(null);
        }

        buttonSaveProfile.setEnabled(false);

        counselorRepository.updateCounselorProfile(counselorId, updatedCounselor,
                new CounselorRepository.OnUpdateCallback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(CounselorProfileEditActivity.this,
                                getString(R.string.success_profile_saved),
                                Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        buttonSaveProfile.setEnabled(true);
                        Toast.makeText(CounselorProfileEditActivity.this,
                                getString(R.string.error_saving_profile),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}
