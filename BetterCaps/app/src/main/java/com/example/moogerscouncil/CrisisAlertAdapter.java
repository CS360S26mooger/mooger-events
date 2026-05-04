/*
 * CrisisAlertAdapter.java
 * Role: RecyclerView adapter for the admin's Crisis Alerts tab.
 *       Each row shows severity badge, counselor name (async lookup), student name
 *       (async lookup), date filed, action taken, notes, and a "Mark Resolved" button
 *       that removes the item from the list on success.
 *
 * Design pattern: Adapter pattern (RecyclerView); Repository pattern for resolve action.
 * Part of the BetterCAPS counseling platform — Sprint 11 admin crisis visibility.
 */
package com.example.moogerscouncil;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for the admin Crisis Alerts tab.
 * Binds each {@link CrisisEscalation} to a card row with severity colouring and async name lookups.
 */
public class CrisisAlertAdapter extends RecyclerView.Adapter<CrisisAlertAdapter.ViewHolder> {

    private final List<CrisisEscalation> items = new ArrayList<>();
    private final CrisisEscalationRepository repository = new CrisisEscalationRepository();
    private final UserRepository userRepository = new UserRepository();

    /** Replaces the dataset and notifies the adapter. */
    public void setData(List<CrisisEscalation> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_crisis_alert, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CrisisEscalation e = items.get(position);
        applySeverityStyle(holder, e.getSeverity());
        holder.textDate.setText(formatTimestamp(e));
        holder.textAction.setText(humanReadableAction(holder, e.getActionTaken()));
        holder.textNotes.setText(e.getNotes() != null ? e.getNotes() : "");
        holder.textNotes.setVisibility(
                e.getNotes() == null || e.getNotes().isEmpty() ? View.GONE : View.VISIBLE);

        holder.textCounselor.setText(
                holder.itemView.getContext().getString(R.string.crisis_alert_counselor_loading));
        holder.textStudent.setText(
                holder.itemView.getContext().getString(R.string.crisis_alert_student_loading));

        if (e.getCounselorId() != null) {
            userRepository.getUserName(e.getCounselorId(), name -> {
                if (holder.getBindingAdapterPosition() == RecyclerView.NO_POSITION) return;
                holder.textCounselor.setText(
                        holder.itemView.getContext()
                                .getString(R.string.crisis_alert_counselor, name));
            });
        }
        if (e.getStudentId() != null) {
            userRepository.getUserName(e.getStudentId(), name -> {
                if (holder.getBindingAdapterPosition() == RecyclerView.NO_POSITION) return;
                holder.textStudent.setText(
                        holder.itemView.getContext()
                                .getString(R.string.crisis_alert_student, name));
            });
        }

        holder.btnResolve.setOnClickListener(v -> resolveEscalation(e, position, holder));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void resolveEscalation(CrisisEscalation e, int position, ViewHolder holder) {
        holder.btnResolve.setEnabled(false);
        repository.markResolved(e.getId(), new CrisisEscalationRepository.OnResolveCallback() {
            @Override
            public void onSuccess() {
                int pos = holder.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    items.remove(pos);
                    notifyItemRemoved(pos);
                }
                AppToast.show(holder.itemView.getContext(),
                        R.string.crisis_resolve_success, AppToast.LENGTH_SHORT);
            }

            @Override
            public void onFailure(Exception ex) {
                holder.btnResolve.setEnabled(true);
                AppToast.show(holder.itemView.getContext(),
                        R.string.crisis_resolve_error, AppToast.LENGTH_SHORT);
            }
        });
    }

    private void applySeverityStyle(ViewHolder holder, String severity) {
        int stripColor;
        int badgeText;
        int badgeBg;
        String label;
        if (CrisisEscalation.SEVERITY_IMMEDIATE.equals(severity)) {
            stripColor = Color.parseColor("#D32F2F");
            badgeText  = Color.parseColor("#D32F2F");
            badgeBg    = Color.parseColor("#FFEBEE");
            label = "IMMEDIATE";
        } else if (CrisisEscalation.SEVERITY_HIGH.equals(severity)) {
            stripColor = Color.parseColor("#E8761A");
            badgeText  = Color.parseColor("#E8761A");
            badgeBg    = Color.parseColor("#FFF3E8");
            label = "HIGH";
        } else {
            stripColor = Color.parseColor("#F57F17");
            badgeText  = Color.parseColor("#F57F17");
            badgeBg    = Color.parseColor("#FFFDE7");
            label = "MODERATE";
        }
        holder.viewStrip.setBackgroundColor(stripColor);
        holder.textSeverity.setText(label);
        holder.textSeverity.setTextColor(badgeText);
        ViewCompat.setBackgroundTintList(holder.textSeverity,
                ColorStateList.valueOf(badgeBg));
    }

    private static String formatTimestamp(CrisisEscalation e) {
        if (e.getCreatedAt() == null) return "—";
        return new SimpleDateFormat("MMM d, HH:mm", Locale.US)
                .format(e.getCreatedAt().toDate());
    }

    private static String humanReadableAction(ViewHolder holder, String action) {
        if (action == null) return "";
        switch (action) {
            case CrisisEscalation.ACTION_CALLED_SECURITY:
                return holder.itemView.getContext().getString(R.string.crisis_action_security);
            case CrisisEscalation.ACTION_REFERRED_CAPS:
                return holder.itemView.getContext().getString(R.string.crisis_action_caps);
            case CrisisEscalation.ACTION_SAFETY_PLAN:
                return holder.itemView.getContext().getString(R.string.crisis_action_safety_plan);
            default:
                return holder.itemView.getContext().getString(R.string.crisis_action_other);
        }
    }

    // ── ViewHolder ────────────────────────────────────────────────────────────

    static class ViewHolder extends RecyclerView.ViewHolder {
        View viewStrip;
        TextView textSeverity, textDate, textCounselor, textStudent, textAction, textNotes;
        MaterialButton btnResolve;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            viewStrip      = itemView.findViewById(R.id.viewSeverityStrip);
            textSeverity   = itemView.findViewById(R.id.textCrisisAlertSeverity);
            textDate       = itemView.findViewById(R.id.textCrisisAlertDate);
            textCounselor  = itemView.findViewById(R.id.textCrisisAlertCounselor);
            textStudent    = itemView.findViewById(R.id.textCrisisAlertStudent);
            textAction     = itemView.findViewById(R.id.textCrisisAlertAction);
            textNotes      = itemView.findViewById(R.id.textCrisisAlertNotes);
            btnResolve     = itemView.findViewById(R.id.btnMarkResolved);
        }
    }
}
