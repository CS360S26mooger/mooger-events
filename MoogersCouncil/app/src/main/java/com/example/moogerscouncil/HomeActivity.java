package com.example.moogerscouncil;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.cardview.widget.CardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Home screen shown after successful login.
 * Displays upcoming sessions, crisis support, and navigation.
 */
public class HomeActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mAuth = FirebaseAuth.getInstance();

        // Redirect to login if not authenticated
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Crisis banner
        CardView crisisBanner = findViewById(R.id.crisisBanner);
        crisisBanner.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("🚨 Crisis Support")
                    .setMessage("Umang helpline: 0317-4288665\nRozan Counseling: 051-2890505\nLUMS CAPS: 042-35608000")
                    .setPositiveButton("Call Now", null)
                    .setNegativeButton("Dismiss", null)
                    .show();
        });

        // Find My Match → goes to counselor list for now
        CardView findMatchCard = findViewById(R.id.findMatchCard);
        findMatchCard.setOnClickListener(v ->
                startActivity(new Intent(this, CounselorListActivity.class))
        );

        // AI Chat card → placeholder
        CardView aiChatCard = findViewById(R.id.aiChatCard);
        aiChatCard.setOnClickListener(v ->
                Toast.makeText(this, "AI Chat coming soon!", Toast.LENGTH_SHORT).show()
        );

        // Slide to cancel → placeholder
        CardView slideToCancelBtn = findViewById(R.id.slideToCancelBtn);
        slideToCancelBtn.setOnClickListener(v ->
                Toast.makeText(this, "No active booking to cancel", Toast.LENGTH_SHORT).show()
        );

        // Logout
        ImageButton navLogout = findViewById(R.id.navLogout);
        navLogout.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        // Discreet mode button
        ImageButton discreetBtn = findViewById(R.id.discreetModeBtn);
        discreetBtn.setOnClickListener(v ->
                Toast.makeText(this, "Discreet mode coming soon!", Toast.LENGTH_SHORT).show()
        );
    }
}