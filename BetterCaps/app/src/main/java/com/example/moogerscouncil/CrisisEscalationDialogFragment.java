/*
 * CrisisEscalationDialogFragment.java
 * Role: Custom pastel pink/white dialog for counselor-triggered crisis escalation.
 *       Presents severity selection, action taken, dual dial buttons (campus security
 *       and crisis line), and a notes field. On save, notifies the host activity via
 *       OnEscalationSavedListener so the Crisis button can be permanently disabled.
 *
 * Design pattern: DialogFragment; Listener callback to host activity.
 * Part of the BetterCAPS counseling platform — Sprint 11 crisis escalation redesign.
 */
package com.example.moogerscouncil;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;

/**
 * Full-screen custom crisis escalation dialog matching the app's pastel pink design language.
 * Provides two distinct dial buttons (campus security + crisis line) and a save callback.
 */
public class CrisisEscalationDialogFragment extends DialogFragment {

    private static final String ARG_APPOINTMENT_ID = "APPOINTMENT_ID";
    private static final String ARG_COUNSELOR_ID   = "COUNSELOR_ID";
    private static final String ARG_STUDENT_ID     = "STUDENT_ID";

    /**
     * Callback interface for the host activity to receive the escalation ID after a successful save.
     * The host uses this to disable the Crisis button immediately without a second Firestore read.
     */
    public interface OnEscalationSavedListener {
        /** @param escalationId The Firestore document ID of the newly created escalation. */
        void onEscalationSaved(String escalationId);
    }

    /**
     * Creates a crisis escalation dialog for one appointment.
     *
     * @param appointmentId Firestore appointment document ID.
     * @param counselorId   Counselor UID.
     * @param studentId     Student UID.
     * @return Configured dialog fragment.
     */
    public static CrisisEscalationDialogFragment newInstance(String appointmentId,
                                                             String counselorId,
                                                             String studentId) {
        CrisisEscalationDialogFragment f = new CrisisEscalationDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_APPOINTMENT_ID, appointmentId);
        args.putString(ARG_COUNSELOR_ID, counselorId);
        args.putString(ARG_STUDENT_ID, studentId);
        f.setArguments(args);
        return f;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_crisis_escalation, null, false);
        dialog.setContentView(view);

        Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawableResource(android.R.color.transparent);
            android.util.DisplayMetrics dm = requireContext().getResources().getDisplayMetrics();
            WindowManager.LayoutParams p = w.getAttributes();
            p.width  = (int) (dm.widthPixels  * 0.88f);
            p.height = (int) (dm.heightPixels * 0.82f);
            p.gravity = Gravity.CENTER;
            w.setAttributes(p);
        }

        RadioGroup severityGroup = view.findViewById(R.id.radioSeverityGroup);
        RadioGroup actionGroup   = view.findViewById(R.id.radioActionGroup);
        EditText notesEdit       = view.findViewById(R.id.editCrisisNotes);

        // Dual dial buttons — ACTION_DIAL requires no CALL_PHONE permission
        view.findViewById(R.id.btnCallSecurity).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + getString(R.string.campus_security_number)));
            startActivity(intent);
        });

        view.findViewById(R.id.btnCallCrisisLine).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + getString(R.string.crisis_line_number)));
            startActivity(intent);
        });

        view.findViewById(R.id.btnCrisisCancel).setOnClickListener(v -> dismiss());

        view.findViewById(R.id.btnSaveEscalation).setOnClickListener(v ->
                saveEscalation(severityGroup, actionGroup, notesEdit, dialog));

        return dialog;
    }

    private void saveEscalation(RadioGroup severityGroup, RadioGroup actionGroup,
                                EditText notesEdit, Dialog dialog) {
        String severity = selectedSeverity(severityGroup.getCheckedRadioButtonId());
        String action   = selectedAction(actionGroup.getCheckedRadioButtonId());
        String notes    = notesEdit.getText() == null ? "" : notesEdit.getText().toString().trim();

        Bundle args = getArguments();
        String appointmentId = args != null ? args.getString(ARG_APPOINTMENT_ID) : null;
        String counselorId   = args != null ? args.getString(ARG_COUNSELOR_ID) : null;
        String studentId     = args != null ? args.getString(ARG_STUDENT_ID) : null;

        MaterialButton btnSave = dialog.findViewById(R.id.btnSaveEscalation);
        if (btnSave != null) btnSave.setEnabled(false);

        CrisisEscalation escalation = new CrisisEscalation(
                appointmentId, counselorId, studentId, severity, action, notes);
        new CrisisEscalationRepository().createEscalation(escalation,
                new CrisisEscalationRepository.OnCrisisActionCallback() {
                    @Override
                    public void onSuccess(String escalationId) {
                        if (!isAdded()) return;
                        AppToast.show(requireContext(),
                                R.string.crisis_escalation_saved, AppToast.LENGTH_LONG);
                        if (getActivity() instanceof OnEscalationSavedListener) {
                            ((OnEscalationSavedListener) getActivity()).onEscalationSaved(escalationId);
                        }
                        dismiss();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        if (!isAdded()) return;
                        if (btnSave != null) btnSave.setEnabled(true);
                        AppToast.show(requireContext(),
                                R.string.crisis_escalation_error, AppToast.LENGTH_LONG);
                    }
                });
    }

    private String selectedSeverity(int checkedId) {
        if (checkedId == R.id.radioSeverityHigh)      return CrisisEscalation.SEVERITY_HIGH;
        if (checkedId == R.id.radioSeverityImmediate)  return CrisisEscalation.SEVERITY_IMMEDIATE;
        return CrisisEscalation.SEVERITY_MODERATE;
    }

    private String selectedAction(int checkedId) {
        if (checkedId == R.id.radioActionSecurity) return CrisisEscalation.ACTION_CALLED_SECURITY;
        if (checkedId == R.id.radioActionCaps)     return CrisisEscalation.ACTION_REFERRED_CAPS;
        if (checkedId == R.id.radioActionOther)    return CrisisEscalation.ACTION_OTHER;
        return CrisisEscalation.ACTION_SAFETY_PLAN;
    }
}
