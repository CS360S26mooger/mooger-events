/*
 * BookingConfirmationFragment.java
 * Role: BottomSheetDialogFragment shown when a student taps "Book" on a time slot.
 *       Displays the booking summary (counselor, date, time) and a Confirm button
 *       that fires back to BookingActivity via the OnConfirmListener callback.
 *
 * Part of the BetterCAPS counseling platform.
 */
package com.example.moogerscouncil;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

/**
 * Bottom sheet that shows a booking summary before the student confirms.
 *
 * <p>Displays counselor name, date, and time. The "Confirm Booking" button
 * triggers the atomic Firestore transaction via a callback to the hosting
 * {@link BookingActivity}.</p>
 */
public class BookingConfirmationFragment extends BottomSheetDialogFragment {

    /**
     * Callback fired when the student taps "Confirm Booking".
     */
    public interface OnConfirmListener {
        /**
         * Called when the user confirms the booking.
         *
         * @param slot The {@link TimeSlot} that was confirmed.
         */
        void onConfirm(TimeSlot slot);
    }

    private static final String ARG_COUNSELOR_NAME = "counselorName";
    private static final String ARG_DATE = "date";
    private static final String ARG_TIME = "time";

    private TimeSlot slot;
    private OnConfirmListener listener;

    /**
     * Creates a new instance with the counselor name and slot details pre-filled.
     *
     * @param counselorName The counselor's display name for the summary.
     * @param slot          The {@link TimeSlot} the student is about to book.
     * @return A configured {@link BookingConfirmationFragment}.
     */
    public static BookingConfirmationFragment newInstance(String counselorName, TimeSlot slot) {
        BookingConfirmationFragment fragment = new BookingConfirmationFragment();
        Bundle args = new Bundle();
        args.putString(ARG_COUNSELOR_NAME, counselorName);
        args.putString(ARG_DATE, slot.getDate());
        args.putString(ARG_TIME, slot.getTime());
        fragment.setArguments(args);
        fragment.slot = slot;
        return fragment;
    }

    /**
     * Registers the confirmation callback. Must be called before
     * {@link #show(androidx.fragment.app.FragmentManager, String)}.
     *
     * @param listener The listener to fire on confirmation.
     */
    public void setOnConfirmListener(OnConfirmListener listener) {
        this.listener = listener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_booking_confirmation,
                container, false);

        TextView textCounselor = view.findViewById(R.id.textCounselorName);
        TextView textDate = view.findViewById(R.id.textDate);
        TextView textTime = view.findViewById(R.id.textTime);
        MaterialButton buttonConfirm = view.findViewById(R.id.buttonConfirmBooking);
        MaterialButton buttonCancel = view.findViewById(R.id.buttonCancelBooking);

        Bundle args = getArguments();
        if (args != null) {
            textCounselor.setText(args.getString(ARG_COUNSELOR_NAME, "—"));
            textDate.setText(args.getString(ARG_DATE, "—"));
            textTime.setText(args.getString(ARG_TIME, "—"));
        }

        buttonConfirm.setOnClickListener(v -> {
            if (listener != null && slot != null) {
                listener.onConfirm(slot);
            }
            dismiss();
        });

        buttonCancel.setOnClickListener(v -> dismiss());

        return view;
    }
}
