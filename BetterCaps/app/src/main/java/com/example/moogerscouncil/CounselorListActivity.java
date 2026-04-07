/*
 * CounselorListActivity.java
 * Role: Searchable, filterable directory of counselors for students.
 *       Loads all counselors from Firestore via CounselorRepository, then
 *       applies name search, multi-select specialization chip filters, and
 *       language/gender dropdown filters entirely client-side for responsiveness.
 *
 * Design pattern: Repository pattern (reads via CounselorRepository).
 * Part of the BetterCAPS counseling platform.
 */
package com.example.moogerscouncil;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Displays a searchable, filterable directory of all counselors.
 * Students can search by name, filter by specialization chips, language,
 * and gender. Tapping a card opens {@link CounselorProfileActivity}.
 */
public class CounselorListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView textEmptyState;
    private EditText searchField;
    private ChipGroup chipGroupSpecializations;
    private AutoCompleteTextView dropdownLanguage;
    private AutoCompleteTextView dropdownGender;

    private CounselorAdapter adapter;
    private CounselorRepository counselorRepository;

    /** Full unfiltered list fetched from Firestore — never mutated after load. */
    private List<Counselor> masterList = new ArrayList<>();

    /** Tracks all active filter state. */
    private final FilterState filterState = new FilterState();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_counselor_list);

        counselorRepository = new CounselorRepository();

        recyclerView = findViewById(R.id.counselorRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        textEmptyState = findViewById(R.id.textEmptyState);
        searchField = findViewById(R.id.searchField);
        chipGroupSpecializations = findViewById(R.id.chipGroupSpecializations);
        dropdownLanguage = findViewById(R.id.dropdownLanguage);
        dropdownGender = findViewById(R.id.dropdownGender);

        adapter = new CounselorAdapter(this, new ArrayList<>());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Handle incoming specialization intent extra from QuizActivity
        String intentSpec = getIntent().getStringExtra("SPECIALIZATION");
        if (intentSpec != null) {
            filterState.selectedSpecializations.add(intentSpec);
        }

        setupSearchBar();
        buildSpecializationChips(intentSpec);
        loadCounselors();
    }

    /**
     * Wires the search bar to trigger {@link #applyFilters()} on every keystroke.
     */
    private void setupSearchBar() {
        searchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                filterState.nameQuery = s.toString().trim().toLowerCase();
                applyFilters();
            }
        });
    }

    /**
     * Programmatically builds the specialization {@link ChipGroup} from
     * {@link SpecializationTags#ALL_TAGS}. Pre-selects any tag that matches
     * the intent filter or the initial filter state.
     *
     * @param preSelectedTag Tag to pre-check, or null for none.
     */
    private void buildSpecializationChips(String preSelectedTag) {
        chipGroupSpecializations.removeAllViews();

        for (String tag : SpecializationTags.ALL_TAGS) {
            Chip chip = new Chip(this);
            chip.setText(tag);
            chip.setCheckable(true);
            chip.setChecked(tag.equalsIgnoreCase(preSelectedTag)
                    || filterState.selectedSpecializations.contains(tag));
            chipGroupSpecializations.addView(chip);
        }

        chipGroupSpecializations.setOnCheckedStateChangeListener((group, checkedIds) -> {
            filterState.selectedSpecializations.clear();
            for (int id : checkedIds) {
                Chip chip = group.findViewById(id);
                if (chip != null) {
                    filterState.selectedSpecializations.add(chip.getText().toString());
                }
            }
            applyFilters();
        });
    }

    /**
     * Populates the language and gender dropdown adapters from unique values
     * found in the fetched counselor list.
     *
     * @param counselors The full counselor list used to derive unique values.
     */
    private void populateFilterDropdowns(List<Counselor> counselors) {
        TreeSet<String> languages = new TreeSet<>();
        TreeSet<String> genders = new TreeSet<>();
        for (Counselor c : counselors) {
            if (c.getLanguage() != null && !c.getLanguage().isEmpty()) {
                languages.add(c.getLanguage());
            }
            if (c.getGender() != null && !c.getGender().isEmpty()) {
                genders.add(c.getGender());
            }
        }

        final List<String> langList = new ArrayList<>();
        langList.add(getString(R.string.filter_all_languages));
        langList.addAll(languages);

        final List<String> genderList = new ArrayList<>();
        genderList.add(getString(R.string.filter_all_genders));
        genderList.addAll(genders);

        dropdownLanguage.setAdapter(new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, langList));
        dropdownGender.setAdapter(new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, genderList));

        dropdownLanguage.setOnItemClickListener((parent, view, position, id) -> {
            filterState.language = position == 0 ? null : langList.get(position);
            applyFilters();
        });

        dropdownGender.setOnItemClickListener((parent, view, position, id) -> {
            filterState.gender = position == 0 ? null : genderList.get(position);
            applyFilters();
        });
    }

    /**
     * Fetches all counselors from Firestore and populates the master list.
     */
    private void loadCounselors() {
        progressBar.setVisibility(View.VISIBLE);

        counselorRepository.getAllCounselors(new CounselorRepository.OnCounselorsLoadedCallback() {
            @Override
            public void onSuccess(List<Counselor> counselors) {
                progressBar.setVisibility(View.GONE);
                masterList = counselors;
                populateFilterDropdowns(counselors);
                applyFilters();
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(CounselorListActivity.this,
                        getString(R.string.error_loading_counselors),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Applies all active filters from {@link FilterState} to the master list
     * and updates the adapter. Shows the empty-state view when no results match.
     */
    private void applyFilters() {
        List<Counselor> filtered = new ArrayList<>(masterList);

        // Name search
        if (!filterState.nameQuery.isEmpty()) {
            filtered.removeIf(c -> c.getName() == null
                    || !c.getName().toLowerCase().contains(filterState.nameQuery));
        }

        // Specialization filter (multi-select OR: counselor must have at least one selected tag)
        if (!filterState.selectedSpecializations.isEmpty()) {
            filtered.removeIf(c -> {
                if (c.getSpecializations() == null) return true;
                for (String tag : filterState.selectedSpecializations) {
                    if (c.getSpecializations().contains(tag)) return false;
                }
                return true;
            });
        }

        // Language filter
        if (filterState.language != null) {
            filtered.removeIf(c -> !filterState.language.equals(c.getLanguage()));
        }

        // Gender filter
        if (filterState.gender != null) {
            filtered.removeIf(c -> !filterState.gender.equals(c.getGender()));
        }

        adapter.setData(filtered);

        boolean isEmpty = filtered.isEmpty();
        textEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    /**
     * Holds all currently active filter values.
     * All filters default to "no filter" (empty/null).
     */
    private static class FilterState {
        String nameQuery = "";
        List<String> selectedSpecializations = new ArrayList<>();
        String language = null;
        String gender = null;
    }
}
