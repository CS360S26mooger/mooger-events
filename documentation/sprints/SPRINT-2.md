# Sprint 2 — US-23 (Counselor Directory) + US-06 (Specialization Tags)
### Detailed Implementation Guide

---

## 0. Pre-Sprint Status Report

### Sprint 1 Review — US-22 (Registration & Auth): COMPLETE

All Sprint 1 deliverables have been verified as complete:

| Deliverable | Status | Notes |
|---|---|---|
| `Student.java` | Done | Correct package, Javadoc, all fields, Firestore-compatible |
| `UserRole.java` | Done | STUDENT, COUNSELOR, ADMIN constants |
| `UserRepository.java` | Done | `createUser`, `getCurrentUser`, `getCurrentUserRole` |
| `Counselor.java` package fix | Done | Package corrected to `com.example.moogerscouncil` |
| `RegisterActivity.java` | Done | Full form, email domain validation, Firebase Auth + Firestore write |
| `PrivacyPolicyActivity.java` | Done | Scrollable policy, "I Agree" button, role-based routing |
| `StudentHomeActivity.java` | Done | Welcome greeting with preferred name, logout |
| `LoginActivity.java` | Done | Role-based routing via `UserRepository.getCurrentUserRole()`, auto-redirect |
| `strings.xml` | Done | All registration, validation, privacy, and home strings |
| `AndroidManifest.xml` | Done | All Sprint 1 activities registered |
| `StudentTest.java` (unit) | Done | Role defaults, constructor, empty constructor, setters, timestamp |
| `RegistrationFlowTest.java` (UI) | Done | Form display, email validation, password checks |
| `LoginRoleRoutingTest.java` (UI) | Done | Role buttons, button text updates, sign-up link |

### Merged Teammate Work — Student & Counselor Dashboards

Between Sprint 1 and Sprint 2, teammate Shahbaz's work was merged from branches `shahbaz/student-dash`, `shahbaz/counselor-dashboard`, and `shahbaz/booking-and-dashboard`. This introduced significant functionality that overlaps with and partially fulfills stories originally planned for later sprints. Below is a detailed inventory of what was merged and its current state.

#### New Activities Merged

| File | Implements | Completeness | Key Details |
|---|---|---|---|
| `StudentHomeActivity.java` | Enhanced student dashboard | ~95% | Privacy overlay (5s PII masking), crisis support banner with emergency numbers, "Find My Match" → QuizActivity, upcoming session display with slide-to-cancel, feedback dialog (UI only — not persisted), bottom nav (Home/Calendar/History/Logout) |
| `CounselorDashboardActivity.java` | Counselor appointment overview | ~80% | Greeting with counselor name from Firestore, 3-column stats (Today/Total/Week — same count, not differentiated), Add Availability slot via DatePicker+TimePicker → Firestore `slots` collection, appointment RecyclerView |
| `CounselorListActivity.java` | Counselor browsing with specialty filter | ~90% | Loads from Firestore `counselors` collection, filters by SPECIALIZATION intent extra, RecyclerView with CounselorAdapter, routes to BookingActivity |
| `BookingActivity.java` | Time slot selection + atomic booking | ~85% | Fetches available slots from `slots` collection, Firestore transaction for double-booking prevention, creates `appointments` doc with status CONFIRMED |
| `CalendarActivity.java` | Student appointment calendar | ~85% | CalendarView widget, date-based appointment filtering, StudentAppointmentAdapter |
| `HistoryActivity.java` | Past appointment list | ~75% | Ordered by date descending, basic display — no status/date-range filtering |
| `QuizActivity.java` | Specialty-based counselor matcher | ~100% | 4 categories (Mental Health, Career, Academic, Relationships), routes to CounselorListActivity with specialty filter |

#### New Adapters Merged

| File | Purpose | Completeness |
|---|---|---|
| `CounselorAdapter.java` | Counselor list items (name, specializations, rating, "View Slots") | ~80% — rating hardcoded "★ 4.9", "Available" static |
| `AppointmentAdapter.java` | Counselor-side appointment cards with action buttons | ~40% — all action buttons (Join, No-Show, Crisis, Profile, Notes) show placeholder toasts only |
| `StudentAppointmentAdapter.java` | Student-side appointment cards | ~75% — async counselor name lookup, hides unused action buttons |
| `TimeSlotAdapter.java` | Available time slots for booking | ~100% — shows date/time, Book/Booked state |

#### New Layouts Merged

| File | Lines | Key Elements |
|---|---|---|
| `activity_student_home.xml` | 495 | Top bar, crisis banner, action cards, upcoming session, slide-to-cancel, bottom nav, privacy overlay |
| `activity_counselor_dashboard.xml` | 290 | Greeting, 3-column stats, add-slot banner, appointment RecyclerView |
| `activity_counselor_list.xml` | 161 | Search bar, filter chips (Academic Stress, Career Anxiety, etc.), counselor RecyclerView |
| `activity_booking.xml` | 34 | Title, progress bar, time slot RecyclerView |
| `activity_calendar.xml` | 50 | CalendarView + appointment list |
| `activity_history.xml` | 26 | Title + appointment RecyclerView |
| `activity_quiz.xml` | 120 | 4 specialty cards |
| `dialog_feedback.xml` | 65 | 5-star RatingBar + comment field + submit |
| `item_appointment.xml` | 173 | Avatar, name, time, date, status, 5 action buttons |
| `item_counselor.xml` | 94 | Avatar, name, specialization, rating, "View Slots" |
| `item_timeslot.xml` | 49 | Date, time, Book button |

#### What This Means for Sprint 2

The merged work **partially covers** stories from the PLAN.md sprint schedule:

| PLAN.md Story | Sprint Originally Assigned | Status After Merge |
|---|---|---|
| **US-23** (Counselor Directory) | Sprint 2 | Partially done — `CounselorListActivity` exists with specialty filtering and RecyclerView. Missing: name search bar wiring, language/gender filter dropdowns, on-leave badge, `CounselorProfileActivity` (full bio + "Book" entry) |
| **US-06** (Specialization Tags) | Sprint 2 | Partially done — `CounselorAdapter` displays specialization tags on cards. Missing: `CounselorProfileEditActivity` for counselors to edit tags, `SpecializationTags` constants class, Firestore write for tag updates |
| **US-01** (Booking Flow) | Sprint 3 | Partially done — `BookingActivity` with atomic transaction exists. Missing: calendar-based date view (currently flat list), confirmation bottom sheet, `AvailabilitySchedule` model grouping slots by date |
| **US-05/US-10** (Counselor Dashboard) | Sprint 4 | Partially done — `CounselorDashboardActivity` exists with appointment list and add-slot. Missing: Today/Week/Month tab filtering, stats differentiation, status badges (CONFIRMED/COMPLETED/CANCELLED styling) |
| **US-20** (Emergency Button) | Sprint 4 | Partially done — crisis support banner with phone numbers exists in `StudentHomeActivity`. Missing: `EmergencyDialogFragment` with ACTION_DIAL intent, persistent FAB, phone numbers in strings.xml |
| **US-21** (Anonymous Feedback) | Sprint 5 | Partially done — feedback dialog UI exists. Missing: `FeedbackService` model, `FeedbackRepository`, Firestore persistence, no-studentId anonymity enforcement |

#### Firestore Collections in Use After Merge

| Collection | Used By | Fields |
|---|---|---|
| `users` | LoginActivity, RegisterActivity, StudentHomeActivity, StudentAppointmentAdapter | uid, name, email, preferredName, pronouns, role, createdAt |
| `counselors` | CounselorDashboardActivity, CounselorListActivity, StudentAppointmentAdapter | uid/id, name, specializations (array), rating |
| `slots` | CounselorDashboardActivity (write), BookingActivity (read + transaction) | counselorId, date, time, available |
| `appointments` | BookingActivity (write), CounselorDashboardActivity (read), CalendarActivity (read), HistoryActivity (read) | studentId, counselorId, slotId, date, time, status |

**Note:** The merged code uses `slots` as the collection name while PLAN.md specifies `timeSlots`. Sprint 2 will standardize on `slots` since it is already in use across multiple activities.

---

## 1. Sprint 2 Objective

By the end of this sprint, the app delivers the following additional capabilities on top of the Sprint 1 foundation and merged teammate work:

1. **A student can browse a searchable, filterable directory of counselors** — with real-time name search, multi-select specialization chips, language dropdown, and gender dropdown (US-23).
2. **A counselor can view and edit their specialization tags** from a profile edit screen, with changes persisted to Firestore and reflected in the student-facing directory (US-06).
3. **Infrastructure hardening** — `CounselorRepository` abstracts all `counselors` collection access, `SpecializationTags` constants class prevents tag typos, and filter chips in `CounselorListActivity` are wired to working filter logic.

### What Already Exists vs. What Sprint 2 Builds

| Component | Already Exists (from merge) | Sprint 2 Builds |
|---|---|---|
| Counselor list UI | `CounselorListActivity` with RecyclerView + specialty intent filter | Real-time search bar, multi-filter UI (specialization chips + language + gender dropdowns), empty-state message, on-leave badge |
| Counselor cards | `CounselorAdapter` with name, specializations, rating, "View Slots" | Dynamic rating display, "Currently Unavailable" badge for on-leave counselors, navigates to profile instead of directly to booking |
| Counselor profile | Does not exist | `CounselorProfileActivity` — full bio, tags as chips, language, gender, "Book Appointment" button (routes to existing BookingActivity) |
| Counselor profile edit | Does not exist | `CounselorProfileEditActivity` — toggleable specialization chips, save to Firestore |
| Repository layer | Direct Firestore calls in Activities | `CounselorRepository` wrapping all `counselors` collection operations |
| Tag constants | Hardcoded strings in QuizActivity | `SpecializationTags` constants class |

---

## 2. Files to Create or Modify

### 2.1 New Files

```
src/main/java/com/example/moogerscouncil/
├── CounselorRepository.java              // Repository — all counselors collection ops
├── CounselorProfileActivity.java         // UI — full counselor profile view
├── CounselorProfileEditActivity.java     // UI — counselor edits their own tags
├── SpecializationTags.java               // Constants — predefined specialization strings

src/main/res/layout/
├── activity_counselor_profile.xml        // Full counselor bio + "Book" button
├── activity_counselor_profile_edit.xml   // Chip-based tag editor for counselors

src/test/java/com/example/moogerscouncil/
├── CounselorTest.java                    // Unit test — specializations, on-leave toggle

src/androidTest/java/com/example/moogerscouncil/
├── CounselorDirectoryTest.java           // UI test — directory loads, filter works
```

### 2.2 Files to Modify

```
CounselorListActivity.java      // Wire search bar, add language/gender filters, empty state, on-leave badge
CounselorAdapter.java           // Dynamic rating, on-leave badge, tap → CounselorProfileActivity
Counselor.java                  // Add bio, language, gender, onLeave fields if missing
CounselorDashboardActivity.java // Add "Edit Profile" entry point → CounselorProfileEditActivity
activity_counselor_list.xml     // Add language/gender dropdowns alongside existing chips
item_counselor.xml              // Add on-leave badge area
strings.xml                     // Add all new user-facing strings
AndroidManifest.xml             // Register CounselorProfileActivity, CounselorProfileEditActivity
```

---

## 3. Implementation Details — Constants

### 3.1 `SpecializationTags.java`

**Purpose:** Single source of truth for all valid specialization tag strings. Prevents typos across activities, adapters, and Firestore documents.

```java
package com.example.moogerscouncil;

/**
 * String constants for counselor specialization tags.
 * Used in CounselorProfileEditActivity for tag selection,
 * CounselorListActivity for filter chips, and the Counselor
 * model's specializations field.
 *
 * Constants class — not instantiable.
 */
public final class SpecializationTags {

    public static final String ANXIETY = "Anxiety";
    public static final String ACADEMIC_STRESS = "Academic Stress";
    public static final String GRIEF = "Grief";
    public static final String RELATIONSHIPS = "Relationships";
    public static final String CAREER_GUIDANCE = "Career Guidance";
    public static final String DEPRESSION = "Depression";
    public static final String TRAUMA = "Trauma";
    public static final String FAMILY_ISSUES = "Family Issues";

    /** All tags in display order. Used to populate chip groups. */
    public static final String[] ALL_TAGS = {
        ANXIETY, ACADEMIC_STRESS, GRIEF, RELATIONSHIPS,
        CAREER_GUIDANCE, DEPRESSION, TRAUMA, FAMILY_ISSUES
    };

    private SpecializationTags() {}
}
```

**Design decisions:**
- Display-friendly strings (capitalized, spaces) rather than UPPER_SNAKE_CASE values. These are the strings stored in Firestore and shown to users.
- `ALL_TAGS` array allows iterating over all tags to build chip groups without listing them twice.
- The existing `QuizActivity` maps its 4 categories ("Mental Health & Well-being", "Career & Future Planning", etc.) to subsets of these tags. The mapping logic belongs in `QuizActivity`, not here.

---

## 4. Implementation Details — Model Updates

### 4.1 `Counselor.java` — Field Additions

The existing `Counselor.java` model needs verification and potential additions for Sprint 2. The PLAN.md Firestore schema specifies these fields:

```
counselors/{counselorId}
  ├── uid: String
  ├── name: String
  ├── bio: String
  ├── specializations: List<String>
  ├── language: String
  ├── gender: String
  ├── onLeave: Boolean
  ├── onLeaveMessage: String
  └── referralCounselorId: String
```

**What to check and add if missing:**

| Field | Purpose | Used By |
|---|---|---|
| `bio` | Full counselor bio displayed on profile screen | `CounselorProfileActivity` |
| `language` | Spoken language preference | Filter dropdown in `CounselorListActivity` |
| `gender` | Counselor gender | Filter dropdown in `CounselorListActivity` |
| `onLeave` | Boolean — currently on leave | `CounselorAdapter` badge, `CounselorProfileActivity` booking gate |
| `onLeaveMessage` | Custom leave message | Shown when on-leave counselor is viewed |
| `referralCounselorId` | ID of colleague handling their students | "See Referred Counselor" link (US-19, stretch) |

Each missing field needs:
1. Private field declaration with correct type
2. Getter and setter (Firestore requirement)
3. Javadoc on the getter

**No-arg constructor remains unchanged** — Firestore deserialization sets fields via setters. The parametrized constructor should be updated to accept `bio`, `language`, and `gender` if it exists, or these can be set post-construction.

---

## 5. Implementation Details — Repository Layer

### 5.1 `CounselorRepository.java`

**Purpose:** Single point of access for all Firestore operations on the `counselors` collection. Replaces direct Firestore calls currently scattered across `CounselorListActivity` and `CounselorDashboardActivity`.

**Design pattern:** Repository pattern, consistent with `UserRepository` from Sprint 1.

```java
package com.example.moogerscouncil;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;

/**
 * Repository for the Firestore 'counselors' collection.
 * All reads and writes to counselor documents flow through this class.
 * Activities depend on this class, never on FirebaseFirestore directly.
 *
 * Follows the Repository design pattern to isolate the data layer.
 */
public class CounselorRepository {

    private final CollectionReference counselorsCollection;

    public CounselorRepository() {
        this.counselorsCollection = FirebaseFirestore.getInstance()
            .collection("counselors");
    }

    // --- Callback interfaces ---

    public interface OnCounselorsLoadedCallback {
        void onSuccess(List<Counselor> counselors);
        void onFailure(Exception e);
    }

    public interface OnCounselorFetchedCallback {
        void onSuccess(Counselor counselor);
        void onFailure(Exception e);
    }

    public interface OnUpdateCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    // --- Read operations ---

    /**
     * Fetches all counselor documents from Firestore.
     * The full list is returned — filtering happens client-side
     * in the Activity for responsiveness.
     *
     * @param callback Receives the list on success, or an Exception on failure.
     */
    public void getAllCounselors(OnCounselorsLoadedCallback callback) {
        counselorsCollection.get()
            .addOnSuccessListener(querySnapshot -> {
                List<Counselor> counselors = querySnapshot.toObjects(Counselor.class);
                callback.onSuccess(counselors);
            })
            .addOnFailureListener(callback::onFailure);
    }

    /**
     * Fetches a single counselor by document ID.
     *
     * @param counselorId The Firestore document ID.
     * @param callback    Receives the Counselor on success, or an Exception on failure.
     */
    public void getCounselor(String counselorId, OnCounselorFetchedCallback callback) {
        counselorsCollection.document(counselorId)
            .get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    Counselor counselor = doc.toObject(Counselor.class);
                    callback.onSuccess(counselor);
                } else {
                    callback.onFailure(
                        new IllegalStateException("Counselor document not found"));
                }
            })
            .addOnFailureListener(callback::onFailure);
    }

    // --- Write operations ---

    /**
     * Updates the specialization tags for a counselor.
     *
     * @param counselorId    The Firestore document ID.
     * @param specializations The updated list of specialization tag strings.
     * @param callback        Success/failure callback.
     */
    public void updateSpecializations(String counselorId,
                                       List<String> specializations,
                                       OnUpdateCallback callback) {
        counselorsCollection.document(counselorId)
            .update("specializations", specializations)
            .addOnSuccessListener(unused -> callback.onSuccess())
            .addOnFailureListener(callback::onFailure);
    }

    /**
     * Updates multiple profile fields for a counselor at once.
     * Used by CounselorProfileEditActivity when the counselor saves
     * their full profile (bio, language, gender, specializations).
     *
     * @param counselorId The Firestore document ID.
     * @param counselor   The updated Counselor object — all fields are overwritten.
     * @param callback    Success/failure callback.
     */
    public void updateCounselorProfile(String counselorId,
                                        Counselor counselor,
                                        OnUpdateCallback callback) {
        counselorsCollection.document(counselorId)
            .set(counselor)
            .addOnSuccessListener(unused -> callback.onSuccess())
            .addOnFailureListener(callback::onFailure);
    }
}
```

**Migration note:** After `CounselorRepository` is created, the direct Firestore calls in `CounselorListActivity` and `CounselorDashboardActivity` should be refactored to use the repository. This keeps the codebase consistent with the `UserRepository` pattern from Sprint 1.

### 5.2 Refactoring Existing Activities to Use CounselorRepository

**`CounselorListActivity.java`** currently does:
```java
db.collection("counselors").get()
    .addOnSuccessListener(queryDocumentSnapshots -> { ... })
```

**Replace with:**
```java
CounselorRepository repo = new CounselorRepository();
repo.getAllCounselors(new CounselorRepository.OnCounselorsLoadedCallback() {
    @Override
    public void onSuccess(List<Counselor> counselors) {
        masterList = counselors;
        applyFilters();
    }

    @Override
    public void onFailure(Exception e) {
        Toast.makeText(CounselorListActivity.this,
            getString(R.string.error_loading_counselors), Toast.LENGTH_LONG).show();
    }
});
```

**`CounselorDashboardActivity.java`** — the counselor name fetch on the dashboard also queries `counselors`. Move this to use `CounselorRepository.getCounselor()`.

---

## 6. Implementation Details — UI Layer

### 6.1 `CounselorListActivity.java` — Enhancements

**Current state:** Loads all counselors, filters by a single specialty passed via intent. Filter chips in the layout are not wired.

**What Sprint 2 adds:**

#### 6.1.1 Search Bar Wiring

The layout already has a search field (`SearchView` or `EditText`). Wire it to filter the counselor list by name in real time:

```java
searchBar.addTextChangedListener(new TextWatcher() {
    @Override
    public void afterTextChanged(Editable s) {
        nameFilter = s.toString().trim().toLowerCase();
        applyFilters();
    }
    // beforeTextChanged, onTextChanged: no-op
});
```

#### 6.1.2 Multi-Filter Architecture

Introduce a `FilterState` object (inner class or standalone) to track all active filters:

```java
private static class FilterState {
    String nameQuery = "";
    List<String> selectedSpecializations = new ArrayList<>();
    String language = null;   // null = "All"
    String gender = null;     // null = "All"
}
```

Central `applyFilters()` method applies all active filters to the master list:

```java
private void applyFilters() {
    List<Counselor> filtered = new ArrayList<>(masterList);

    // Name search
    if (!filterState.nameQuery.isEmpty()) {
        filtered.removeIf(c ->
            !c.getName().toLowerCase().contains(filterState.nameQuery));
    }

    // Specialization filter (multi-select: counselor must have at least one selected tag)
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
        filtered.removeIf(c ->
            !filterState.language.equals(c.getLanguage()));
    }

    // Gender filter
    if (filterState.gender != null) {
        filtered.removeIf(c ->
            !filterState.gender.equals(c.getGender()));
    }

    adapter.setData(filtered);

    // Empty state
    emptyStateView.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    recyclerView.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
}
```

#### 6.1.3 Filter Chips Wiring

The layout already has horizontal filter chips. Replace the static chip list with programmatically generated chips from `SpecializationTags.ALL_TAGS`:

```java
ChipGroup chipGroup = findViewById(R.id.chipGroupSpecializations);
chipGroup.removeAllViews();

// "All" chip (default selected)
Chip allChip = new Chip(this);
allChip.setText("All");
allChip.setCheckable(true);
allChip.setChecked(true);
chipGroup.addView(allChip);

for (String tag : SpecializationTags.ALL_TAGS) {
    Chip chip = new Chip(this);
    chip.setText(tag);
    chip.setCheckable(true);
    chipGroup.addView(chip);
}

chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
    filterState.selectedSpecializations.clear();
    for (int id : checkedIds) {
        Chip chip = group.findViewById(id);
        if (chip != null && !"All".equals(chip.getText().toString())) {
            filterState.selectedSpecializations.add(chip.getText().toString());
        }
    }
    applyFilters();
});
```

#### 6.1.4 Language and Gender Dropdowns

Add two `Spinner` or `AutoCompleteTextView` dropdowns below the chip group:

```xml
<!-- In activity_counselor_list.xml, below chip group -->
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="horizontal"
    android:layout_marginTop="8dp">

    <com.google.android.material.textfield.TextInputLayout
        style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox.ExposedDropdownMenu"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:layout_marginEnd="4dp"
        android:hint="@string/filter_language">
        <AutoCompleteTextView
            android:id="@+id/dropdownLanguage"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:inputType="none" />
    </TextInputLayout>

    <com.google.android.material.textfield.TextInputLayout
        style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox.ExposedDropdownMenu"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:layout_marginStart="4dp"
        android:hint="@string/filter_gender">
        <AutoCompleteTextView
            android:id="@+id/dropdownGender"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:inputType="none" />
    </TextInputLayout>
</LinearLayout>
```

Populate dropdowns dynamically from the fetched counselor data:

```java
private void populateFilterDropdowns(List<Counselor> counselors) {
    // Extract unique languages
    Set<String> languages = new TreeSet<>();
    Set<String> genders = new TreeSet<>();
    for (Counselor c : counselors) {
        if (c.getLanguage() != null) languages.add(c.getLanguage());
        if (c.getGender() != null) genders.add(c.getGender());
    }

    List<String> langList = new ArrayList<>();
    langList.add("All Languages");
    langList.addAll(languages);

    List<String> genderList = new ArrayList<>();
    genderList.add("All Genders");
    genderList.addAll(genders);

    dropdownLanguage.setAdapter(
        new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, langList));
    dropdownGender.setAdapter(
        new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, genderList));

    dropdownLanguage.setOnItemClickListener((parent, view, position, id) -> {
        filterState.language = position == 0 ? null : langList.get(position);
        applyFilters();
    });

    dropdownGender.setOnItemClickListener((parent, view, position, id) -> {
        filterState.gender = position == 0 ? null : genderList.get(position);
        applyFilters();
    });
}
```

#### 6.1.5 Empty State

Add a `TextView` to the layout (below the RecyclerView, initially `GONE`) for when no counselors match the current filters:

```xml
<TextView
    android:id="@+id/textEmptyState"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:text="@string/no_counselors_match"
    android:textAlignment="center"
    android:padding="32dp"
    android:textSize="16sp"
    android:visibility="gone" />
```

#### 6.1.6 On-Leave Badge

In `CounselorAdapter`, check `counselor.isOnLeave()` (or `counselor.getOnLeave()`) and if true:
- Show a "Currently Unavailable" badge on the card
- Disable the "View Slots" button
- Optionally show `onLeaveMessage` as a subtitle

```java
// In CounselorAdapter.onBindViewHolder
if (counselor.getOnLeave() != null && counselor.getOnLeave()) {
    holder.badgeOnLeave.setVisibility(View.VISIBLE);
    holder.buttonViewSlots.setEnabled(false);
    holder.buttonViewSlots.setText(R.string.currently_unavailable);
} else {
    holder.badgeOnLeave.setVisibility(View.GONE);
    holder.buttonViewSlots.setEnabled(true);
    holder.buttonViewSlots.setText(R.string.view_slots);
}
```

---

### 6.2 `CounselorAdapter.java` — Enhancements

**Current state:** Shows name, specialization (joined string), hardcoded "★ 4.9" rating, "View Slots" button that routes directly to BookingActivity.

**Sprint 2 changes:**

1. **Tap target changed:** Card tap → `CounselorProfileActivity` (full bio view) instead of directly to BookingActivity. The "View Slots" button is replaced with "View Profile".
2. **On-leave badge:** New `TextView` in `item_counselor.xml` for the "Currently Unavailable" chip, toggled by `onLeave` field.
3. **Language display:** Show the counselor's language below specializations.

```java
// Updated click listener
holder.itemView.setOnClickListener(v -> {
    Intent intent = new Intent(context, CounselorProfileActivity.class);
    intent.putExtra("COUNSELOR_ID", counselor.getUid());
    intent.putExtra("COUNSELOR_NAME", counselor.getName());
    context.startActivity(intent);
});
```

---

### 6.3 `CounselorProfileActivity.java` — New

**Purpose:** Shows the full profile of a single counselor. Entry point for booking.

**Layout — `activity_counselor_profile.xml`:**

```
ScrollView
└── ConstraintLayout (padding 24dp)
    ├── ImageView: counselor avatar placeholder (80dp circle)
    ├── TextView: counselor name (24sp, bold)
    ├── ChipGroup: specialization tags (read-only, non-checkable)
    ├── TextView: "Language: {language}" (14sp)
    ├── TextView: "Gender: {gender}" (14sp)
    │
    ├── CardView: Bio section
    │   └── TextView: counselor bio (14sp, lineSpacingExtra 6dp)
    │
    ├── [If onLeave == true]
    │   └── CardView: On-leave message (amber background)
    │       ├── TextView: "Currently On Leave"
    │       └── TextView: onLeaveMessage
    │
    └── MaterialButton: "Book Appointment" (full width, primary_blue, 56dp)
          └── If onLeave: disabled, text = "Currently Unavailable"
          └── If available: enabled, routes to BookingActivity with counselorId
```

**Runtime behavior:**

```
CounselorProfileActivity opens
  │
  ├── Receive counselorId from Intent
  ├── CounselorRepository.getCounselor(counselorId, callback)
  │     ├── onSuccess: populate all UI fields
  │     └── onFailure: Toast error, finish()
  │
  ├── Populate:
  │     ├── Name
  │     ├── Specializations as Material Chips (non-checkable)
  │     ├── Language, Gender
  │     ├── Bio text
  │     └── On-leave card (if applicable)
  │
  └── "Book Appointment" tap:
        ├── If onLeave → disabled, no action
        └── If available:
              Intent to BookingActivity with:
                - COUNSELOR_ID
                - COUNSELOR_NAME
```

---

### 6.4 `CounselorProfileEditActivity.java` — New

**Purpose:** Allows a logged-in counselor to edit their profile — specifically, select specialization tags, set bio, language, and gender.

**Entry point:** "Edit Profile" button on `CounselorDashboardActivity`.

**Layout — `activity_counselor_profile_edit.xml`:**

```
ScrollView
└── LinearLayout (vertical, padding 24dp)
    ├── Toolbar: "Edit Profile" with back arrow
    │
    ├── TextInputLayout: "Bio" (editTextBio)
    │     └── multiline, 4 lines min
    │
    ├── TextInputLayout: "Language" (editTextLanguage)
    │     └── helperText: "e.g. English, Urdu, Punjabi"
    │
    ├── TextInputLayout: "Gender" (editTextGender)
    │     └── helperText: "As displayed on your profile"
    │
    ├── TextView: "Specialization Areas" (section header, 16sp, bold)
    │
    ├── ChipGroup (id: chipGroupSpecializations)
    │     └── Populated programmatically from SpecializationTags.ALL_TAGS
    │     └── Checkable = true (toggle on/off)
    │     └── Pre-selected based on current Firestore data
    │
    └── MaterialButton: "Save Profile" (full width, primary_blue, 56dp)
```

**Runtime behavior:**

```
CounselorProfileEditActivity opens
  │
  ├── Get counselorId from FirebaseAuth.getCurrentUser().getUid()
  ├── CounselorRepository.getCounselor(counselorId, callback)
  │     ├── onSuccess: pre-fill all fields
  │     │     ├── Set bio, language, gender in TextInputLayouts
  │     │     └── Check chips matching existing specializations
  │     └── onFailure: Toast error
  │
  ├── Build ChipGroup from SpecializationTags.ALL_TAGS:
  │     for (String tag : SpecializationTags.ALL_TAGS) {
  │         Chip chip = new Chip(this);
  │         chip.setText(tag);
  │         chip.setCheckable(true);
  │         chip.setChecked(existingSpecializations.contains(tag));
  │         chipGroup.addView(chip);
  │     }
  │
  └── "Save Profile" tap:
        ├── Collect checked chips into List<String>
        ├── Build updated Counselor object with bio, language, gender, specializations
        ├── CounselorRepository.updateCounselorProfile(counselorId, counselor, callback)
        │     ├── onSuccess: Toast "Profile updated", finish()
        │     └── onFailure: Toast error
        └── finish() returns to CounselorDashboardActivity
```

**Key implementation notes:**
- The counselor's UID (from `FirebaseAuth`) is used as the document ID in the `counselors` collection. If the counselor document doesn't exist yet (first-time setup), use `set()` instead of `update()` — `updateCounselorProfile` already uses `set()`.
- The `CounselorDashboardActivity` needs a button or menu item that launches `CounselorProfileEditActivity`. Add an "Edit Profile" icon or text to the top bar.

---

### 6.5 `CounselorDashboardActivity.java` — Modifications

**Add "Edit Profile" entry point:**

In the top bar area (near the logout button), add an icon or text button:

```java
ImageButton editProfileButton = findViewById(R.id.buttonEditProfile);
editProfileButton.setOnClickListener(v -> {
    startActivity(new Intent(this, CounselorProfileEditActivity.class));
});
```

The corresponding XML addition in `activity_counselor_dashboard.xml`:

```xml
<ImageButton
    android:id="@+id/buttonEditProfile"
    android:layout_width="40dp"
    android:layout_height="40dp"
    android:src="@android:drawable/ic_menu_edit"
    android:contentDescription="@string/edit_profile"
    android:background="?attr/selectableItemBackgroundBorderless" />
```

---

### 6.6 `AndroidManifest.xml` — Updates

Register the two new activities:

```xml
<activity
    android:name=".CounselorProfileActivity"
    android:exported="false" />
<activity
    android:name=".CounselorProfileEditActivity"
    android:exported="false" />
```

---

## 7. Implementation Details — Strings

All new user-facing text for Sprint 2:

```xml
<!-- Counselor Directory (US-23) -->
<string name="title_counselor_directory">Find a Specialist</string>
<string name="search_counselors_hint">Search by name…</string>
<string name="filter_language">Language</string>
<string name="filter_gender">Gender</string>
<string name="filter_all_languages">All Languages</string>
<string name="filter_all_genders">All Genders</string>
<string name="no_counselors_match">No counselors match your current filters.</string>
<string name="currently_unavailable">Currently Unavailable</string>
<string name="view_slots">View Slots</string>
<string name="view_profile">View Profile</string>
<string name="error_loading_counselors">Unable to load counselors. Please try again.</string>
<string name="error_loading_profile">Unable to load counselor profile.</string>

<!-- Counselor Profile (US-23) -->
<string name="title_counselor_profile">Counselor Profile</string>
<string name="label_language">Language</string>
<string name="label_gender">Gender</string>
<string name="label_bio">About</string>
<string name="label_specializations">Areas of Focus</string>
<string name="button_book_appointment">Book Appointment</string>
<string name="on_leave_title">Currently On Leave</string>

<!-- Counselor Profile Edit (US-06) -->
<string name="title_edit_profile">Edit Profile</string>
<string name="edit_profile">Edit Profile</string>
<string name="hint_bio">Bio</string>
<string name="helper_bio">Tell students about your approach and experience</string>
<string name="hint_language">Language</string>
<string name="helper_language">e.g. English, Urdu, Punjabi</string>
<string name="hint_gender">Gender</string>
<string name="helper_gender">As displayed on your profile</string>
<string name="label_select_specializations">Specialization Areas</string>
<string name="button_save_profile">Save Profile</string>
<string name="success_profile_saved">Profile updated successfully</string>
<string name="error_saving_profile">Failed to save profile. Please try again.</string>
```

---

## 8. Data Flow Diagrams

### 8.1 Counselor Directory Flow (Student-side)

```
Student                CounselorListActivity       CounselorRepository        Firestore
  │                          │                           │                       │
  │  taps "Find My Match"   │                           │                       │
  │  (from StudentHome      │                           │                       │
  │   or QuizActivity)      │                           │                       │
  │ ─────────────────────►  │                           │                       │
  │                          │  getAllCounselors(cb)      │                       │
  │                          │ ─────────────────────────►│                       │
  │                          │                           │  .get() counselors    │
  │                          │                           │ ─────────────────────►│
  │                          │                           │                       │
  │                          │                           │  QuerySnapshot        │
  │                          │                           │ ◄─────────────────────│
  │                          │  onSuccess(counselorList) │                       │
  │                          │ ◄─────────────────────────│                       │
  │                          │                           │                       │
  │                          │  masterList = counselors  │                       │
  │                          │  populateFilterDropdowns() │                       │
  │                          │  applyFilters()           │                       │
  │  sees counselor cards    │                           │                       │
  │ ◄────────────────────── │                           │                       │
  │                          │                           │                       │
  │  types in search bar     │                           │                       │
  │ ─────────────────────►  │                           │                       │
  │                          │  filterState.nameQuery    │                       │
  │                          │  = input                  │                       │
  │                          │  applyFilters() ← local  │                       │
  │  sees filtered results   │                           │                       │
  │ ◄────────────────────── │                           │                       │
  │                          │                           │                       │
  │  taps a counselor card   │                           │                       │
  │ ─────────────────────►  │                           │                       │
  │                          │  → CounselorProfileActivity                      │
```

### 8.2 Counselor Profile Edit Flow (Counselor-side)

```
Counselor          CounselorProfileEditActivity    CounselorRepository       Firestore
  │                          │                           │                       │
  │  taps "Edit Profile"    │                           │                       │
  │  (from Dashboard)       │                           │                       │
  │ ─────────────────────►  │                           │                       │
  │                          │  getCounselor(uid, cb)    │                       │
  │                          │ ─────────────────────────►│                       │
  │                          │                           │  .get() counselor     │
  │                          │                           │ ─────────────────────►│
  │                          │                           │  DocumentSnapshot     │
  │                          │                           │ ◄─────────────────────│
  │                          │  onSuccess(counselor)     │                       │
  │                          │ ◄─────────────────────────│                       │
  │                          │                           │                       │
  │                          │  Pre-fill bio, lang,      │                       │
  │                          │  gender, check spec chips │                       │
  │  sees current profile    │                           │                       │
  │ ◄────────────────────── │                           │                       │
  │                          │                           │                       │
  │  toggles chips, edits   │                           │                       │
  │  bio, taps "Save"       │                           │                       │
  │ ─────────────────────►  │                           │                       │
  │                          │  updateCounselorProfile   │                       │
  │                          │  (uid, counselor, cb)     │                       │
  │                          │ ─────────────────────────►│                       │
  │                          │                           │  .set(counselor)      │
  │                          │                           │ ─────────────────────►│
  │                          │                           │  success              │
  │                          │                           │ ◄─────────────────────│
  │                          │  onSuccess()              │                       │
  │                          │  Toast + finish()         │                       │
  │  returns to Dashboard   │ ◄─────────────────────────│                       │
  │ ◄────────────────────── │                           │                       │
```

---

## 9. Testing Plan

### 9.1 Unit Tests

#### `CounselorTest.java`

| Test | What it verifies |
|---|---|
| `testDefaultOnLeaveIsFalse` | New Counselor has `onLeave == false` (or null) |
| `testSpecializationsSetAndGet` | Setting a list of tags and retrieving it returns the same list |
| `testEmptyConstructorForFirestore` | No-arg constructor creates an instance with all fields null |
| `testSettersOverrideFields` | Bio, language, gender setters work correctly |

#### `SpecializationTagsTest.java` (optional, lightweight)

| Test | What it verifies |
|---|---|
| `testAllTagsNotEmpty` | `ALL_TAGS` array has expected length (8) |
| `testNoDuplicates` | No duplicate entries in `ALL_TAGS` |

### 9.2 UI Tests (Espresso)

#### `CounselorDirectoryTest.java`

| Test | What it asserts |
|---|---|
| `testDirectoryLoadsAndDisplaysCounselors` | RecyclerView is displayed and has items after Firestore fetch |
| `testSearchFiltersByName` | Typing a name in search bar reduces the visible list |
| `testSpecializationChipFilters` | Tapping a chip filters the list to matching counselors |
| `testEmptyStateShownWhenNoMatch` | Applying filters that match no counselors shows the empty-state text |
| `testTappingCardOpensCounselorProfile` | Tapping a counselor card launches `CounselorProfileActivity` |

---

## 10. Task Breakdown and Sequencing

Sprint 2 tasks should be implemented in this order due to dependencies:

```
┌─────────────────────────────────────────────────────┐
│ Phase A — Foundation (no UI dependency)              │
│                                                     │
│  A1. SpecializationTags.java (constants)            │
│  A2. Counselor.java field additions (bio, lang,     │
│      gender, onLeave, onLeaveMessage)               │
│  A3. CounselorRepository.java (full implementation) │
│  A4. CounselorTest.java (unit tests)                │
│                                                     │
│  These can all be done in parallel.                 │
└──────────────────────┬──────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────┐
│ Phase B — Counselor-side UI                         │
│                                                     │
│  B1. CounselorProfileEditActivity + layout          │
│      (tag chips, bio, language, gender, save)       │
│  B2. CounselorDashboardActivity — add "Edit Profile"│
│      button                                         │
│  B3. AndroidManifest + strings for edit screen      │
│                                                     │
│  B1 and B2 can be done in parallel.                 │
└──────────────────────┬──────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────┐
│ Phase C — Student-side UI                           │
│                                                     │
│  C1. CounselorProfileActivity + layout              │
│      (bio, tags, "Book Appointment" button)         │
│  C2. CounselorListActivity — wire search bar,       │
│      add language/gender dropdowns, empty state,    │
│      on-leave badge                                 │
│  C3. CounselorAdapter — tap → profile, on-leave     │
│      badge, language display                        │
│  C4. Refactor CounselorListActivity &               │
│      CounselorDashboardActivity to use              │
│      CounselorRepository                            │
│  C5. strings.xml additions + AndroidManifest update │
│                                                     │
│  C1 can start independently. C2/C3 depend on A3.   │
│  C4 depends on A3.                                  │
└──────────────────────┬──────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────┐
│ Phase D — Testing & Polish                          │
│                                                     │
│  D1. CounselorDirectoryTest.java (Espresso)         │
│  D2. Manual end-to-end walkthrough:                 │
│      - Counselor logs in → edits profile → saves    │
│      - Student logs in → browses directory →        │
│        filters → views profile → books              │
│  D3. Javadoc pass on all new files                  │
│  D4. Move stories to Done on Kanban board           │
└─────────────────────────────────────────────────────┘
```

---

## 11. Acceptance Criteria Checklist

### US-23 — Browse and Search Counselor Directory

- [ ] Counselor directory loads all counselors from Firestore on screen open
- [ ] Each counselor card displays: name, specialization tags (as chips), language, and gender
- [ ] Student can filter by specialization (multi-select chips)
- [ ] Student can filter by language (dropdown)
- [ ] Student can filter by gender (dropdown)
- [ ] Search bar filters counselors by name in real time
- [ ] Tapping a card navigates to `CounselorProfileActivity` showing full bio and "Book Appointment" button
- [ ] Counselors with `onLeave: true` display "Currently Unavailable" badge; booking button disabled
- [ ] Empty-state message shown when no counselors match filters
- [ ] All Firestore calls go through `CounselorRepository`
- [ ] All strings in `strings.xml`
- [ ] Javadoc on all new public methods
- [ ] `CounselorDirectoryTest.java` passes

### US-06 — Counselor Specialization Tags on Profile

- [ ] Counselor can navigate to "Edit Profile" from dashboard
- [ ] Edit screen displays all predefined tags from `SpecializationTags` as toggleable Material chips
- [ ] Counselor can select/deselect any combination of tags
- [ ] On save, selected tags are persisted to `counselors/{id}.specializations` in Firestore
- [ ] Saved tags appear correctly on the counselor's card in the directory
- [ ] Bio, language, and gender are also editable and persisted
- [ ] `SpecializationTags` constants class exists and is used for chip generation
- [ ] `CounselorTest.java` passes
- [ ] All strings in `strings.xml`
- [ ] Javadoc on all new public methods

---

## 12. Definition of Done (Sprint 2)

Per the project-wide Definition of Done in PLAN.md Section 12:

| # | Criterion | How to verify |
|---|---|---|
| 1 | All acceptance criteria pass when manually tested | Demo walkthrough |
| 2 | Firestore reads/writes are live | Code review — no stubbed data |
| 3 | Unit tests pass | `./gradlew test` |
| 4 | Intent test for directory happy path passes | `./gradlew connectedAndroidTest` |
| 5 | File header comment in every new `.java` file | Code review |
| 6 | Javadoc on all new model class public methods | Code review |
| 7 | No hardcoded user-facing strings | Code review — all in `strings.xml` |
| 8 | All Firestore calls have `.addOnFailureListener` | Code review |
| 9 | PR linked to GitHub Issue, reviewed by teammate | GitHub |
| 10 | Stories moved to "Done" on Kanban board | Assignee |
