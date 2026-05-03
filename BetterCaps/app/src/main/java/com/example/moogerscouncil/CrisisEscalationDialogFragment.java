package com.example.moogerscouncil;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;

/**
 * Dialog workflow for counselor-triggered crisis escalation.
 * Saves an escalation record and can open the campus security dialer.
 */
public class CrisisEscalationDialogFragment extends DialogFragment {

    private static final String ARG_APPOINTMENT_ID = "APPOINTMENT_ID";
    private static final String ARG_COUNSELOR_ID = "COUNSELOR_ID";
    private static final String ARG_STUDENT_ID = "STUDENT_ID";

    /** Creates a crisis escalation dialog for one appointment. */
    public static CrisisEscalationDialogFragment newInstance(String appointmentId,
                                                             String counselorId,
                                                             String studentId) {
        CrisisEscalationDialogFragment fragment = new CrisisEscalationDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_APPOINTMENT_ID, appointmentId);
        args.putString(ARG_COUNSELOR_ID, counselorId);
        args.putString(ARG_STUDENT_ID, studentId);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_crisis_escalation, null, false);

        RadioGroup severityGroup = view.findViewById(R.id.radioSeverityGroup);
        RadioGroup actionGroup = view.findViewById(R.id.radioActionGroup);
        EditText notesEdit = view.findViewById(R.id.editCrisisNotes);
        MaterialButton callButton = view.findViewById(R.id.buttonCallSecurity);

        callButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + getString(R.string.campus_security_number)));
            startActivity(intent);
        });

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(view)
                .setNegativeButton(R.string.button_cancel, null)
                .setPositiveButton(R.string.save_escalation, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> saveEscalation(severityGroup, actionGroup, notesEdit, dialog)));

        return dialog;
    }

    private void saveEscalation(RadioGroup severityGroup, RadioGroup actionGroup,
                                EditText notesEdit, Dialog dialog) {
        String severity = selectedSeverity(severityGroup.getCheckedRadioButtonId());
        String action = selectedAction(actionGroup.getCheckedRadioButtonId());
        String notes = notesEdit.getText() == null ? "" : notesEdit.getText().toString().trim();

        Bundle args = getArguments();
        String appointmentId = args != null ? args.getString(ARG_APPOINTMENT_ID) : null;
        String counselorId = args != null ? args.getString(ARG_COUNSELOR_ID) : null;
        String studentId = args != null ? args.getString(ARG_STUDENT_ID) : null;

        CrisisEscalation escalation = new CrisisEscalation(
                appointmentId, counselorId, studentId, severity, action, notes);
        new CrisisEscalationRepository().createEscalation(
                escalation,
                new CrisisEscalationRepository.OnCrisisActionCallback() {
                    @Override
                    public void onSuccess(String escalationId) {
                        Toast.makeText(requireContext(),
                                R.string.crisis_escalation_saved,
                                Toast.LENGTH_LONG).show();
                        dialog.dismiss();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(requireContext(),
                                R.string.crisis_escalation_error,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private String selectedSeverity(int checkedId) {
        if (checkedId == R.id.radioSeverityHigh) return CrisisEscalation.SEVERITY_HIGH;
        if (checkedId == R.id.radioSeverityImmediate) return CrisisEscalation.SEVERITY_IMMEDIATE;
        return CrisisEscalation.SEVERITY_MODERATE;
    }

    private String selectedAction(int checkedId) {
        if (checkedId == R.id.radioActionSecurity) return CrisisEscalation.ACTION_CALLED_SECURITY;
        if (checkedId == R.id.radioActionCaps) return CrisisEscalation.ACTION_REFERRED_CAPS;
        if (checkedId == R.id.radioActionOther) return CrisisEscalation.ACTION_OTHER;
        return CrisisEscalation.ACTION_SAFETY_PLAN;
    }
}
