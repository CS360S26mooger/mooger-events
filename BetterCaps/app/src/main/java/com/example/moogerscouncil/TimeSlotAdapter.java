package com.example.moogerscouncil;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

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
        holder.dateText.setText(slot.getDate());
        holder.timeText.setText(slot.getTime());

        if (slot.isAvailable()) {
            holder.bookButton.setEnabled(true);
            holder.bookButton.setText("Book");
            holder.bookButton.setOnClickListener(v -> listener.onBookClick(slot));
        } else {
            holder.bookButton.setEnabled(false);
            holder.bookButton.setText("Booked");
        }
    }

    @Override
    public int getItemCount() {
        return slots.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView dateText, timeText;
        Button bookButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            dateText = itemView.findViewById(R.id.slotDate);
            timeText = itemView.findViewById(R.id.slotTime);
            bookButton = itemView.findViewById(R.id.bookButton);
        }
    }
}