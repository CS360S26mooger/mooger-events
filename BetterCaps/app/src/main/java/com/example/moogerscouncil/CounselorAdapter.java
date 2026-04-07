/*
 * CounselorAdapter.java
 * Role: RecyclerView adapter for displaying counselor cards in the directory.
 *       Each card shows name, specializations, language, and an on-leave badge
 *       if applicable. Tapping a card navigates to CounselorProfileActivity.
 *
 * Design pattern: Adapter pattern (RecyclerView).
 * Part of the BetterCAPS counseling platform.
 */
package com.example.moogerscouncil;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * RecyclerView adapter for the counselor directory list.
 * Displays name, specializations, language, and an on-leave badge per card.
 * Card taps route to {@link CounselorProfileActivity}; on-leave counselors
 * have the "View Profile" button disabled.
 */
public class CounselorAdapter extends RecyclerView.Adapter<CounselorAdapter.ViewHolder> {

    private List<Counselor> counselorList;
    private final Context context;

    /**
     * Creates a new adapter.
     *
     * @param context      The Activity context used for inflation and Intent creation.
     * @param counselorList The initial list of counselors to display.
     */
    public CounselorAdapter(Context context, List<Counselor> counselorList) {
        this.context = context;
        this.counselorList = counselorList;
    }

    /**
     * Replaces the current counselor list and refreshes the RecyclerView.
     * Used by the filter logic in {@link CounselorListActivity}.
     *
     * @param newList The filtered list of counselors to display.
     */
    public void setData(List<Counselor> newList) {
        this.counselorList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_counselor, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Counselor counselor = counselorList.get(position);

        holder.nameText.setText(counselor.getName());

        // Specializations — join as readable string
        if (counselor.getSpecializations() != null && !counselor.getSpecializations().isEmpty()) {
            holder.specializationText.setText(
                    android.text.TextUtils.join(" · ", counselor.getSpecializations()));
        } else {
            holder.specializationText.setText("");
        }

        // Language
        if (counselor.getLanguage() != null && !counselor.getLanguage().isEmpty()) {
            holder.languageText.setVisibility(View.VISIBLE);
            holder.languageText.setText(counselor.getLanguage());
        } else {
            holder.languageText.setVisibility(View.GONE);
        }

        // On-leave badge — show "Currently Away" chip but still allow profile navigation.
        // The profile itself handles disabling the book button and showing the leave card.
        boolean isOnLeave = Boolean.TRUE.equals(counselor.getOnLeave());
        if (isOnLeave) {
            holder.badgeOnLeave.setVisibility(View.VISIBLE);
            holder.badgeOnLeave.setText(R.string.currently_away);
        } else {
            holder.badgeOnLeave.setVisibility(View.GONE);
        }
        holder.viewProfileButton.setEnabled(true);
        holder.viewProfileButton.setText(R.string.view_profile);

        // Card tap → CounselorProfileActivity (on-leave counselors can still be viewed)
        View.OnClickListener profileClickListener = v -> {
            Intent intent = new Intent(context, CounselorProfileActivity.class);
            intent.putExtra("COUNSELOR_ID", counselor.getId());
            intent.putExtra("COUNSELOR_NAME", counselor.getName());
            context.startActivity(intent);
        };

        holder.itemView.setOnClickListener(profileClickListener);
        holder.viewProfileButton.setOnClickListener(profileClickListener);
    }

    @Override
    public int getItemCount() {
        return counselorList.size();
    }

    /**
     * ViewHolder for a single counselor card.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText;
        TextView specializationText;
        TextView languageText;
        TextView badgeOnLeave;
        Button viewProfileButton;

        /**
         * Binds view references from {@code item_counselor.xml}.
         *
         * @param itemView The inflated card view.
         */
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.counselorName);
            specializationText = itemView.findViewById(R.id.counselorSpecializations);
            languageText = itemView.findViewById(R.id.counselorLanguage);
            badgeOnLeave = itemView.findViewById(R.id.badgeOnLeave);
            viewProfileButton = itemView.findViewById(R.id.viewSlotsButton);
        }
    }
}
