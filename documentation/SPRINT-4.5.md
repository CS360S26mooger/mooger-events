# Sprint 4.5 — Slide-to-Cancel Hardening, Quiz Expansion, Homepage Chips & Search Routing
### Integration & Refinement Sprint

---

## 0. Pre-Sprint Context

This sprint formalizes and completes functionality that was added by concurrent developers outside the original PLAN.md scope. These features exist in various states of partial implementation and need to be brought to parity with the rest of the codebase — wired to real data, following repository patterns, and integrated into the user journey.

### What Already Exists and What Needs Work

#### Feature 1: Slide-to-Cancel (US-03)

| Component | Current State | What This Sprint Builds |
|---|---|---|
| SeekBar slide UI | **Fully working.** FrameLayout with transparent SeekBar, pink fill tracks thumb position, triggers at ≥95% progress. | No visual changes needed. |
| Cancel confirmation dialog | **Fully working.** AlertDialog: "Cancel Appointment?" with Yes/No. | No changes needed. |
| Firestore cancellation | **Fully working.** `AppointmentRepository.cancelAppointment(apptId, slotId, callback)` marks appointment CANCELLED and restores slot `available = true`. | No changes needed. |
| Upcoming session loading | **Fully working.** `fetchUpcomingSession()` on resume, filters for earliest CONFIRMED appointment ≥ today, populates card via `CounselorRepository.getCounselor()` lookup. | Minor: the slide-to-cancel only works for the single displayed appointment. If cancellation succeeds, the card clears — but does not check for the next upcoming appointment. Need to re-fetch after cancel. |
| Session card refresh after cancel | **Partial.** `clearSessionCard()` hides the card but does not load the next appointment. | Wire `fetchUpcomingSession()` after successful cancel so the next appointment (if any) appears without requiring a screen re-open. |

**Verdict:** Slide-to-cancel is ~95% done. The only gap is re-fetching the next appointment after a successful cancellation.

#### Feature 2: Find My Match Quiz (US-04)

| Component | Current State | What This Sprint Builds |
|---|---|---|
| `QuizActivity.java` | **Minimal.** Single "question" — 4 category buttons (Mental Health, Career, Academics, Relationships). Tapping any button routes to `CounselorListActivity` with a `SPECIALIZATION` intent extra. | Expand into a multi-step questionnaire (3–4 questions) that narrows down to a recommendation. For this phase, all paths lead to Dr. Baz (the only counselor on the platform), but the questionnaire structure should be extensible. |
| `activity_quiz.xml` | 4 large CardView buttons. | Redesign for multi-step flow with progress indicator, question text, and option buttons. |
| Routing to CounselorListActivity | Works. Passes specialization filter. | Change final destination: after quiz completion, navigate directly to `CounselorProfileActivity` for Dr. Baz (the matched counselor) instead of the list. |

**Verdict:** Quiz needs a full overhaul — from a 1-question category picker to a guided 3–4 question assessment.

#### Feature 3: Homepage Filter Chips + Search Bar

| Component | Current State | What This Sprint Builds |
|---|---|---|
| Search bar on homepage | **Static placeholder.** CardView with "Search by name or issue..." text. `onClick` launches `CounselorListActivity` (no search query passed). | Wire it as a clickable entry point that passes the typed text to `CounselorListActivity` as a pre-filled search query. Use an `EditText` so users can type before navigating. |
| Homepage filter chips | **Completely non-functional.** 6 hardcoded TextViews ("All", "Academic Stress", "Career Anxiety", "General Therapy", "Relationships", "Mindfulness") with no click listeners. Static decorative elements. | Wire each chip to launch `CounselorListActivity` with the corresponding specialization pre-selected. Replace hardcoded TextViews with proper Material Chips built from `SpecializationTags.ALL_TAGS`. |
| `CounselorListActivity` filters | **Fully working.** Name search (real-time), specialization chips (multi-select), language/gender dropdowns. All client-side against master list. | Add support for receiving a `SEARCH_QUERY` intent extra to pre-fill the search bar on load. Already handles `SPECIALIZATION` extra from QuizActivity. |

**Verdict:** Homepage elements are purely decorative. They need to be wired as entry points into the already-working `CounselorListActivity` filter system.

---

## 1. Sprint 4.5 Objective

By the end of this sprint:

1. **Slide-to-cancel refreshes the session card** after a successful cancellation, showing the next upcoming appointment if one exists.
2. **The Find My Match quiz is a guided multi-step questionnaire** with 3–4 questions, a progress indicator, and a final recommendation screen that routes to the matched counselor's profile.
3. **Homepage filter chips are functional** — tapping a chip navigates to `CounselorListActivity` with that specialization pre-selected.
4. **The homepage search bar accepts text input** and passes it to `CounselorListActivity` as a pre-filled search query, opening a dedicated search results page.

---

## 2. Files to Create or Modify

### 2.1 New Files

```
src/main/res/layout/
├── activity_quiz_question.xml          // Multi-step quiz layout (replaces activity_quiz.xml content)
├── activity_quiz_result.xml            // Quiz result / recommendation screen
```

### 2.2 Files to Modify

```
StudentHomeActivity.java           // Re-fetch after cancel, wire search bar + chips
activity_student_home.xml          // Replace static chips with dynamic ChipGroup, make search bar an EditText
QuizActivity.java                  // Multi-step questionnaire logic
activity_quiz.xml                  // Redesign for step-by-step flow
CounselorListActivity.java         // Accept SEARCH_QUERY intent extra
strings.xml                        // Quiz questions, chip labels, search strings
```

---

## 3. Implementation Details — Feature 1: Slide-to-Cancel Refresh

### 3.1 The Gap

In `StudentHomeActivity.java`, when `handleCancellation()` succeeds:

```java
// CURRENT (line ~315):
@Override
public void onSuccess() {
    Toast.makeText(..., "Appointment cancelled.", ...);
    clearSessionCard();  // Hides card, but doesn't load next appointment
}
```

After `clearSessionCard()`, the session card area goes blank. If the student has another CONFIRMED appointment, they won't see it until they leave and re-enter the screen.

### 3.2 The Fix

Replace `clearSessionCard()` with `fetchUpcomingSession()` in the cancel success callback:

```java
@Override
public void onSuccess() {
    Toast.makeText(StudentHomeActivity.this,
        getString(R.string.appointment_cancelled), Toast.LENGTH_SHORT).show();
    seekBar.setProgress(0);  // Reset slider
    fetchUpcomingSession();  // Re-fetch: shows next appointment or clears card
}
```

**How `fetchUpcomingSession()` already handles both cases:**
- If another CONFIRMED appointment exists ≥ today → `populateSessionCard(appointment)` displays it with the slider ready for another cancel.
- If no appointments remain → `clearSessionCard()` hides the session area cleanly.

This is a one-line change. The infrastructure already exists — it just wasn't called at the right moment.

### 3.3 How It Connects to the Existing Stack

```
User slides to 95% → handleCancellation()
    │
    ├── AlertDialog: "Cancel Appointment?"
    │     ├── "Yes, Cancel":
    │     │     └── appointmentRepository.cancelAppointment(apptId, slotId, callback)
    │     │           │
    │     │           ├── Firestore: appointments/{id}.status = "CANCELLED"
    │     │           ├── Firestore: slots/{slotId}.available = true (best-effort restore)
    │     │           │
    │     │           └── onSuccess():
    │     │                 ├── Toast "Appointment cancelled."
    │     │                 ├── seekBar.setProgress(0)
    │     │                 └── fetchUpcomingSession()    ← THE FIX
    │     │                       │
    │     │                       ├── appointmentRepository.getAppointmentsForStudent()
    │     │                       ├── Filter: status == CONFIRMED && date >= today
    │     │                       ├── If found → populateSessionCard(nextAppt)
    │     │                       │               └── CounselorRepository.getCounselor()
    │     │                       │                     → show counselor name, date, time
    │     │                       └── If none → clearSessionCard()
    │     │
    │     └── "No":
    │           └── resetSlider()
    │
    └── onFailure():
          ├── Toast "Could not cancel."
          └── resetSlider()
```

---

## 4. Implementation Details — Feature 2: Quiz Expansion

### 4.1 Design: Multi-Step Questionnaire

The quiz transforms from a single category picker into a 3-question guided assessment. Each question narrows the recommendation context, but since Dr. Baz is currently the only counselor, all paths converge to the same result. The structure is built to be extensible — when more counselors join, the question answers can feed into a real matching algorithm.

**Question Flow:**

```
Q1: "What's been on your mind lately?"
    → Emotional/Personal
    → Academic/Career
    → Relationships
    → Not sure / General

Q2: "How long have you been dealing with this?"
    → Just started (< 1 week)
    → A few weeks
    → More than a month
    → On and off for a while

Q3: "What kind of support are you looking for?"
    → Someone to talk to and listen
    → Practical strategies and coping tools
    → Help understanding my feelings
    → Crisis or urgent support

→ Result Screen: "We recommend Dr. Baz"
    → "View Profile" → CounselorProfileActivity
    → "Browse All Counselors" → CounselorListActivity
```

**Why 3 questions, not more:** The quiz should feel quick and low-friction. A student seeking counseling is already in a vulnerable state — a 10-question intake form would be a barrier. Three questions provide enough signal for future matching without feeling like a chore.

### 4.2 `QuizActivity.java` — Multi-Step Logic

The activity manages quiz state with a simple step counter and a list of question data:

```java
package com.example.moogerscouncil;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

/**
 * Multi-step questionnaire that guides students through a brief
 * assessment and recommends a counselor match. Currently all paths
 * lead to the same counselor (Dr. Baz) since the platform has one
 * counselor. The question structure is extensible for future matching.
 *
 * Flow: 3 questions → result screen → CounselorProfileActivity.
 */
public class QuizActivity extends AppCompatActivity {

    private static final int TOTAL_QUESTIONS = 3;

    private int currentStep = 0;
    private String[] answers = new String[TOTAL_QUESTIONS];

    // Question data: each question has a prompt and 4 options
    private final String[][] questions = {
        // Q1
        {"What's been on your mind lately?",
         "Emotional / Personal",
         "Academic / Career",
         "Relationships",
         "Not sure / General"},
        // Q2
        {"How long have you been dealing with this?",
         "Just started (less than a week)",
         "A few weeks",
         "More than a month",
         "On and off for a while"},
        // Q3
        {"What kind of support are you looking for?",
         "Someone to talk to and listen",
         "Practical strategies and coping tools",
         "Help understanding my feelings",
         "Crisis or urgent support"}
    };

    private TextView textQuestionNumber;
    private TextView textQuestion;
    private ProgressBar progressBar;
    private MaterialButton btnOption1, btnOption2, btnOption3, btnOption4;
    private ImageButton btnBack;

    // Result screen views
    private View layoutQuestion;
    private View layoutResult;
    private TextView textResultTitle;
    private TextView textResultSubtitle;
    private TextView textResultSummary;
    private MaterialButton btnViewProfile;
    private MaterialButton btnBrowseAll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        // Question views
        layoutQuestion = findViewById(R.id.layoutQuestion);
        textQuestionNumber = findViewById(R.id.textQuestionNumber);
        textQuestion = findViewById(R.id.textQuestion);
        progressBar = findViewById(R.id.progressQuiz);
        btnOption1 = findViewById(R.id.btnOption1);
        btnOption2 = findViewById(R.id.btnOption2);
        btnOption3 = findViewById(R.id.btnOption3);
        btnOption4 = findViewById(R.id.btnOption4);
        btnBack = findViewById(R.id.btnBack);

        // Result views
        layoutResult = findViewById(R.id.layoutResult);
        textResultTitle = findViewById(R.id.textResultTitle);
        textResultSubtitle = findViewById(R.id.textResultSubtitle);
        textResultSummary = findViewById(R.id.textResultSummary);
        btnViewProfile = findViewById(R.id.btnViewProfile);
        btnBrowseAll = findViewById(R.id.btnBrowseAll);

        progressBar.setMax(TOTAL_QUESTIONS);

        // Wire option buttons
        View.OnClickListener optionListener = v -> {
            MaterialButton btn = (MaterialButton) v;
            answers[currentStep] = btn.getText().toString();
            advanceStep();
        };
        btnOption1.setOnClickListener(optionListener);
        btnOption2.setOnClickListener(optionListener);
        btnOption3.setOnClickListener(optionListener);
        btnOption4.setOnClickListener(optionListener);

        btnBack.setOnClickListener(v -> {
            if (currentStep > 0) {
                currentStep--;
                displayQuestion(currentStep);
            } else {
                finish();
            }
        });

        displayQuestion(0);
    }

    private void displayQuestion(int step) {
        layoutQuestion.setVisibility(View.VISIBLE);
        layoutResult.setVisibility(View.GONE);

        textQuestionNumber.setText(getString(R.string.quiz_step_format,
            step + 1, TOTAL_QUESTIONS));
        textQuestion.setText(questions[step][0]);
        btnOption1.setText(questions[step][1]);
        btnOption2.setText(questions[step][2]);
        btnOption3.setText(questions[step][3]);
        btnOption4.setText(questions[step][4]);

        progressBar.setProgress(step);

        // Show/hide back arrow
        btnBack.setVisibility(step > 0 ? View.VISIBLE : View.INVISIBLE);
    }

    private void advanceStep() {
        currentStep++;
        if (currentStep < TOTAL_QUESTIONS) {
            displayQuestion(currentStep);
        } else {
            showResult();
        }
    }

    private void showResult() {
        layoutQuestion.setVisibility(View.GONE);
        layoutResult.setVisibility(View.VISIBLE);
        progressBar.setProgress(TOTAL_QUESTIONS);

        textResultTitle.setText(getString(R.string.quiz_result_title));
        textResultSubtitle.setText(getString(R.string.quiz_result_counselor_name));

        // Build a brief summary from answers
        String summary = getString(R.string.quiz_result_summary,
            answers[0], answers[2]);
        textResultSummary.setText(summary);

        // "View Profile" → Dr. Baz's profile
        btnViewProfile.setOnClickListener(v -> {
            // Fetch Dr. Baz's counselor document ID from Firestore
            CounselorRepository repo = new CounselorRepository();
            repo.getAllCounselors(
                new CounselorRepository.OnCounselorsLoadedCallback() {
                    @Override
                    public void onSuccess(java.util.List<Counselor> counselors) {
                        if (!counselors.isEmpty()) {
                            // Route to the first (only) counselor
                            Counselor match = counselors.get(0);
                            Intent intent = new Intent(QuizActivity.this,
                                CounselorProfileActivity.class);
                            intent.putExtra("COUNSELOR_ID", match.getId());
                            intent.putExtra("COUNSELOR_NAME", match.getName());
                            startActivity(intent);
                            finish();
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        android.widget.Toast.makeText(QuizActivity.this,
                            getString(R.string.error_loading_counselors),
                            android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
        });

        // "Browse All Counselors" → CounselorListActivity
        btnBrowseAll.setOnClickListener(v -> {
            startActivity(new Intent(QuizActivity.this,
                CounselorListActivity.class));
            finish();
        });
    }
}
```

**How it builds on existing infrastructure:**
- Uses `CounselorRepository.getAllCounselors()` (Sprint 2) to find the matched counselor.
- Routes to `CounselorProfileActivity` (Sprint 2) for viewing the match.
- Falls back to `CounselorListActivity` (Sprint 2) for browsing all counselors.
- No new Firestore collections or repositories needed — the quiz is purely UI logic.

### 4.3 `activity_quiz.xml` — Redesigned Layout

The layout needs to support both the question flow and the result screen within the same activity, toggled via visibility:

```xml
ConstraintLayout (background: app background color)
│
├── ProgressBar (id: progressQuiz, horizontal, determinate)
│     └── style: @style/Widget.MaterialComponents.LinearProgressIndicator
│     └── max = 3, progress updated per step
│
├── LinearLayout (id: layoutQuestion, vertical, padding 24dp)
│   ├── ImageButton (id: btnBack, back arrow, top-left)
│   │
│   ├── TextView (id: textQuestionNumber)
│   │     └── "Question 1 of 3" (14sp, grey)
│   │
│   ├── TextView (id: textQuestion)
│   │     └── The question text (22sp, bold, marginTop 8dp)
│   │
│   ├── Space (24dp)
│   │
│   ├── MaterialButton (id: btnOption1, outlined, full width, 56dp)
│   ├── MaterialButton (id: btnOption2, outlined, full width, 56dp, marginTop 12dp)
│   ├── MaterialButton (id: btnOption3, outlined, full width, 56dp, marginTop 12dp)
│   └── MaterialButton (id: btnOption4, outlined, full width, 56dp, marginTop 12dp)
│
└── LinearLayout (id: layoutResult, vertical, padding 24dp, GONE initially)
    ├── ImageView (checkmark or match icon, 80dp, centered)
    │
    ├── TextView (id: textResultTitle)
    │     └── "We found your match!" (24sp, bold, center)
    │
    ├── TextView (id: textResultSubtitle)
    │     └── "Dr. Baz" (20sp, center, primary color)
    │
    ├── TextView (id: textResultSummary)
    │     └── "Based on your responses..." (14sp, grey, center)
    │
    ├── MaterialButton (id: btnViewProfile, filled, full width, 56dp, marginTop 24dp)
    │     └── "View Profile"
    │
    └── MaterialButton (id: btnBrowseAll, outlined, full width, marginTop 12dp)
          └── "Browse All Counselors"
```

### 4.4 Future Extensibility

When more counselors join the platform, the quiz answer arrays can feed into a scoring function:

```java
// Future: score each counselor based on quiz answers
private Counselor findBestMatch(List<Counselor> counselors, String[] answers) {
    // Map Q1 answers to specialization tags
    Map<String, String> q1ToTag = Map.of(
        "Emotional / Personal", SpecializationTags.ANXIETY,
        "Academic / Career", SpecializationTags.ACADEMIC_STRESS,
        "Relationships", SpecializationTags.RELATIONSHIPS
    );
    // Score counselors by specialization match, availability, etc.
    // For now: return first available counselor
}
```

This is out of scope for Sprint 4.5 but the data structure (`answers[]`) is designed to support it.

---

## 5. Implementation Details — Feature 3: Homepage Chips & Search Bar

### 5.1 The Problem

`activity_student_home.xml` contains two UI elements that look functional but aren't:

1. **Filter chips** (lines 521–601): Six hardcoded `TextView` elements styled as chips. No click listeners. The tags don't match `SpecializationTags.ALL_TAGS` (e.g., "Mindfulness" isn't a defined tag).
2. **Search bar** (lines 486–519): A `CardView` with static "Search by name or issue..." text. Clicking it launches `CounselorListActivity` but passes no query — the user has to retype their search.

### 5.2 Fix: Replace Static Chips with Dynamic ChipGroup

**Remove** the 6 hardcoded TextViews from `activity_student_home.xml` and replace with a `ChipGroup` populated from `SpecializationTags.ALL_TAGS`:

```xml
<!-- Replace the hardcoded HorizontalScrollView chip section with: -->
<HorizontalScrollView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:scrollbars="none"
    android:layout_marginTop="12dp"
    android:paddingStart="16dp"
    android:paddingEnd="16dp">

    <com.google.android.material.chip.ChipGroup
        android:id="@+id/chipGroupHome"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        app:singleLine="true" />
</HorizontalScrollView>
```

**Java logic in `StudentHomeActivity.java`:**

```java
private void setupHomeChips() {
    ChipGroup chipGroup = findViewById(R.id.chipGroupHome);
    chipGroup.removeAllViews();

    for (String tag : SpecializationTags.ALL_TAGS) {
        Chip chip = new Chip(this);
        chip.setText(tag);
        chip.setCheckable(false);  // Not a filter — it's a navigation shortcut
        chip.setClickable(true);
        chip.setOnClickListener(v -> {
            Intent intent = new Intent(this, CounselorListActivity.class);
            intent.putExtra("SPECIALIZATION", tag);
            startActivity(intent);
        });
        chipGroup.addView(chip);
    }
}
```

**How it connects to the existing stack:**

```
User taps "Academic Stress" chip on StudentHomeActivity
    │
    └── Intent to CounselorListActivity
          └── intent.putExtra("SPECIALIZATION", "Academic Stress")
                │
                └── CounselorListActivity.onCreate()
                      └── String intentSpec = getIntent().getStringExtra("SPECIALIZATION")
                            └── buildSpecializationChips(intentSpec)
                                  └── Pre-checks "Academic Stress" chip
                                        └── applyFilters()
                                              └── Shows counselors with "Academic Stress"
```

This reuses the exact same intent-extra pathway that `QuizActivity` already uses. No changes needed in `CounselorListActivity` for specialization routing — it already handles the `SPECIALIZATION` extra.

### 5.3 Fix: Search Bar with Text Input → Query Pass-Through

**Replace** the static CardView search bar with an actual `EditText` inside a CardView that captures user input and passes it as a query:

```xml
<!-- Replace the static search bar CardView content with: -->
<androidx.cardview.widget.CardView
    android:id="@+id/searchBarHome"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginStart="16dp"
    android:layout_marginEnd="16dp"
    android:layout_marginTop="16dp"
    app:cardCornerRadius="24dp"
    app:cardElevation="2dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="48dp"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        android:paddingStart="16dp"
        android:paddingEnd="8dp">

        <ImageView
            android:layout_width="20dp"
            android:layout_height="20dp"
            android:src="@android:drawable/ic_menu_search"
            android:contentDescription="@string/search_icon_desc" />

        <EditText
            android:id="@+id/editSearchHome"
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:layout_weight="1"
            android:background="@android:color/transparent"
            android:hint="@string/search_counselors_hint"
            android:inputType="text"
            android:imeOptions="actionSearch"
            android:paddingStart="12dp"
            android:textSize="14sp"
            android:maxLines="1" />

        <ImageButton
            android:id="@+id/btnSearchGo"
            android:layout_width="36dp"
            android:layout_height="36dp"
            android:src="@android:drawable/ic_menu_send"
            android:background="?attr/selectableItemBackgroundBorderless"
            android:contentDescription="@string/search_go_desc" />
    </LinearLayout>
</androidx.cardview.widget.CardView>
```

**Java logic in `StudentHomeActivity.java`:**

```java
private void setupSearchBar() {
    EditText editSearch = findViewById(R.id.editSearchHome);
    ImageButton btnSearchGo = findViewById(R.id.btnSearchGo);

    // Navigate on "Go" button tap
    View.OnClickListener searchAction = v -> {
        String query = editSearch.getText().toString().trim();
        Intent intent = new Intent(this, CounselorListActivity.class);
        if (!query.isEmpty()) {
            intent.putExtra("SEARCH_QUERY", query);
        }
        startActivity(intent);
    };

    btnSearchGo.setOnClickListener(searchAction);

    // Also navigate on keyboard "Search" action
    editSearch.setOnEditorActionListener((v, actionId, event) -> {
        if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
            searchAction.onClick(v);
            return true;
        }
        return false;
    });
}
```

### 5.4 `CounselorListActivity.java` — Accept `SEARCH_QUERY` Extra

The activity already handles `SPECIALIZATION` intent extra. Add handling for `SEARCH_QUERY`:

```java
// In onCreate, after existing SPECIALIZATION handling:
String intentSearchQuery = getIntent().getStringExtra("SEARCH_QUERY");
if (intentSearchQuery != null && !intentSearchQuery.isEmpty()) {
    // Pre-fill the search bar
    editTextSearch.setText(intentSearchQuery);
    filterState.nameQuery = intentSearchQuery.toLowerCase();
    // applyFilters() will be called after counselors load
}
```

**How it connects:**

```
User types "anxiety" in homepage search bar → taps Go
    │
    └── Intent to CounselorListActivity
          └── intent.putExtra("SEARCH_QUERY", "anxiety")
                │
                └── CounselorListActivity.onCreate()
                      └── editTextSearch.setText("anxiety")
                      └── filterState.nameQuery = "anxiety"
                            │
                            └── (counselors load from Firestore)
                                  └── applyFilters()
                                        └── name filter: "anxiety".contains() check
                                              └── Shows counselors whose name
                                                  contains "anxiety"
```

**Note on filter interaction:** The `SEARCH_QUERY` extra pre-fills the name search. The `SPECIALIZATION` extra pre-selects a chip. If both are passed (unlikely from current UI), both filters apply simultaneously — this is correct behavior since `applyFilters()` applies all active filters.

---

## 6. Data Flow Diagrams

### 6.1 Homepage → Counselor Discovery (All Paths)

```
StudentHomeActivity
  │
  ├── [Search Bar] User types "Dr. Baz" → taps Go
  │     └── CounselorListActivity(SEARCH_QUERY="Dr. Baz")
  │           └── Search bar pre-filled → name filter applied → shows matching counselors
  │
  ├── [Filter Chip] User taps "Anxiety"
  │     └── CounselorListActivity(SPECIALIZATION="Anxiety")
  │           └── "Anxiety" chip pre-checked → specialization filter applied
  │
  ├── [Find My Match Card] User taps "Find My Match"
  │     └── QuizActivity
  │           ├── Q1 → Q2 → Q3 → Result Screen
  │           ├── "View Profile" → CounselorProfileActivity(Dr. Baz)
  │           └── "Browse All" → CounselorListActivity
  │
  └── [Counselor Cards] User taps a counselor card
        └── CounselorListActivity (no filters)
```

### 6.2 Quiz Flow (Step by Step)

```
QuizActivity
  │
  ├── Step 0: displayQuestion(0)
  │     └── "What's been on your mind lately?"
  │           └── 4 options → user taps → answers[0] = selection
  │                 └── advanceStep() → currentStep = 1
  │
  ├── Step 1: displayQuestion(1)
  │     └── "How long have you been dealing with this?"
  │           └── 4 options → user taps → answers[1] = selection
  │                 └── advanceStep() → currentStep = 2
  │
  ├── Step 2: displayQuestion(2)
  │     └── "What kind of support are you looking for?"
  │           └── 4 options → user taps → answers[2] = selection
  │                 └── advanceStep() → currentStep = 3
  │                       └── currentStep == TOTAL_QUESTIONS
  │                             └── showResult()
  │
  └── Result Screen:
        ├── "We found your match! — Dr. Baz"
        ├── Summary based on answers[0] and answers[2]
        │
        ├── "View Profile":
        │     └── CounselorRepository.getAllCounselors()
        │           └── counselors.get(0) → Dr. Baz
        │                 └── CounselorProfileActivity(counselorId, counselorName)
        │
        └── "Browse All Counselors":
              └── CounselorListActivity
```

### 6.3 Cancel → Re-fetch Flow

```
Student slides SeekBar to 95%
    │
    └── handleCancellation()
          └── AlertDialog "Cancel Appointment?"
                │
                ├── "Yes, Cancel":
                │     └── appointmentRepository.cancelAppointment(apptId, slotId)
                │           │
                │           ├── Firestore: appointments/{id}.status = "CANCELLED"
                │           ├── Firestore: slots/{slotId}.available = true
                │           │
                │           └── onSuccess:
                │                 ├── Toast "Appointment cancelled."
                │                 ├── seekBar.setProgress(0)
                │                 └── fetchUpcomingSession()    ← RE-FETCH
                │                       │
                │                       ├── getAppointmentsForStudent(studentId)
                │                       ├── Filter CONFIRMED ≥ today
                │                       │
                │                       ├── Found next → populateSessionCard(nextAppt)
                │                       │                  └── Show new counselor, date, time
                │                       │                  └── Slider ready for next cancel
                │                       │
                │                       └── None left → clearSessionCard()
                │                                         └── Hide session area
                │
                └── "No":
                      └── resetSlider()
```

---

## 7. Implementation Details — Strings

```xml
<!-- Quiz (expanded) -->
<string name="quiz_step_format">Question %1$d of %2$d</string>
<string name="quiz_q1">What\'s been on your mind lately?</string>
<string name="quiz_q1_opt1">Emotional / Personal</string>
<string name="quiz_q1_opt2">Academic / Career</string>
<string name="quiz_q1_opt3">Relationships</string>
<string name="quiz_q1_opt4">Not sure / General</string>
<string name="quiz_q2">How long have you been dealing with this?</string>
<string name="quiz_q2_opt1">Just started (less than a week)</string>
<string name="quiz_q2_opt2">A few weeks</string>
<string name="quiz_q2_opt3">More than a month</string>
<string name="quiz_q2_opt4">On and off for a while</string>
<string name="quiz_q3">What kind of support are you looking for?</string>
<string name="quiz_q3_opt1">Someone to talk to and listen</string>
<string name="quiz_q3_opt2">Practical strategies and coping tools</string>
<string name="quiz_q3_opt3">Help understanding my feelings</string>
<string name="quiz_q3_opt4">Crisis or urgent support</string>
<string name="quiz_result_title">We found your match!</string>
<string name="quiz_result_counselor_name">Dr. Baz</string>
<string name="quiz_result_summary">Based on your concern about \"%1$s\" and your preference for \"%2$s\", we think this counselor is a great fit.</string>
<string name="quiz_view_profile">View Profile</string>
<string name="quiz_browse_all">Browse All Counselors</string>

<!-- Homepage search + chips -->
<string name="search_counselors_hint">Search by name or issue…</string>
<string name="search_icon_desc">Search icon</string>
<string name="search_go_desc">Search</string>

<!-- Slide-to-cancel -->
<string name="appointment_cancelled">Appointment cancelled.</string>
<string name="cancel_failed">Could not cancel. Please try again.</string>
```

---

## 8. Testing Considerations

### Manual Verification Checklist

#### Slide-to-Cancel Refresh

- [ ] Cancel appointment via slider → next upcoming appointment appears immediately
- [ ] Cancel last remaining appointment → session card hides cleanly
- [ ] Cancel and re-open app → session area correctly shows remaining appointments

#### Quiz Flow

- [ ] Quiz starts on Q1, shows progress bar at 0/3
- [ ] Selecting an option advances to Q2, progress at 1/3
- [ ] Back button on Q2 returns to Q1 with progress at 0/3
- [ ] Completing Q3 shows result screen with progress at 3/3
- [ ] Result shows "Dr. Baz" with summary from answers
- [ ] "View Profile" navigates to CounselorProfileActivity
- [ ] "Browse All Counselors" navigates to CounselorListActivity
- [ ] Back button on Q1 exits the quiz

#### Homepage Chips

- [ ] Chips match `SpecializationTags.ALL_TAGS` (8 tags, not 6 hardcoded ones)
- [ ] Tapping any chip opens CounselorListActivity with that specialization pre-selected
- [ ] The pre-selected chip filters the counselor list correctly

#### Homepage Search Bar

- [ ] User can type in the search bar
- [ ] Tapping Go navigates to CounselorListActivity with search query pre-filled
- [ ] Pressing keyboard "Search" action also navigates
- [ ] Empty search bar navigates without a query (shows all counselors)
- [ ] Pre-filled query filters counselor list by name on load

---

## 9. Task Breakdown and Sequencing

```
┌──────────────────────────────────────────────────────┐
│ Phase A — Quick Fixes (independent, parallel)         │
│                                                      │
│  A1. StudentHomeActivity — fetchUpcomingSession()    │
│      call after cancel success (1-line fix)          │
│  A2. CounselorListActivity — accept SEARCH_QUERY    │
│      intent extra, pre-fill search bar (3-line add)  │
│                                                      │
│  Both are trivial. Can be done in minutes.           │
└──────────────────────┬───────────────────────────────┘
                       │
┌──────────────────────▼───────────────────────────────┐
│ Phase B — Homepage Wiring (depends on A2)            │
│                                                      │
│  B1. activity_student_home.xml — replace static      │
│      chips with ChipGroup                            │
│  B2. StudentHomeActivity — setupHomeChips() with     │
│      SpecializationTags.ALL_TAGS + navigation        │
│  B3. activity_student_home.xml — replace static      │
│      search bar with EditText + Go button            │
│  B4. StudentHomeActivity — setupSearchBar() with     │
│      query pass-through to CounselorListActivity     │
│  B5. strings.xml — search strings                    │
│                                                      │
│  B1/B2 and B3/B4 can be done in parallel.            │
└──────────────────────┬───────────────────────────────┘
                       │
┌──────────────────────▼───────────────────────────────┐
│ Phase C — Quiz Overhaul (independent of B)           │
│                                                      │
│  C1. activity_quiz.xml — redesign for multi-step     │
│      + result screen (both layouts in one file)      │
│  C2. QuizActivity.java — multi-step state machine,   │
│      3 questions, result with CounselorRepository    │
│      lookup routing to CounselorProfileActivity      │
│  C3. strings.xml — quiz questions and result text    │
│                                                      │
│  C1/C2 must be done together. Independent of B.      │
└──────────────────────┬───────────────────────────────┘
                       │
┌──────────────────────▼───────────────────────────────┐
│ Phase D — Verification                               │
│                                                      │
│  D1. Manual walkthrough of all 4 features            │
│  D2. Verify SpecializationTags consistency across    │
│      homepage chips, quiz categories, and            │
│      CounselorListActivity chips                     │
│  D3. Javadoc on modified methods                     │
└──────────────────────────────────────────────────────┘
```

**Parallelization:** Phase B (homepage wiring) and Phase C (quiz overhaul) touch completely different files and can be assigned to different team members.

---

## 10. How Everything Connects — Layer Map

```
┌─────────────────────────────────────────────────────────────────┐
│                        UI Layer                                  │
│                                                                 │
│  StudentHomeActivity                                            │
│  ├── Search bar (EditText) ──────────────────────┐              │
│  ├── Filter chips (ChipGroup from SpecTags) ─────┤              │
│  ├── "Find My Match" card ───────┐               │              │
│  ├── Session card + slide-cancel │               │              │
│  │     └── fetchUpcomingSession()│               │              │
│  │         after cancel ─────────┼───┐           │              │
│  │                               │   │           │              │
│  QuizActivity                    │   │           │              │
│  ├── Q1 → Q2 → Q3 ──────────────┤   │           │              │
│  └── Result → "View Profile" ───┤   │           │              │
│                                  │   │           │              │
│  CounselorListActivity ◄────────┘   │           │              │
│  ├── SPECIALIZATION extra (chips)    │    ◄──────┘              │
│  ├── SEARCH_QUERY extra (search)     │                          │
│  └── Combined filters apply          │                          │
│                                      │                          │
│  CounselorProfileActivity ◄─────────┘                          │
│  └── "Book Appointment" → BookingActivity                       │
└─────────────────────┬───────────────────────────────────────────┘
                      │ calls
┌─────────────────────▼───────────────────────────────────────────┐
│                    Repository Layer                              │
│                                                                 │
│  AppointmentRepository                                          │
│  ├── cancelAppointment(apptId, slotId) → CANCELLED + slot free  │
│  ├── getAppointmentsForStudent(studentId) → filter CONFIRMED    │
│  └── bookAppointment(studentId, counselorId, slot) → transaction│
│                                                                 │
│  CounselorRepository                                            │
│  ├── getAllCounselors() → quiz result lookup + list population   │
│  └── getCounselor(id) → session card counselor name display     │
│                                                                 │
│  AvailabilityRepository                                         │
│  └── getAvailableSlotsForCounselor() → booking calendar         │
└─────────────────────┬───────────────────────────────────────────┘
                      │ reads/writes
┌─────────────────────▼───────────────────────────────────────────┐
│                    Firestore                                     │
│                                                                 │
│  appointments/{id}  ← cancel writes status + slot restore       │
│  slots/{id}         ← availability restored on cancel           │
│  counselors/{id}    ← quiz result lookup, chip filter source    │
│  users/{uid}        ← student name for session card             │
└─────────────────────────────────────────────────────────────────┘
```

---

## 11. Relationship to PLAN.md

These features map to the following stories from the original product backlog:

| Feature | User Story | PLAN.md Status | Sprint 4.5 Action |
|---|---|---|---|
| Slide-to-cancel | US-03 | Deferred to Phase 4 (originally) | Formalize as complete — only needs post-cancel refresh fix |
| Find My Match quiz | US-04 | Deferred to Phase 4 (originally) | Expand from 1-question picker to 3-question guided assessment |
| Homepage chips + search | Part of US-23 | Core (Sprint 2) — directory exists | Wire homepage entry points into the already-built directory system |

All three features build on top of Sprint 2 (CounselorListActivity, CounselorRepository, SpecializationTags) and Sprint 3 (AppointmentRepository) foundations. No new Firestore collections or repository classes are needed — this sprint is purely about completing UI wiring and expanding the quiz experience.
