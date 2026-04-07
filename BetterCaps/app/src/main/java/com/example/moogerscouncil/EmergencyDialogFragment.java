/*
 * EmergencyDialogFragment.java
 * Role: DialogFragment that displays campus crisis line phone numbers and triggers
 *       ACTION_DIAL intents. Has zero network dependencies — works fully offline.
 *       Implements the CrisisIntervention CRC card's dial-only responsibility.
 *
 * Design pattern: Fragment (DialogFragment).
 * Part of the BetterCAPS counseling platform.
 */
package com.example.moogerscouncil;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

/**
 * Emergency contact dialog accessible from the student home screen.
 * Displays crisis line phone numbers with buttons that pre-fill the phone dialer.
 *
 * <p>Uses {@code ACTION_DIAL} (not {@code ACTION_CALL}) so the user sees the
 * number before the call is placed — no {@code CALL_PHONE} permission required,
 * and accidental taps never auto-dial.</p>
 *
 * <p>Phone numbers are loaded from {@code strings.xml} and can be updated
 * without code changes.</p>
 */
public class EmergencyDialogFragment extends DialogFragment {

    /**
     * Creates a new instance of the dialog.
     *
     * @return A new {@link EmergencyDialogFragment}.
     */
    public static EmergencyDialogFragment newInstance() {
        return new EmergencyDialogFragment();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Build a vertical list: Crisis Line → Campus Emergency → Dismiss
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_emergency, null);

        Button btnCrisis  = view.findViewById(R.id.btnCallCrisis);
        Button btnCampus  = view.findViewById(R.id.btnCallCampus);
        Button btnDismiss = view.findViewById(R.id.btnDismissEmergency);

        btnCrisis.setOnClickListener(v -> {
            dialNumber(getString(R.string.crisis_line_number));
            dismiss();
        });

        btnCampus.setOnClickListener(v -> {
            dialNumber(getString(R.string.campus_security_number));
            dismiss();
        });

        btnDismiss.setOnClickListener(v -> dismiss());

        return new AlertDialog.Builder(requireContext())
                .setTitle(R.string.emergency_title)
                .setView(view)
                .create();
    }

    /**
     * Opens the phone dialer with the given number pre-filled.
     * Does not place the call automatically.
     *
     * @param phoneNumber The phone number string to dial.
     */
    private void dialNumber(String phoneNumber) {
        Intent dialIntent = new Intent(Intent.ACTION_DIAL,
                Uri.parse("tel:" + phoneNumber));
        startActivity(dialIntent);
    }
}
