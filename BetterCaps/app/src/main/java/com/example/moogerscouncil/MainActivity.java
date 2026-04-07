package com.example.moogerscouncil;

import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Main entry point of the app.
 * Currently tests Firebase connectivity on launch.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Test Firebase connection
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("counselors")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Log.d(TAG, "Firebase connected! Docs: " + querySnapshot.size());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firebase failed: " + e.getMessage());
                });
    }
}