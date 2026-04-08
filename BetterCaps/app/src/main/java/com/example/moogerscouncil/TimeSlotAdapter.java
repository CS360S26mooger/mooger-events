package com.example.moogerscouncil;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TimeSlotAdapter extends RecyclerView.Adapter<TimeSlotAdapter.ViewHolder> {

    private List<TimeSlot> slots;
    private OnBookClickListener listener;

    public interface OnBookClickListener {
        void onBookClick(TimeSlot slot);
    }

    public TimeSlotAdapter(List<TimeSlot> slots, OnBookClickListener listener) {
        this.slots = slots;
        this.listener = listener;
    }

    /** Replaces the slot list and refreshes the RecyclerView. */
    public void setData(List<TimeSlot> newSlots) {
        this.slots = newSlots;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_timeslot, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TimeSlot slot = slots.get(position);
        holder.dateText.setText(formatDate(slot.getDate()));
        holder.timeText.setText(slot.getTime());

        if (slot.isAvailable()) {
            holder.bookButton.setEnabled(true);
            holder.bookButton.setText("Book");
            holder.bookButton.setAlpha(1f);
            holder.bookButton.setOnClickListener(v -> listener.onBookClick(slot));
        } else {
            holder.bookButton.setEnabled(false);
            holder.bookButton.setText("Booked");
            holder.bookButton.setAlpha(0.5f);
        }
    }

    private static String formatDate(String raw) {
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(raw);
            return new SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(d);
        } catch (ParseException e) {
            return raw;
        }
    }

    @Override
    public int getItemCount() {
        return slots.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView dateText, timeText;
        MaterialButton bookButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            dateText = itemView.findViewById(R.id.slotDate);
            timeText = itemView.findViewById(R.id.slotTime);
            bookButton = itemView.findViewById(R.id.bookButton);
        }
    }
}