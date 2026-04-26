package com.example.moogerscouncil;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;

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

    public static EmergencyDialogFragment newInstance() {
        return new EmergencyDialogFragment();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_emergency, null);
        dialog.setContentView(view);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }

        MaterialButton btnCrisis  = view.findViewById(R.id.btnCallCrisis);
        MaterialButton btnCampus  = view.findViewById(R.id.btnCallCampus);
        MaterialButton btnDismiss = view.findViewById(R.id.btnDismissEmergency);

        btnCrisis.setOnClickListener(v -> {
            dialNumber(getString(R.string.crisis_line_number));
            dismiss();
        });
        btnCampus.setOnClickListener(v -> {
            dialNumber(getString(R.string.campus_security_number));
            dismiss();
        });
        btnDismiss.setOnClickListener(v -> dismiss());

        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog == null) return;
        Window window = dialog.getWindow();
        if (window == null) return;
        int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.85);
        window.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);
        window.setGravity(android.view.Gravity.CENTER);
        window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
        window.setDimAmount(0.55f);
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
    }

    private void dialNumber(String phoneNumber) {
        Intent dialIntent = new Intent(Intent.ACTION_DIAL,
                Uri.parse("tel:" + phoneNumber));
        startActivity(dialIntent);
    }
}
