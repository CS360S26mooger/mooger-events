/*
 * StudentWaitlistActivity.java
 * Role: Displays a student's active waitlist requests.
 *       Students can cancel requests from this screen via a styled confirmation dialog.
 *
 * Design pattern: Activity backed by WaitlistRepository and CounselorRepository.
 * Part of the BetterCAPS counseling platform.
 */
package com.example.moogerscouncil;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Shows the student's active waitlist requests only.
 * Counselor display names are resolved before rendering so the list never shows raw UIDs.
 */
public class StudentWaitlistActivity extends AppCompatActivity {

    private RecyclerView recycler;
    private TextView textEmpty;
    private WaitlistRepository waitlistRepository;
    private CounselorRepository counselorRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_waitlist);

        waitlistRepository = new WaitlistRepository();
        counselorRepository = new CounselorRepository();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        recycler = findViewById(R.id.recyclerStudentWaitlist);
        textEmpty = findViewById(R.id.textWaitlistEmpty);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        loadEntries();
    }

    private void loadEntries() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;

        waitlistRepository.getActiveWaitlistForStudent(uid,
                new WaitlistRepository.OnWaitlistLoadedCallback() {
                    @Override
                    public void onSuccess(List<WaitlistEntry> entries) {
                        if (entries.isEmpty()) {
                            textEmpty.setVisibility(View.VISIBLE);
                            recycler.setVisibility(View.GONE);
                            return;
                        }
                        textEmpty.setVisibility(View.GONE);
                        recycler.setVisibility(View.VISIBLE);
                        resolveCounselorNames(entries);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        AppToast.show(StudentWaitlistActivity.this,
                                R.string.waitlist_load_error, AppToast.LENGTH_SHORT);
                    }
                });
    }

    private void resolveCounselorNames(List<WaitlistEntry> entries) {
        Map<String, String> names = new HashMap<>();
        AtomicInteger remaining = new AtomicInteger(entries.size());

        for (WaitlistEntry entry : entries) {
            String counselorId = entry.getCounselorId();
            counselorRepository.getCounselor(counselorId,
                    new CounselorRepository.OnCounselorFetchedCallback() {
                        @Override
                        public void onSuccess(Counselor counselor) {
                            String n = counselor.getName();
                            names.put(counselorId, n != null ? n : counselorId);
                            if (remaining.decrementAndGet() == 0) showList(entries, names);
                        }

                        @Override
                        public void onFailure(Exception e) {
                            names.put(counselorId, counselorId);
                            if (remaining.decrementAndGet() == 0) showList(entries, names);
                        }
                    });
        }
    }

    private void showList(List<WaitlistEntry> entries, Map<String, String> names) {
        WaitlistAdapter adapter = new WaitlistAdapter(
                this, entries, names, this::confirmCancel);
        recycler.setAdapter(adapter);
    }

    private void confirmCancel(WaitlistEntry entry) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_waitlist_cancel);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.85),
                    WindowManager.LayoutParams.WRAP_CONTENT);
            window.setGravity(android.view.Gravity.CENTER);
            window.setDimAmount(0.55f);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }

        MaterialButton btnConfirm = dialog.findViewById(R.id.btnCancelConfirm);
        MaterialButton btnDismiss = dialog.findViewById(R.id.btnCancelDismiss);

        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            cancelEntry(entry);
        });
        btnDismiss.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void cancelEntry(WaitlistEntry entry) {
        waitlistRepository.cancelEntry(entry.getId(),
                new WaitlistRepository.OnWaitlistSimpleCallback() {
                    @Override
                    public void onSuccess() {
                        AppToast.show(StudentWaitlistActivity.this,
                                R.string.waitlist_cancel_success, AppToast.LENGTH_SHORT);
                        loadEntries();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        AppToast.show(StudentWaitlistActivity.this,
                                R.string.waitlist_cancel_error, AppToast.LENGTH_SHORT);
                    }
                });
    }
}
