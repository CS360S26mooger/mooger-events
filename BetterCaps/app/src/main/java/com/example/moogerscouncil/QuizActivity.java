package com.example.moogerscouncil;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class QuizActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnMentalHealth).setOnClickListener(v -> startMatching("Mental Health"));
        findViewById(R.id.btnCareer).setOnClickListener(v -> startMatching("Career"));
        findViewById(R.id.btnAcademics).setOnClickListener(v -> startMatching("Academics"));
        findViewById(R.id.btnRelationships).setOnClickListener(v -> startMatching("Relationships"));
    }

    private void startMatching(String specialization) {
        Intent intent = new Intent(this, CounselorListActivity.class);
        intent.putExtra("SPECIALIZATION", specialization);
        startActivity(intent);
        finish();
    }
}