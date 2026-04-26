/*
 * AvailabilitySetupActivity.java
 * Role: Dedicated counselor screen for managing time slots — viewing, adding, and
 *       removing. Replaces the inline DatePicker/TimePicker in CounselorDashboardActivity.
 *       All Firestore operations go through AvailabilityRepository.
 *
 * Design pattern: Repository pattern (AvailabilityRepository).
 * Part of the BetterCAPS counseling platform.
 */
package com.example.moogerscouncil;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Screen where a counselor can view their existing time slots, add new ones
 * via date/time pickers, and swipe-to-delete available slots.
 *
 * <p>Launched from {@link CounselorDashboardActivity} via the "Add Availability Slot" banner.</p>
 */
public class AvailabilitySetupActivity extends AppCompatActivity {

    private RecyclerView recyclerSlots;
    private TextView textEmptySlots;
    private AvailabilityRepository availabilityRepository;
    private String counselorId;

    /** Flat list of all slots (available + booked) for display. */
    private final List<TimeSlot> slotList = new ArrayList<>();
    private TimeSlotSetupAdapter slotSetupAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_availability_setup);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        counselorId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        availabilityRepository = new AvailabilityRepository();

        recyclerSlots = findViewById(R.id.recyclerSlots);
        textEmptySlots = findViewById(R.id.textEmptySlots);
        MaterialButton buttonAddSlot = findViewById(R.id.buttonAddSlot);

        slotSetupAdapter = new TimeSlotSetupAdapter(slotList);
        recyclerSlots.setLayoutManager(new LinearLayoutManager(this));
        recyclerSlots.setAdapter(slotSetupAdapter);

        attachSwipeToDelete();

        buttonAddSlot.setOnClickListener(v -> showDatePicker());

        loadSlots();
    }

    /**
     * Fetches all slots (available and booked) for this counselor from Firestore.
     */
    private void loadSlots() {
        availabilityRepository.getSlotsForCounselor(counselorId,
                new AvailabilityRepository.OnSlotsLoadedCallback() {
                    @Override
                    public void onSuccess(List<TimeSlot> slots) {
                        slotList.clear();
                        slotList.addAll(slots);
                        slotSetupAdapter.notifyDataSetChanged();
                        textEmptySlots.setVisibility(
                                slots.isEmpty() ? View.VISIBLE : View.GONE);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(AvailabilitySetupActivity.this,
                                getString(R.string.error_adding_slot),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Shows a DatePickerDialog, then chained TimePickerDialog, then calls
     * {@link AvailabilityRepository#addSlot} on confirmation.
     */
    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            String date = String.format(java.util.Locale.US, "%04d-%02d-%02d",
                    year, month + 1, day);
            new TimePickerDialog(this, (tView, hour, minute) -> {
                String time = String.format(java.util.Locale.US, "%02d:%02d", hour, minute);
                addSlot(date, time);
            }, calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE), true).show();
        }, calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    /**
     * Persists a new slot to Firestore and refreshes the list on success.
     *
     * @param date The slot date in "yyyy-MM-dd" format.
     * @param time The slot time in "HH:mm" format.
     */
    private void addSlot(String date, String time) {
        availabilityRepository.addSlot(counselorId, date, time,
                new AvailabilityRepository.OnSlotActionCallback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(AvailabilitySetupActivity.this,
                                getString(R.string.slot_added), Toast.LENGTH_SHORT).show();
                        loadSlots();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(AvailabilitySetupActivity.this,
                                getString(R.string.error_adding_slot), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Attaches a swipe-left ItemTouchHelper to delete available slots.
     * Booked (unavailable) slots are not deletable — swipe is ignored.
     */
    private void attachSwipeToDelete() {
        ItemTouchHelper.SimpleCallback swipeCallback =
                new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
                    @Override
                    public boolean onMove(@androidx.annotation.NonNull RecyclerView rv,
                                         @androidx.annotation.NonNull RecyclerView.ViewHolder vh,
                                         @androidx.annotation.NonNull RecyclerView.ViewHolder target) {
                        return false;
                    }

                    @Override
                    public void onSwiped(@androidx.annotation.NonNull RecyclerView.ViewHolder vh,
                                        int direction) {
                        int pos = vh.getAdapterPosition();
                        TimeSlot slot = slotList.get(pos);

                        if (!slot.isAvailable()) {
                            // Restore — booked slots cannot be deleted
                            slotSetupAdapter.notifyItemChanged(pos);
                            Toast.makeText(AvailabilitySetupActivity.this,
                                    "Cannot remove a booked slot", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        availabilityRepository.removeSlot(counselorId, slot.getId(),
                                new AvailabilityRepository.OnSlotActionCallback() {
                                    @Override
                                    public void onSuccess() {
                                        slotList.remove(pos);
                                        slotSetupAdapter.notifyItemRemoved(pos);
                                        textEmptySlots.setVisibility(
                                                slotList.isEmpty() ? View.VISIBLE : View.GONE);
                                        Toast.makeText(AvailabilitySetupActivity.this,
                                                getString(R.string.slot_removed),
                                                Toast.LENGTH_SHORT).show();
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        slotSetupAdapter.notifyItemChanged(pos);
                                        Toast.makeText(AvailabilitySetupActivity.this,
                                                getString(R.string.error_removing_slot),
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                };

        new ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerSlots);
    }

    /**
     * Simple inline adapter for displaying slot date, time, and availability status.
     * Used only within AvailabilitySetupActivity.
     */
    private static class TimeSlotSetupAdapter
            extends RecyclerView.Adapter<TimeSlotSetupAdapter.SlotViewHolder> {

        private final List<TimeSlot> slots;

        TimeSlotSetupAdapter(List<TimeSlot> slots) {
            this.slots = slots;
        }

        @androidx.annotation.NonNull
        @Override
        public SlotViewHolder onCreateViewHolder(@androidx.annotation.NonNull android.view.ViewGroup parent,
                                                 int viewType) {
            android.view.View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_2, parent, false);
            return new SlotViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@androidx.annotation.NonNull SlotViewHolder holder,
                                     int position) {
            TimeSlot slot = slots.get(position);
            holder.text1.setText(slot.getDate() + "  " + slot.getTime());
            holder.text2.setText(slot.isAvailable() ? "Available" : "Booked");
        }

        @Override
        public int getItemCount() {
            return slots.size();
        }

        static class SlotViewHolder extends RecyclerView.ViewHolder {
            android.widget.TextView text1, text2;

            SlotViewHolder(android.view.View itemView) {
                super(itemView);
                text1 = itemView.findViewById(android.R.id.text1);
                text2 = itemView.findViewById(android.R.id.text2);
            }
        }
    }
}
