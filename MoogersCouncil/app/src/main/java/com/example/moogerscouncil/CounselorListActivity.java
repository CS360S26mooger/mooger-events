package com.example.moogerscouncil;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.example.moogerscouncil.Counselor;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays a searchable list of available counselors fetched from Firestore.
 * Students tap View Slots to proceed to booking.
 */
public class CounselorListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private FirebaseFirestore db;
    private List<Counselor> counselorList;
    private CounselorAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_counselor_list);

        db = FirebaseFirestore.getInstance();
        counselorList = new ArrayList<>();

        recyclerView = findViewById(R.id.counselorRecyclerView);
        progressBar = findViewById(R.id.progressBar);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CounselorAdapter(this, counselorList);
        recyclerView.setAdapter(adapter);

        loadCounselors();
    }

    private void loadCounselors() {
        progressBar.setVisibility(View.VISIBLE);

        db.collection("counselors")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    progressBar.setVisibility(View.GONE);
                    counselorList.clear();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Counselor counselor = doc.toObject(Counselor.class);
                        counselor.setId(doc.getId());
                        counselorList.add(counselor);
                    }

                    adapter.notifyDataSetChanged();

                    if (counselorList.isEmpty()) {
                        Toast.makeText(this,
                                "No counselors found. Add some in Firebase console.",
                                Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this,
                            "Failed to load counselors: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}