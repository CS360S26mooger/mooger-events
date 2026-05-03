package com.example.moogerscouncil;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AvailabilitySetupActivity extends AppCompatActivity {

    private RecyclerView recyclerSlots;
    private TextView textEmptySlots;
    private AvailabilityRepository availabilityRepository;
    private String counselorId;

    private final List<TimeSlot> slotList = new ArrayList<>();
    private SlotGroupAdapter adapter;

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

        adapter = new SlotGroupAdapter(this::onDeleteSlotTapped);
        recyclerSlots.setLayoutManager(new LinearLayoutManager(this));
        recyclerSlots.setAdapter(adapter);

        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());
        findViewById(R.id.buttonAddSlot).setOnClickListener(v ->
                startActivity(new Intent(this, AddSlotActivity.class)));
        findViewById(R.id.buttonAvailabilitySettings).setOnClickListener(v ->
                startActivity(new Intent(this, AvailabilitySettingsActivity.class)));
        findViewById(R.id.buttonGenerateSlots).setOnClickListener(v ->
                startActivity(new Intent(this, GenerateSlotsActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSlots();
    }

    private void loadSlots() {
        availabilityRepository.getSlotsForCounselor(counselorId,
                new AvailabilityRepository.OnSlotsLoadedCallback() {
                    @Override
                    public void onSuccess(List<TimeSlot> slots) {
                        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US)
                                .format(new Date());
                        slotList.clear();
                        for (TimeSlot s : slots) {
                            if (s.getDate() == null) continue;
                            boolean isPast = s.getDate().compareTo(today) < 0;
                            if (isPast && s.isAvailable()) continue;
                            slotList.add(s);
                        }
                        Collections.sort(slotList, (a, b) -> {
                            int d = a.getDate().compareTo(b.getDate());
                            return d != 0 ? d : a.getTime().compareTo(b.getTime());
                        });
                        adapter.setSlots(slotList);
                        textEmptySlots.setVisibility(slotList.isEmpty() ? View.VISIBLE : View.GONE);
                        recyclerSlots.setVisibility(slotList.isEmpty() ? View.GONE : View.VISIBLE);
                    }
                    @Override
                    public void onFailure(Exception e) {
                        AppToast.show(AvailabilitySetupActivity.this,
                                getString(R.string.error_adding_slot), AppToast.LENGTH_SHORT);
                    }
                });
    }

    private void onDeleteSlotTapped(TimeSlot slot) {
        if (slot.isAvailable()) {
            showConfirmDeleteDialog(slot);
        } else {
            showBookedSlotDialog();
        }
    }

    private void showConfirmDeleteDialog(TimeSlot slot) {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_confirm_action);
        Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawableResource(android.R.color.transparent);
            WindowManager.LayoutParams p = w.getAttributes();
            p.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.88f);
            p.gravity = Gravity.CENTER;
            w.setAttributes(p);
        }
        ImageView icon = dialog.findViewById(R.id.dialogIcon);
        icon.setImageResource(R.drawable.ic_trash);
        icon.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFEBEE")));
        icon.setColorFilter(Color.parseColor("#C62828"));
        ((TextView) dialog.findViewById(R.id.dialogTitle)).setText("Remove Slot?");
        ((TextView) dialog.findViewById(R.id.dialogBody))
                .setText("This availability slot will be permanently removed and students will no longer be able to book it.");
        MaterialButton btnConfirm = dialog.findViewById(R.id.btnConfirm);
        btnConfirm.setText("Yes, remove");
        btnConfirm.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#C62828")));
        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            doDeleteSlot(slot);
        });
        dialog.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void doDeleteSlot(TimeSlot slot) {
        // Optimistic update: remove immediately so the UI feels instant.
        slotList.remove(slot);
        adapter.removeSlotById(slot.getId());
        textEmptySlots.setVisibility(slotList.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerSlots.setVisibility(slotList.isEmpty() ? View.GONE : View.VISIBLE);

        availabilityRepository.removeSlot(counselorId, slot.getId(),
                new AvailabilityRepository.OnSlotActionCallback() {
                    @Override
                    public void onSuccess() {
                        SessionCache.getInstance().invalidateSlots(counselorId);
                        AppToast.show(AvailabilitySetupActivity.this,
                                getString(R.string.slot_removed), AppToast.LENGTH_SHORT);
                    }
                    @Override
                    public void onFailure(Exception e) {
                        // Rollback: reload the real state from Firestore.
                        AppToast.show(AvailabilitySetupActivity.this,
                                getString(R.string.error_removing_slot), AppToast.LENGTH_SHORT);
                        loadSlots();
                    }
                });
    }

    private void showBookedSlotDialog() {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_confirm_action);
        Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawableResource(android.R.color.transparent);
            WindowManager.LayoutParams p = w.getAttributes();
            p.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.88f);
            p.gravity = Gravity.CENTER;
            w.setAttributes(p);
        }
        ImageView icon = dialog.findViewById(R.id.dialogIcon);
        icon.setImageResource(R.drawable.ic_nav_calendar);
        icon.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFF0F5")));
        icon.setColorFilter(Color.parseColor("#C96B8E"));
        ((TextView) dialog.findViewById(R.id.dialogTitle)).setText("Slot Already Booked");
        ((TextView) dialog.findViewById(R.id.dialogBody))
                .setText("A student is booked for this slot. To free it, cancel their appointment from the appointments tab first.");
        MaterialButton btnConfirm = dialog.findViewById(R.id.btnConfirm);
        btnConfirm.setText("OK");
        btnConfirm.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#C96B8E")));
        btnConfirm.setOnClickListener(v -> dialog.dismiss());
        dialog.findViewById(R.id.btnCancel).setVisibility(View.GONE);
        dialog.show();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Multi-view-type adapter: TYPE_HEADER (date) + TYPE_SLOT (TimeSlot card)
    // ──────────────────────────────────────────────────────────────────────────

    interface OnSlotActionListener {
        void onDelete(TimeSlot slot);
    }

    private static class SlotGroupAdapter
            extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int TYPE_HEADER = 0;
        private static final int TYPE_SLOT   = 1;

        private final List<Object> items = new ArrayList<>();
        private final OnSlotActionListener listener;

        SlotGroupAdapter(OnSlotActionListener listener) {
            this.listener = listener;
        }

        void setSlots(List<TimeSlot> slots) {
            items.clear();
            String lastDate = null;
            for (TimeSlot slot : slots) {
                if (!slot.getDate().equals(lastDate)) {
                    items.add(slot.getDate());
                    lastDate = slot.getDate();
                }
                items.add(slot);
            }
            notifyDataSetChanged();
        }

        void removeSlotById(String slotId) {
            // Find the slot position.
            int slotPos = -1;
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i) instanceof TimeSlot
                        && ((TimeSlot) items.get(i)).getId().equals(slotId)) {
                    slotPos = i;
                    break;
                }
            }
            if (slotPos == -1) return;

            items.remove(slotPos);
            notifyItemRemoved(slotPos);

            // If the preceding header now has no slot siblings, remove it too.
            int headerPos = slotPos - 1;
            if (headerPos >= 0 && items.get(headerPos) instanceof String) {
                boolean hasNextSlot = slotPos < items.size()
                        && items.get(slotPos) instanceof TimeSlot
                        && ((TimeSlot) items.get(slotPos)).getDate()
                                .equals(items.get(headerPos));
                if (!hasNextSlot) {
                    items.remove(headerPos);
                    notifyItemRemoved(headerPos);
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            return items.get(position) instanceof String ? TYPE_HEADER : TYPE_SLOT;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == TYPE_HEADER) {
                View v = inflater.inflate(R.layout.item_slot_date_header, parent, false);
                return new HeaderViewHolder(v);
            }
            View v = inflater.inflate(R.layout.item_slot_counselor, parent, false);
            return new SlotViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof HeaderViewHolder) {
                ((HeaderViewHolder) holder).bind((String) items.get(position));
            } else {
                ((SlotViewHolder) holder).bind((TimeSlot) items.get(position), listener);
            }
        }

        @Override
        public int getItemCount() { return items.size(); }

        // ── Header ViewHolder ───────────────────────────────────────────────
        static class HeaderViewHolder extends RecyclerView.ViewHolder {
            final TextView textDate;
            HeaderViewHolder(View v) {
                super(v);
                textDate = v.findViewById(R.id.textDateHeader);
            }
            void bind(String rawDate) {
                try {
                    Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(rawDate);
                    textDate.setText(new SimpleDateFormat("EEE, MMM d", Locale.US).format(d));
                } catch (ParseException e) {
                    textDate.setText(rawDate);
                }
            }
        }

        // ── Slot ViewHolder ─────────────────────────────────────────────────
        static class SlotViewHolder extends RecyclerView.ViewHolder {
            final TextView textTime;
            final TextView textStatus;
            final ImageButton buttonDelete;

            SlotViewHolder(View v) {
                super(v);
                textTime     = v.findViewById(R.id.textSlotTime);
                textStatus   = v.findViewById(R.id.textSlotStatus);
                buttonDelete = v.findViewById(R.id.buttonDeleteSlot);
            }

            void bind(TimeSlot slot, OnSlotActionListener listener) {
                textTime.setText(slot.getTime());

                if (slot.isAvailable()) {
                    textStatus.setText("Available");
                    textStatus.setTextColor(Color.parseColor("#3A7D5A"));
                    textStatus.setBackgroundTintList(
                            ColorStateList.valueOf(Color.parseColor("#E8F5EF")));
                    buttonDelete.setColorFilter(Color.parseColor("#C62828"));
                    buttonDelete.setBackgroundTintList(
                            ColorStateList.valueOf(Color.parseColor("#FFEBEE")));
                } else {
                    textStatus.setText("Booked");
                    textStatus.setTextColor(Color.parseColor("#8B6BAE"));
                    textStatus.setBackgroundTintList(
                            ColorStateList.valueOf(Color.parseColor("#EDE7F6")));
                    buttonDelete.setColorFilter(Color.parseColor("#9E9E9E"));
                    buttonDelete.setBackgroundTintList(
                            ColorStateList.valueOf(Color.parseColor("#F5F5F5")));
                }
                buttonDelete.setOnClickListener(v -> listener.onDelete(slot));
            }
        }
    }
}
