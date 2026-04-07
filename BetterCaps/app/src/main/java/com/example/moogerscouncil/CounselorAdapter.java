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
import com.example.moogerscouncil.Counselor;
import java.util.List;

/**
 * RecyclerView adapter for displaying the list of counselors.
 * Each item shows name, specializations, and a View Slots button.
 */
public class CounselorAdapter extends RecyclerView.Adapter<CounselorAdapter.ViewHolder> {

    private List<Counselor> counselorList;
    private Context context;

    public CounselorAdapter(Context context, List<Counselor> counselorList) {
        this.context = context;
        this.counselorList = counselorList;
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

        // Join specializations into one string
        if (counselor.getSpecializations() != null && !counselor.getSpecializations().isEmpty()) {
            holder.specializationText.setText(
                    android.text.TextUtils.join(" & ", counselor.getSpecializations())
            );
        }

        holder.nextSlotText.setText("Next: Available");
        holder.ratingText.setText("★ 4.9");

        holder.viewSlotsButton.setOnClickListener(v -> {
            Intent intent = new Intent(context, BookingActivity.class);
            intent.putExtra("counselorId", counselor.getId());
            intent.putExtra("counselorName", counselor.getName());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return counselorList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, specializationText, nextSlotText, ratingText;
        Button viewSlotsButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.counselorName);
            specializationText = itemView.findViewById(R.id.counselorSpecializations);
            nextSlotText = itemView.findViewById(R.id.counselorNextSlot);
            ratingText = itemView.findViewById(R.id.counselorRating);
            viewSlotsButton = itemView.findViewById(R.id.viewSlotsButton);
        }
    }
}