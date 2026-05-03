# Sprint 4 — US-19 (On-Leave Status) + US-21 (Anonymous Feedback)
### Detailed Implementation Guide

---

## 0. Pre-Sprint Status Report

### Sprint 3 Review — US-01 (Booking), US-05/US-10 (Dashboard), US-20 (Emergency): COMPLETE

Sprint 3 collapsed the original PLAN.md Sprints 3 and 4 into a single sprint, delivering the booking flow, counselor dashboard, and emergency button together. All deliverables verified as complete:

#### US-01 — Booking Flow: COMPLETE

| Deliverable | Status | Notes |
|---|---|---|
| `AppointmentRepository.java` | Done | `bookAppointment()` with atomic Firestore transaction (SLOT_TAKEN three-way callback), `getAppointmentsForCounselor()`, `getAppointmentsForStudent()`, `getAppointmentsForStudentOnDate()`, `updateAppointmentStatus()` |
| `AvailabilityRepository.java` | Done | `getSlotsForCounselor()`, `getAvailableSlotsForCounselor()`, `addSlot()`, `removeSlot()`. Collection standardized on `slots`. |
| `AvailabilitySchedule.java` | Done | `fromSlots()` factory, `getSlotsForDate()`, `getDatesWithAvailability()`. Filters to available-only slots. |
| `BookingActivity.java` rebuild | Done | CalendarView with date selection, date-filtered slot display via AvailabilitySchedule, horizontal slot RecyclerView, uses AppointmentRepository for booking. |
| `BookingConfirmationFragment.java` | Done | BottomSheetDialogFragment showing counselor name, date, time. `OnConfirmListener` callback triggers transaction. |
| `activity_booking.xml` | Done | CalendarView + slot RecyclerView + progress bar. |
| `fragment_booking_confirmation.xml` | Done | Summary with confirm/cancel buttons. |
| `AvailabilityScheduleTest.java` (unit) | Done | 5 tests: filters unavailable, correct date subset, empty for no slots, dates with availability, empty list produces empty schedule. |
| `AppointmentTest.java` (unit) | Done | Empty constructor, setters, status values. |
| `BookingFlowTest.java` (UI) | Done | Calendar displayed, counselor name title, slot list appears on date tap. |

#### US-05/US-10 — Counselor Dashboard: COMPLETE

| Deliverable | Status | Notes |
|---|---|---|
| `CounselorDashboardActivity.java` overhaul | Done | TabLayout (Today / This Week / This Month), master list loaded once, client-side date-range filtering. Stats cards: Today sessions, Total patients, Week count. Uses `AppointmentRepository` + `CounselorRepository`. `onResume()` refreshes. |
| `AppointmentAdapter.java` enhancements | Done | Student name lookup via `UserRepository.getUserName()`. Status badges: CONFIRMED (green), COMPLETED (grey), CANCELLED/NO_SHOW (red with strikethrough). No-Show button wired to `AppointmentRepository.updateAppointmentStatus()`. |
| `AvailabilitySetupActivity.java` | Done | Counselor slot management with add (DatePicker → TimePicker → `AvailabilityRepository.addSlot()`) and swipe-to-delete (`removeSlot()`). |
| `UserRepository.java` — `getUserName()` | Done | Returns "Unknown" on failure (graceful degradation — name is supplementary). |
| `activity_counselor_dashboard.xml` | Done | TabLayout, stat cards, appointment RecyclerView, add-slot banner. |
| `activity_availability_setup.xml` | Done | Add button + slot RecyclerView. |
| `CounselorDashboardTest.java` (UI) | Done | 6 tests: TabLayout displayed, appointment list loads, tabs present, tab switch changes content, add-slot banner. |

#### US-20 — Emergency Button: COMPLETE

| Deliverable | Status | Notes |
|---|---|---|
| `EmergencyDialogFragment.java` | Done | `DialogFragment` with three buttons (Crisis Line, Campus Security, Dismiss). `ACTION_DIAL` intents. Phone numbers from `strings.xml`. Zero network dependency. |
| `StudentHomeActivity.java` — emergency integration | Done | Crisis banner card + FAB. Tapping opens `EmergencyDialogFragment`. |
| `dialog_emergency.xml` | Done | Three-button layout. |
| Emergency strings | Done | `emergency_title`, `call_crisis_line`, `call_campus_security`, `crisis_line_number` (0311-7786264), `campus_security_number` (042-35608000). |
| `EmergencyButtonTest.java` (UI) | Done | 5 tests: FAB visible, dialog opens, crisis line option, campus security option, dismiss button. |

#### Repository Pattern Enforcement: COMPLETE

All Firestore access now flows through repositories:

| Collection | Repository | Activities Using It |
|---|---|---|
| `users` | `UserRepository` | LoginActivity, RegisterActivity, StudentHomeActivity, AppointmentAdapter |
| `counselors` | `CounselorRepository` | CounselorListActivity, CounselorProfileActivity, CounselorProfileEditActivity, CounselorDashboardActivity |
| `slots` | `AvailabilityRepository` | BookingActivity, AvailabilitySetupActivity |
| `appointments` | `AppointmentRepository` | BookingActivity, CounselorDashboardActivity, CalendarActivity, HistoryActivity |

**Exception:** `StudentAppointmentAdapter` still does a direct Firestore query on `counselors` for counselor name lookup. This should be refactored to use `CounselorRepository` during this sprint as a minor cleanup.

---

### What Carries Over Into Sprint 4

Sprint 4 implements the two **secondary** stories from PLAN.md: US-19 (On-Leave) and US-21 (Feedback). Both have partial implementations from earlier sprints that need to be completed.

#### US-19 On-Leave — Existing Partial Implementation

| Component | Current State | What Sprint 4 Must Build |
|---|---|---|
| `Counselor.java` model | **Fields exist:** `onLeave` (Boolean), `onLeaveMessage` (String), `referralCounselorId` (String). Getters/setters present. | No model changes needed. |
| `CounselorProfileActivity.java` | **On-leave display works:** When `onLeave == true`, shows amber card with title + message. "Book Appointment" button disabled, text changed to "Currently Unavailable". | Missing: "See Referred Counselor" button linking to referral counselor's profile. |
| `CounselorAdapter.java` | **On-leave badge works:** Red "Currently Unavailable" badge shown. "View Profile" button disabled. | Missing: Should still allow viewing profile (just disable booking). Missing: "Currently Away" chip instead of blocking profile access entirely. |
| `CounselorProfileEditActivity.java` | **No on-leave controls at all.** Only has bio, language, gender, specialization chips. | Must add: On-Leave toggle (SwitchMaterial), leave message field (TextInputLayout), referral counselor dropdown (populated from `counselors` collection). |
| `CounselorRepository.java` | **No on-leave write method.** `updateCounselorProfile()` uses `set()` which overwrites the whole document — it could write on-leave fields if the Counselor object has them, but there's no dedicated method. | Must add: `setOnLeaveStatus(id, onLeave, message, referralId, callback)` for targeted update of just the on-leave fields. |
| `CounselorListActivity.java` | Filters and displays counselors. On-leave counselors shown with badge but profile blocked. | Update: Allow navigating to on-leave counselor profiles. Add "See Referred Counselor" routing. |

#### US-21 Feedback — Existing Partial Implementation

| Component | Current State | What Sprint 4 Must Build |
|---|---|---|
| `dialog_feedback.xml` | **Layout exists and is complete:** 5-star RatingBar, optional comment EditText, submit button. | No layout changes needed. |
| `StudentHomeActivity.java` | **Feedback dialog UI works:** `showFeedbackDialog()` inflates the dialog, shows rating + comment, validates rating > 0. But on submit: **only shows a toast** — no Firestore persistence. No link to any appointment. | Must wire to `FeedbackRepository`. Must trigger only for COMPLETED appointments. Must pass appointmentId. Must track dismissed state. |
| `FeedbackService.java` | **Does not exist.** | Must create: Firestore-mapped model with appointmentId, rating, comment, submittedAt. **No studentId field** — anonymity enforced at schema level. |
| `FeedbackRepository.java` | **Does not exist.** | Must create: `submitFeedback()`, `hasFeedbackForAppointment()`. |
| `AppointmentRepository.java` | Exists. Has `getAppointmentsForStudent()`. | Must add: `getCompletedAppointmentsWithoutFeedback()` — query for COMPLETED appointments where feedback has not been submitted. |
| Firestore `feedback` collection | **Does not exist** in live data. | Created when first feedback is submitted. Schema: `{appointmentId, rating, comment, submittedAt}`. |

---

## 1. Sprint 4 Objective

By the end of this sprint:

1. **A counselor can toggle their on-leave status** from their profile edit screen, set a custom leave message, and optionally select a referral colleague — all persisted to Firestore. Students see the on-leave state in the directory and on the counselor's profile, with a link to the referred counselor (US-19).
2. **A student sees a feedback prompt after a completed appointment**, can submit a 1–5 star rating with optional comment, and the feedback is anonymously persisted to Firestore with no `studentId` (US-21).

### Why These Two Stories Together

Both are secondary stories that enrich the core loop:
- US-19 adds graceful handling when a counselor is temporarily unavailable — students are redirected rather than hitting a dead end.
- US-21 closes the appointment lifecycle loop: book → attend → feedback.
- Neither depends on the other, so they can be developed in parallel by different team members.

---

## 2. Files to Create or Modify

### 2.1 New Files

```
src/main/java/com/example/moogerscouncil/
├── FeedbackService.java                 // Model — anonymous feedback data container
├── FeedbackRepository.java              // Repository — feedback collection ops

src/main/res/layout/
├── (no new layouts — dialog_feedback.xml already exists)

src/test/java/com/example/moogerscouncil/
├── FeedbackServiceTest.java             // Unit test — no studentId, rating bounds

src/androidTest/java/com/example/moogerscouncil/
├── OnLeaveFlowTest.java                 // UI test — toggle, save, directory badge
├── FeedbackFlowTest.java                // UI test — dialog, submit, persistence
```

### 2.2 Files to Modify

```
CounselorProfileEditActivity.java   // Add on-leave toggle, message field, referral dropdown
CounselorRepository.java            // Add setOnLeaveStatus() method
CounselorProfileActivity.java       // Add "See Referred Counselor" button
CounselorAdapter.java               // Allow profile navigation for on-leave counselors, show "Away" chip
CounselorListActivity.java          // Update on-leave counselor tap behavior
StudentHomeActivity.java            // Wire feedback dialog to FeedbackRepository, trigger on COMPLETED appointments
AppointmentRepository.java          // Add getCompletedAppointmentsWithoutFeedback()
activity_counselor_profile_edit.xml // Add on-leave section (toggle, message, dropdown)
activity_counselor_profile.xml      // Add "See Referred Counselor" button
strings.xml                         // All new user-facing strings
AndroidManifest.xml                 // No new activities needed
```

---

## 3. Implementation Details — US-19: On-Leave Status

### 3.1 `CounselorRepository.java` — New Method

Add a targeted update for on-leave fields. This avoids overwriting the entire counselor document when only the leave status changes.

```java
/**
 * Updates the on-leave status fields for a counselor.
 * Sets onLeave, onLeaveMessage, and referralCounselorId atomically.
 *
 * @param counselorId       The Firestore document ID.
 * @param onLeave           Whether the counselor is on leave.
 * @param leaveMessage      Custom message shown to students (nullable).
 * @param referralId        Document ID of the referral counselor (nullable).
 * @param callback          Success/failure callback.
 */
public void setOnLeaveStatus(String counselorId, boolean onLeave,
                              String leaveMessage, String referralId,
                              OnUpdateCallback callback) {
    Map<String, Object> updates = new HashMap<>();
    updates.put("onLeave", onLeave);
    updates.put("onLeaveMessage", leaveMessage);
    updates.put("referralCounselorId", referralId);

    counselorsCollection.document(counselorId)
        .update(updates)
        .addOnSuccessListener(unused -> callback.onSuccess())
        .addOnFailureListener(callback::onFailure);
}
```

**Why a dedicated method instead of reusing `updateCounselorProfile()`?**
`updateCounselorProfile()` uses `set()` which overwrites the entire document. If a counselor is editing their on-leave status from a quick-toggle, you don't want to accidentally blank out specializations or bio because those fields weren't loaded into the form. A targeted `update()` only touches the three leave fields.

### 3.2 `CounselorProfileEditActivity.java` — On-Leave Section

**What to add to the layout (`activity_counselor_profile_edit.xml`):**

Below the existing specialization chips section, add an on-leave section:

```xml
<!-- On-Leave Section -->
<View
    android:layout_width="match_parent"
    android:layout_height="1dp"
    android:background="#E0E0E0"
    android:layout_marginTop="24dp"
    android:layout_marginBottom="16dp" />

<TextView
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="@string/label_on_leave_section"
    android:textSize="16sp"
    android:textStyle="bold"
    android:layout_marginBottom="8dp" />

<com.google.android.material.switchmaterial.SwitchMaterial
    android:id="@+id/switchOnLeave"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:text="@string/label_on_leave_toggle"
    android:textSize="14sp"
    android:layout_marginBottom="8dp" />

<com.google.android.material.textfield.TextInputLayout
    android:id="@+id/layoutLeaveMessage"
    style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:hint="@string/hint_leave_message"
    android:visibility="gone"
    android:layout_marginBottom="8dp">
    <com.google.android.material.textfield.TextInputEditText
        android:id="@+id/editTextLeaveMessage"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:inputType="textMultiLine"
        android:minLines="2" />
</com.google.android.material.textfield.TextInputLayout>

<com.google.android.material.textfield.TextInputLayout
    android:id="@+id/layoutReferralCounselor"
    style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox.ExposedDropdownMenu"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:hint="@string/hint_referral_counselor"
    android:visibility="gone"
    android:layout_marginBottom="16dp">
    <AutoCompleteTextView
        android:id="@+id/dropdownReferral"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:inputType="none" />
</com.google.android.material.textfield.TextInputLayout>
```

**Java logic in `CounselorProfileEditActivity`:**

```java
private SwitchMaterial switchOnLeave;
private TextInputLayout layoutLeaveMessage;
private TextInputLayout layoutReferralCounselor;
private AutoCompleteTextView dropdownReferral;
private List<Counselor> allCounselors = new ArrayList<>();
private String selectedReferralId = null;

// In onCreate, after existing field setup:
switchOnLeave = findViewById(R.id.switchOnLeave);
layoutLeaveMessage = findViewById(R.id.layoutLeaveMessage);
layoutReferralCounselor = findViewById(R.id.layoutReferralCounselor);
dropdownReferral = findViewById(R.id.dropdownReferral);

// Toggle visibility of leave fields
switchOnLeave.setOnCheckedChangeListener((buttonView, isChecked) -> {
    int visibility = isChecked ? View.VISIBLE : View.GONE;
    layoutLeaveMessage.setVisibility(visibility);
    layoutReferralCounselor.setVisibility(visibility);
    if (isChecked) {
        loadReferralCounselors();
    }
});
```

**Loading the referral counselor dropdown:**

```java
private void loadReferralCounselors() {
    String currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
    counselorRepository.getAllCounselors(
        new CounselorRepository.OnCounselorsLoadedCallback() {
            @Override
            public void onSuccess(List<Counselor> counselors) {
                allCounselors = counselors;
                List<String> names = new ArrayList<>();
                names.add("None (optional)");
                for (Counselor c : counselors) {
                    // Exclude self from referral list
                    if (!currentUid.equals(c.getUid())) {
                        names.add(c.getName());
                    }
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    CounselorProfileEditActivity.this,
                    android.R.layout.simple_list_item_1, names);
                dropdownReferral.setAdapter(adapter);

                dropdownReferral.setOnItemClickListener((parent, view, pos, id) -> {
                    if (pos == 0) {
                        selectedReferralId = null;
                    } else {
                        // Adjust for "None" at position 0 and self-exclusion
                        int counselorIndex = 0;
                        for (int i = 0; i < allCounselors.size(); i++) {
                            if (!currentUid.equals(allCounselors.get(i).getUid())) {
                                counselorIndex++;
                                if (counselorIndex == pos) {
                                    selectedReferralId = allCounselors.get(i).getId();
                                    break;
                                }
                            }
                        }
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(CounselorProfileEditActivity.this,
                    getString(R.string.error_loading_counselors),
                    Toast.LENGTH_SHORT).show();
            }
        });
}
```

**Pre-filling on-leave fields when editing existing profile:**

```java
// In the getCounselor() onSuccess callback, after existing field pre-fill:
if (counselor.getOnLeave() != null && counselor.getOnLeave()) {
    switchOnLeave.setChecked(true);
    // Visibility is triggered by the listener
    if (counselor.getOnLeaveMessage() != null) {
        editTextLeaveMessage.setText(counselor.getOnLeaveMessage());
    }
    // Referral pre-selection happens after dropdown loads
}
```

**Save button update — add on-leave fields to the save action:**

The existing save logic builds a Counselor object and calls `updateCounselorProfile()`. Add the on-leave fields:

```java
// In the save button click handler, before calling repository:
counselor.setOnLeave(switchOnLeave.isChecked());
if (switchOnLeave.isChecked()) {
    counselor.setOnLeaveMessage(editTextLeaveMessage.getText().toString().trim());
    counselor.setReferralCounselorId(selectedReferralId);
} else {
    // Clear leave fields when toggling off
    counselor.setOnLeave(false);
    counselor.setOnLeaveMessage(null);
    counselor.setReferralCounselorId(null);
}
```

**Alternatively**, for a quick-toggle scenario (e.g., a future shortcut from the dashboard), use the dedicated `setOnLeaveStatus()` method:

```java
counselorRepository.setOnLeaveStatus(counselorId,
    switchOnLeave.isChecked(),
    editTextLeaveMessage.getText().toString().trim(),
    selectedReferralId,
    new CounselorRepository.OnUpdateCallback() {
        @Override
        public void onSuccess() {
            Toast.makeText(..., getString(R.string.success_leave_status_updated), ...);
            finish();
        }
        @Override
        public void onFailure(Exception e) {
            Toast.makeText(..., getString(R.string.error_saving_leave_status), ...);
        }
    });
```

### 3.3 `CounselorProfileActivity.java` — "See Referred Counselor" Button

When a counselor is on leave and has a `referralCounselorId`, show a button that navigates to the referred counselor's profile.

**Layout addition in `activity_counselor_profile.xml`**, inside the on-leave card:

```xml
<com.google.android.material.button.MaterialButton
    android:id="@+id/buttonSeeReferral"
    style="@style/Widget.MaterialComponents.Button.OutlinedButton"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:text="@string/button_see_referred_counselor"
    android:visibility="gone"
    android:layout_marginTop="8dp" />
```

**Java logic:**

```java
// In the counselor profile load callback, when on-leave card is shown:
if (counselor.getReferralCounselorId() != null
    && !counselor.getReferralCounselorId().isEmpty()) {
    buttonSeeReferral.setVisibility(View.VISIBLE);
    buttonSeeReferral.setOnClickListener(v -> {
        Intent intent = new Intent(this, CounselorProfileActivity.class);
        intent.putExtra("COUNSELOR_ID", counselor.getReferralCounselorId());
        startActivity(intent);
    });
} else {
    buttonSeeReferral.setVisibility(View.GONE);
}
```

### 3.4 `CounselorAdapter.java` — Fix On-Leave Behavior

**Current problem:** On-leave counselors have their "View Profile" button disabled and profile navigation blocked entirely. Per US-19 acceptance criteria, students should still be able to view the profile — they just can't book.

**Fix:**

```java
// BEFORE (blocks navigation entirely):
if (counselor.getOnLeave() != null && counselor.getOnLeave()) {
    holder.buttonViewProfile.setEnabled(false);
    holder.buttonViewProfile.setText(R.string.currently_unavailable);
}

// AFTER (allows profile viewing, shows "Away" chip):
if (counselor.getOnLeave() != null && counselor.getOnLeave()) {
    holder.badgeOnLeave.setVisibility(View.VISIBLE);
    holder.badgeOnLeave.setText(R.string.currently_away);
    // Button still enabled — navigates to profile where on-leave card is shown
    holder.buttonViewProfile.setEnabled(true);
    holder.buttonViewProfile.setText(R.string.view_profile);
} else {
    holder.badgeOnLeave.setVisibility(View.GONE);
    holder.buttonViewProfile.setEnabled(true);
    holder.buttonViewProfile.setText(R.string.view_profile);
}
```

The profile itself (CounselorProfileActivity) already handles disabling the booking button and showing the on-leave card with the leave message. The adapter just needs to stop blocking navigation.

---

## 4. Implementation Details — US-21: Anonymous Feedback

### 4.1 `FeedbackService.java` — Model Class

**Purpose:** Firestore-mapped data container for the `feedback` collection. Deliberately has **no `studentId` field** — anonymity is enforced at the schema level.

```java
package com.example.moogerscouncil;

import com.google.firebase.Timestamp;

/**
 * Represents an anonymous post-session feedback submission.
 * Maps directly to a document in the Firestore 'feedback' collection.
 *
 * IMPORTANT: This class intentionally has no studentId field.
 * Anonymity is enforced at the schema level — the feedback document
 * cannot be traced back to the student who submitted it. This is a
 * design decision, not an oversight.
 *
 * This class follows the Firestore model convention: no-argument
 * constructor, private fields, public getters/setters.
 */
public class FeedbackService {

    private String id;
    private String appointmentId;
    private int rating;           // 1–5
    private String comment;       // optional
    private Timestamp submittedAt;

    /** Required empty constructor for Firestore deserialization. */
    public FeedbackService() {}

    /**
     * Constructs a FeedbackService with all fields.
     *
     * @param appointmentId The appointment this feedback relates to.
     * @param rating        Star rating, 1–5.
     * @param comment       Optional text comment (may be null or empty).
     */
    public FeedbackService(String appointmentId, int rating, String comment) {
        this.appointmentId = appointmentId;
        this.rating = rating;
        this.comment = comment;
        this.submittedAt = Timestamp.now();
    }

    // --- Getters ---

    public String getId() { return id; }
    public String getAppointmentId() { return appointmentId; }
    public int getRating() { return rating; }
    public String getComment() { return comment; }
    public Timestamp getSubmittedAt() { return submittedAt; }

    // --- Setters (required by Firestore) ---

    public void setId(String id) { this.id = id; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }
    public void setRating(int rating) { this.rating = rating; }
    public void setComment(String comment) { this.comment = comment; }
    public void setSubmittedAt(Timestamp submittedAt) { this.submittedAt = submittedAt; }
}
```

**Why the class is called `FeedbackService` and not `Feedback`:** This follows the CRC card naming from the README.md design artifacts. The CRC card is named "FeedbackService" — maintaining naming consistency between design and implementation makes traceability clearer.

### 4.2 `FeedbackRepository.java` — Repository

```java
package com.example.moogerscouncil;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Repository for the Firestore 'feedback' collection.
 * All reads and writes to feedback documents flow through this class.
 *
 * Key design constraint: feedback documents never contain a studentId.
 * This repository enforces anonymity by only accepting FeedbackService
 * objects (which have no studentId field) for writes.
 *
 * Follows the Repository design pattern consistent with
 * UserRepository, CounselorRepository, AppointmentRepository,
 * and AvailabilityRepository.
 */
public class FeedbackRepository {

    private final CollectionReference feedbackCollection;

    public FeedbackRepository() {
        this.feedbackCollection = FirebaseFirestore.getInstance()
            .collection("feedback");
    }

    // --- Callback interfaces ---

    public interface OnFeedbackSubmittedCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface OnFeedbackCheckCallback {
        void onResult(boolean feedbackExists);
    }

    // --- Write operations ---

    /**
     * Submits anonymous feedback for a completed appointment.
     * The FeedbackService object must not contain a studentId —
     * anonymity is enforced at the model level.
     *
     * @param feedback The feedback to persist.
     * @param callback Success/failure callback.
     */
    public void submitFeedback(FeedbackService feedback,
                                OnFeedbackSubmittedCallback callback) {
        feedbackCollection.add(feedback)
            .addOnSuccessListener(docRef -> callback.onSuccess())
            .addOnFailureListener(callback::onFailure);
    }

    // --- Read operations ---

    /**
     * Checks whether feedback has already been submitted for a given appointment.
     * Used to determine whether to show the feedback prompt on StudentHomeActivity.
     *
     * @param appointmentId The appointment to check.
     * @param callback      Receives true if feedback exists, false otherwise.
     */
    public void hasFeedbackForAppointment(String appointmentId,
                                           OnFeedbackCheckCallback callback) {
        feedbackCollection
            .whereEqualTo("appointmentId", appointmentId)
            .limit(1)
            .get()
            .addOnSuccessListener(querySnapshot ->
                callback.onResult(!querySnapshot.isEmpty()))
            .addOnFailureListener(e ->
                callback.onResult(false)); // On failure, assume no feedback (don't block UI)
    }
}
```

**Why `hasFeedbackForAppointment` uses `limit(1)`:** We only need to know if at least one feedback document exists for this appointment. Fetching the full set would be wasteful. The `limit(1)` makes this an existence check.

**Why failure returns `false`:** If the Firestore query fails (network issues), we'd rather show the feedback prompt again than silently suppress it. The worst case is the student sees the prompt twice — better than losing feedback entirely.

### 4.3 `AppointmentRepository.java` — Extension

Add a method to find completed appointments that haven't had feedback submitted yet.

```java
/**
 * Fetches completed appointments for a student that don't yet have
 * feedback submitted. Used by StudentHomeActivity to show feedback prompts.
 *
 * This method first fetches all COMPLETED appointments for the student,
 * then checks each against the feedback collection. This two-step
 * approach is necessary because Firestore doesn't support cross-collection
 * joins.
 *
 * @param studentId The student's UID.
 * @param callback  Receives appointments needing feedback.
 */
public void getCompletedAppointmentsNeedingFeedback(
        String studentId,
        OnAppointmentsLoadedCallback callback) {
    appointmentsCollection
        .whereEqualTo("studentId", studentId)
        .whereEqualTo("status", "COMPLETED")
        .get()
        .addOnSuccessListener(querySnapshot -> {
            List<Appointment> completed = querySnapshot.toObjects(Appointment.class);
            for (int i = 0; i < completed.size(); i++) {
                completed.get(i).setId(
                    querySnapshot.getDocuments().get(i).getId());
            }
            callback.onSuccess(completed);
        })
        .addOnFailureListener(callback::onFailure);
}
```

**The two-step filtering process in `StudentHomeActivity`:**

```java
// Step 1: Get completed appointments
appointmentRepository.getCompletedAppointmentsNeedingFeedback(studentId,
    new AppointmentRepository.OnAppointmentsLoadedCallback() {
        @Override
        public void onSuccess(List<Appointment> completedAppointments) {
            // Step 2: For each, check if feedback exists
            for (Appointment appt : completedAppointments) {
                feedbackRepository.hasFeedbackForAppointment(appt.getId(),
                    feedbackExists -> {
                        if (!feedbackExists) {
                            showFeedbackPrompt(appt);
                        }
                    });
            }
        }

        @Override
        public void onFailure(Exception e) {
            // Silently fail — feedback prompts are optional
        }
    });
```

### 4.4 `StudentHomeActivity.java` — Feedback Integration

**Current state:** `showFeedbackDialog()` method exists, inflates `dialog_feedback.xml`, shows RatingBar + comment, but on submit just shows a toast and dismisses. No appointment context. No Firestore write.

**Sprint 4 changes:**

#### 4.4.1 Add Feedback Prompt Card

Instead of the feedback dialog being triggered only by a static button, it should be triggered dynamically when a COMPLETED appointment needs feedback. Add a dismissible card to the student home layout:

```xml
<!-- In activity_student_home.xml, above the action cards -->
<androidx.cardview.widget.CardView
    android:id="@+id/cardFeedbackPrompt"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_margin="16dp"
    app:cardCornerRadius="12dp"
    app:cardElevation="2dp"
    android:visibility="gone">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="16dp">

        <TextView
            android:id="@+id/textFeedbackPromptTitle"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/feedback_prompt_title"
            android:textSize="16sp"
            android:textStyle="bold" />

        <TextView
            android:id="@+id/textFeedbackPromptSubtitle"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textSize="14sp"
            android:textColor="#666666"
            android:layout_marginTop="4dp" />

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:layout_marginTop="12dp">

            <com.google.android.material.button.MaterialButton
                android:id="@+id/buttonGiveFeedback"
                style="@style/Widget.MaterialComponents.Button"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="@string/button_give_feedback"
                android:layout_marginEnd="8dp" />

            <com.google.android.material.button.MaterialButton
                android:id="@+id/buttonDismissFeedback"
                style="@style/Widget.MaterialComponents.Button.OutlinedButton"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="@string/button_dismiss"
                android:layout_marginStart="8dp" />
        </LinearLayout>
    </LinearLayout>
</androidx.cardview.widget.CardView>
```

#### 4.4.2 Wire Feedback Flow

```java
private FeedbackRepository feedbackRepository;
private AppointmentRepository appointmentRepository;
private Appointment pendingFeedbackAppointment; // The appointment awaiting feedback

// In onCreate:
feedbackRepository = new FeedbackRepository();
appointmentRepository = new AppointmentRepository();

checkForPendingFeedback();

private void checkForPendingFeedback() {
    String studentId = FirebaseAuth.getInstance().getCurrentUser().getUid();

    appointmentRepository.getCompletedAppointmentsNeedingFeedback(studentId,
        new AppointmentRepository.OnAppointmentsLoadedCallback() {
            @Override
            public void onSuccess(List<Appointment> completed) {
                if (completed.isEmpty()) return;

                // Check the most recent completed appointment first
                Appointment latest = completed.get(completed.size() - 1);
                feedbackRepository.hasFeedbackForAppointment(latest.getId(),
                    exists -> {
                        if (!exists) {
                            pendingFeedbackAppointment = latest;
                            showFeedbackPromptCard(latest);
                        }
                    });
            }

            @Override
            public void onFailure(Exception e) { /* silent */ }
        });
}

private void showFeedbackPromptCard(Appointment appointment) {
    cardFeedbackPrompt.setVisibility(View.VISIBLE);

    // Look up counselor name for the prompt
    CounselorRepository counselorRepo = new CounselorRepository();
    counselorRepo.getCounselor(appointment.getCounselorId(),
        new CounselorRepository.OnCounselorFetchedCallback() {
            @Override
            public void onSuccess(Counselor counselor) {
                textFeedbackPromptSubtitle.setText(
                    getString(R.string.feedback_prompt_subtitle, counselor.getName()));
            }
            @Override
            public void onFailure(Exception e) {
                textFeedbackPromptSubtitle.setText(
                    getString(R.string.feedback_prompt_subtitle_generic));
            }
        });

    buttonGiveFeedback.setOnClickListener(v ->
        showFeedbackDialog(appointment.getId()));

    buttonDismissFeedback.setOnClickListener(v -> {
        cardFeedbackPrompt.setVisibility(View.GONE);
        // Mark as dismissed so it doesn't reappear this session
        pendingFeedbackAppointment = null;
    });
}
```

#### 4.4.3 Update `showFeedbackDialog()` to Persist

```java
private void showFeedbackDialog(String appointmentId) {
    View dialogView = getLayoutInflater().inflate(R.layout.dialog_feedback, null);
    RatingBar ratingBar = dialogView.findViewById(R.id.ratingBar);
    EditText editComment = dialogView.findViewById(R.id.editComment);
    MaterialButton submitButton = dialogView.findViewById(R.id.btnSubmitFeedback);

    AlertDialog dialog = new AlertDialog.Builder(this)
        .setView(dialogView)
        .create();
    dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

    submitButton.setOnClickListener(v -> {
        int rating = (int) ratingBar.getRating();
        if (rating == 0) {
            Toast.makeText(this, getString(R.string.error_rating_required),
                Toast.LENGTH_SHORT).show();
            return;
        }

        String comment = editComment.getText().toString().trim();

        // Create anonymous feedback — no studentId
        FeedbackService feedback = new FeedbackService(appointmentId, rating, comment);

        feedbackRepository.submitFeedback(feedback,
            new FeedbackRepository.OnFeedbackSubmittedCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(StudentHomeActivity.this,
                        getString(R.string.feedback_submitted),
                        Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    cardFeedbackPrompt.setVisibility(View.GONE);
                    pendingFeedbackAppointment = null;
                }

                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(StudentHomeActivity.this,
                        getString(R.string.error_feedback_submission),
                        Toast.LENGTH_LONG).show();
                }
            });
    });

    dialog.show();
}
```

**How the full feedback flow connects:**

```
StudentHomeActivity.onCreate()
    │
    ├── checkForPendingFeedback()
    │     └── AppointmentRepository.getCompletedAppointmentsNeedingFeedback(studentId)
    │           └── onSuccess(completedList):
    │                 └── FeedbackRepository.hasFeedbackForAppointment(latestApptId)
    │                       └── if no feedback exists:
    │                             └── showFeedbackPromptCard(appointment)
    │                                   ├── Fetch counselor name for display
    │                                   ├── "Give Feedback" → showFeedbackDialog(apptId)
    │                                   └── "Dismiss" → hide card
    │
    └── showFeedbackDialog(appointmentId)
          ├── User rates 1–5 stars
          ├── User types optional comment
          └── Submit:
                └── FeedbackService(appointmentId, rating, comment) ← no studentId
                      └── FeedbackRepository.submitFeedback(feedback)
                            ├── onSuccess: dismiss dialog, hide prompt card
                            └── onFailure: show error toast
```

---

## 5. Implementation Details — Strings

```xml
<!-- On-Leave (US-19) -->
<string name="label_on_leave_section">Leave Status</string>
<string name="label_on_leave_toggle">I am currently on leave</string>
<string name="hint_leave_message">Leave Message</string>
<string name="helper_leave_message">Message shown to students who view your profile</string>
<string name="hint_referral_counselor">Refer Students To</string>
<string name="helper_referral_counselor">Select a colleague to handle your students</string>
<string name="currently_away">Currently Away</string>
<string name="button_see_referred_counselor">See Referred Counselor</string>
<string name="success_leave_status_updated">Leave status updated</string>
<string name="error_saving_leave_status">Failed to update leave status</string>

<!-- Feedback (US-21) -->
<string name="feedback_prompt_title">How was your session?</string>
<string name="feedback_prompt_subtitle">Share your experience with %s</string>
<string name="feedback_prompt_subtitle_generic">Share your experience</string>
<string name="button_give_feedback">Give Feedback</string>
<string name="button_dismiss">Dismiss</string>
<string name="error_rating_required">Please select a rating</string>
<string name="feedback_submitted">Thank you for your feedback!</string>
<string name="error_feedback_submission">Unable to submit feedback. Please try again.</string>
```

---

## 6. Data Flow Diagrams

### 6.1 On-Leave Toggle Flow (Counselor-side)

```
Counselor          ProfileEditActivity      CounselorRepo         Firestore
  │                       │                      │                    │
  │  taps "Edit Profile" │                      │                    │
  │ ─────────────────────►│                      │                    │
  │                       │  getCounselor(uid)   │                    │
  │                       │ ────────────────────►│                    │
  │                       │                      │  .get()            │
  │                       │                      │ ──────────────────►│
  │                       │  onSuccess(counselor)│                    │
  │                       │ ◄────────────────────│◄───────────────────│
  │                       │                      │                    │
  │                       │  Pre-fill fields     │                    │
  │                       │  switchOnLeave =     │                    │
  │                       │  counselor.onLeave   │                    │
  │  sees current state   │                      │                    │
  │ ◄─────────────────────│                      │                    │
  │                       │                      │                    │
  │  toggles On Leave ON  │                      │                    │
  │ ─────────────────────►│                      │                    │
  │                       │  Show message field  │                    │
  │                       │  Show referral       │                    │
  │                       │  dropdown            │                    │
  │                       │                      │                    │
  │                       │  getAllCounselors()   │                    │
  │                       │ ────────────────────►│                    │
  │                       │  onSuccess(list)     │                    │
  │                       │ ◄────────────────────│                    │
  │                       │  Populate dropdown   │                    │
  │                       │  (exclude self)      │                    │
  │                       │                      │                    │
  │  types message,       │                      │                    │
  │  selects referral,    │                      │                    │
  │  taps "Save"          │                      │                    │
  │ ─────────────────────►│                      │                    │
  │                       │  updateCounselorProfile()                 │
  │                       │  (with onLeave=true, │                    │
  │                       │   message, referralId)│                   │
  │                       │ ────────────────────►│                    │
  │                       │                      │  .set(counselor)   │
  │                       │                      │ ──────────────────►│
  │                       │  onSuccess()         │                    │
  │                       │ ◄────────────────────│◄───────────────────│
  │  Toast + finish()     │                      │                    │
  │ ◄─────────────────────│                      │                    │
```

### 6.2 On-Leave Display Flow (Student-side)

```
Student            CounselorListActivity    CounselorProfileActivity    Firestore
  │                       │                         │                       │
  │  browses directory    │                         │                       │
  │ ─────────────────────►│                         │                       │
  │                       │  (counselors loaded)    │                       │
  │                       │                         │                       │
  │  sees "Away" chip     │                         │                       │
  │  on Dr. Smith's card  │                         │                       │
  │ ◄─────────────────────│                         │                       │
  │                       │                         │                       │
  │  taps card (allowed)  │                         │                       │
  │ ─────────────────────►│                         │                       │
  │                       │  → CounselorProfile     │                       │
  │                       │  (counselorId)          │                       │
  │                       │ ───────────────────────►│                       │
  │                       │                         │  getCounselor()       │
  │                       │                         │ ─────────────────────►│
  │                       │                         │  counselor (onLeave)  │
  │                       │                         │ ◄─────────────────────│
  │                       │                         │                       │
  │  sees on-leave card:  │                         │                       │
  │  "Currently On Leave" │                         │                       │
  │  + leave message      │                         │                       │
  │  + "See Referred      │                         │                       │
  │    Counselor" button  │                         │                       │
  │  Book button disabled │                         │                       │
  │ ◄──────────────────────────────────────────────│                       │
  │                       │                         │                       │
  │  taps "See Referred"  │                         │                       │
  │ ──────────────────────────────────────────────►│                       │
  │                       │                         │  → CounselorProfile   │
  │                       │                         │  (referralCounselorId)│
  │                       │                         │  new instance         │
```

### 6.3 Feedback Submission Flow

```
Student           StudentHomeActivity      FeedbackRepo      AppointmentRepo     Firestore
  │                      │                      │                  │                │
  │  opens home screen   │                      │                  │                │
  │ ────────────────────►│                      │                  │                │
  │                      │  getCompleted...     │                  │                │
  │                      │  NeedingFeedback()   │                  │                │
  │                      │ ─────────────────────┼─────────────────►│                │
  │                      │                      │                  │  query COMPLETED│
  │                      │                      │                  │ ───────────────►│
  │                      │                      │                  │  appt list      │
  │                      │                      │                  │ ◄───────────────│
  │                      │  onSuccess(appts)    │                  │                │
  │                      │ ◄────────────────────┼──────────────────│                │
  │                      │                      │                  │                │
  │                      │ hasFeedback(apptId)  │                  │                │
  │                      │ ────────────────────►│                  │                │
  │                      │                      │  query feedback  │                │
  │                      │                      │ ─────────────────┼───────────────►│
  │                      │                      │  empty (none)    │                │
  │                      │                      │ ◄────────────────┼────────────────│
  │                      │  onResult(false)     │                  │                │
  │                      │ ◄────────────────────│                  │                │
  │                      │                      │                  │                │
  │  sees feedback card: │                      │                  │                │
  │  "How was your       │                      │                  │                │
  │   session with       │                      │                  │                │
  │   Dr. Smith?"        │                      │                  │                │
  │ ◄────────────────────│                      │                  │                │
  │                      │                      │                  │                │
  │  taps "Give Feedback"│                      │                  │                │
  │ ────────────────────►│                      │                  │                │
  │                      │  showFeedbackDialog  │                  │                │
  │  sees rating + text  │  (appointmentId)     │                  │                │
  │ ◄────────────────────│                      │                  │                │
  │                      │                      │                  │                │
  │  rates 4 stars,      │                      │                  │                │
  │  types comment,      │                      │                  │                │
  │  taps Submit         │                      │                  │                │
  │ ────────────────────►│                      │                  │                │
  │                      │ submitFeedback(      │                  │                │
  │                      │  FeedbackService(    │                  │                │
  │                      │   apptId, 4, comment │                  │                │
  │                      │   ← NO studentId)    │                  │                │
  │                      │ ────────────────────►│                  │                │
  │                      │                      │  .add(feedback)  │                │
  │                      │                      │ ─────────────────┼───────────────►│
  │                      │                      │  success         │                │
  │                      │                      │ ◄────────────────┼────────────────│
  │                      │  onSuccess()         │                  │                │
  │                      │ ◄────────────────────│                  │                │
  │  Toast "Thank you!"  │                      │                  │                │
  │  card disappears     │                      │                  │                │
  │ ◄────────────────────│                      │                  │                │
```

---

## 7. Testing Plan

### 7.1 Unit Tests

#### `FeedbackServiceTest.java`

| Test | What it verifies |
|---|---|
| `testNoStudentIdField` | FeedbackService has no getStudentId() or setStudentId() method. Verify via reflection that no "studentId" field exists. |
| `testConstructorSetsFields` | appointmentId, rating, comment are correctly set. submittedAt is not null. |
| `testEmptyConstructorForFirestore` | No-arg constructor creates instance with null/zero fields. |
| `testRatingBounds` | Rating can be set to 1 and 5 (boundary values). |
| `testNullCommentAllowed` | Comment can be null (feedback is optional text). |

### 7.2 UI Tests (Espresso)

#### `OnLeaveFlowTest.java`

| Test | What it asserts |
|---|---|
| `testOnLeaveToggleIsDisplayed` | SwitchMaterial for on-leave is visible in CounselorProfileEditActivity |
| `testToggleShowsMessageField` | Toggling on-leave shows the leave message TextInputLayout |
| `testToggleShowsReferralDropdown` | Toggling on-leave shows the referral counselor dropdown |
| `testOnLeaveCounselorShowsBadgeInDirectory` | Counselor with onLeave=true shows "Currently Away" badge in CounselorListActivity |
| `testOnLeaveCounselorProfileShowsLeaveCard` | CounselorProfileActivity shows on-leave card for on-leave counselor |

#### `FeedbackFlowTest.java`

| Test | What it asserts |
|---|---|
| `testFeedbackDialogDisplaysRatingBar` | RatingBar is visible in the feedback dialog |
| `testFeedbackDialogDisplaysCommentField` | Comment EditText is visible |
| `testSubmitWithZeroRatingShowsError` | Submitting without rating shows error toast |
| `testFeedbackPromptCardAppears` | Feedback prompt card appears when a COMPLETED appointment exists without feedback |

---

## 8. Task Breakdown and Sequencing

```
┌──────────────────────────────────────────────────────┐
│ Phase A — Model + Repository (no UI dependency)       │
│                                                      │
│  A1. FeedbackService.java (model)                    │
│  A2. FeedbackRepository.java (repository)            │
│  A3. CounselorRepository — add setOnLeaveStatus()    │
│  A4. AppointmentRepository — add                     │
│      getCompletedAppointmentsNeedingFeedback()       │
│  A5. FeedbackServiceTest.java (unit tests)           │
│                                                      │
│  All can be done in parallel.                        │
└──────────────────────┬───────────────────────────────┘
                       │
┌──────────────────────▼───────────────────────────────┐
│ Phase B — On-Leave UI (depends on A3)                │
│                                                      │
│  B1. CounselorProfileEditActivity — add toggle,      │
│      message field, referral dropdown                │
│  B2. activity_counselor_profile_edit.xml — layout     │
│      additions                                       │
│  B3. CounselorProfileActivity — "See Referred        │
│      Counselor" button                               │
│  B4. CounselorAdapter — fix on-leave behavior        │
│      (allow profile nav, "Away" chip)                │
│  B5. strings.xml — on-leave strings                  │
│                                                      │
│  B1/B2 together. B3/B4 can parallel B1.              │
└──────────────────────┬───────────────────────────────┘
                       │
┌──────────────────────▼───────────────────────────────┐
│ Phase C — Feedback UI (depends on A1, A2, A4)        │
│                                                      │
│  C1. StudentHomeActivity — feedback prompt card,     │
│      wire showFeedbackDialog to FeedbackRepository,  │
│      checkForPendingFeedback() on load               │
│  C2. activity_student_home.xml — feedback prompt     │
│      card layout addition                            │
│  C3. strings.xml — feedback strings                  │
│                                                      │
│  C1/C2 can be done in parallel with Phase B.         │
└──────────────────────┬───────────────────────────────┘
                       │
┌──────────────────────▼───────────────────────────────┐
│ Phase D — Testing + Cleanup                          │
│                                                      │
│  D1. OnLeaveFlowTest.java (Espresso)                 │
│  D2. FeedbackFlowTest.java (Espresso)                │
│  D3. Refactor StudentAppointmentAdapter to use       │
│      CounselorRepository (minor cleanup)             │
│  D4. Javadoc pass on all new files                   │
│  D5. Move US-19 + US-21 to Done on Kanban board      │
└──────────────────────────────────────────────────────┘
```

**Parallelization opportunity:** Phase B (On-Leave UI) and Phase C (Feedback UI) are fully independent — they touch different files and different Firestore collections. Two team members can work on them simultaneously after Phase A is complete.

---

## 9. Acceptance Criteria Checklist

### US-19 — Counselor On-Leave Status

- [ ] In `CounselorProfileEditActivity`, an "On Leave" toggle (SwitchMaterial) is available
- [ ] When toggled on, leave message field and referral counselor dropdown appear
- [ ] Referral dropdown is populated from `counselors` collection, excluding the current counselor
- [ ] Saving writes `onLeave: true`, `onLeaveMessage`, `referralCounselorId` to Firestore
- [ ] In the directory, on-leave counselors show "Currently Away" chip
- [ ] On-leave counselors' profiles are still viewable (not blocked)
- [ ] On-leave counselor's profile shows amber card with leave message
- [ ] "Book Appointment" button disabled when counselor is on leave
- [ ] "See Referred Counselor" button shown when referralCounselorId is set, navigates to that counselor's profile
- [ ] Toggling off clears on-leave state and restores normal booking
- [ ] All strings in `strings.xml`
- [ ] Javadoc on all new public methods

### US-21 — Anonymous Post-Session Feedback

- [ ] When a COMPLETED appointment exists without feedback, a dismissible feedback card appears on `StudentHomeActivity`
- [ ] Card shows counselor name ("How was your session with Dr. Smith?")
- [ ] Tapping "Give Feedback" opens dialog with 1–5 star RatingBar and optional comment
- [ ] Submitting with 0 stars shows validation error
- [ ] On submit, `feedback/{id}` doc created with `appointmentId`, `rating`, `comment`, `submittedAt` — **no `studentId`**
- [ ] After submission, feedback card disappears and does not reappear for that appointment
- [ ] "Dismiss" button hides the card for the current session
- [ ] `FeedbackService` model has no `studentId` field (verified by unit test)
- [ ] All Firestore ops go through `FeedbackRepository`
- [ ] All strings in `strings.xml`
- [ ] Javadoc on all new public methods

---

## 10. Definition of Done (Sprint 4)

| # | Criterion | How to verify |
|---|---|---|
| 1 | All acceptance criteria pass when manually tested | Demo: counselor toggles leave → student sees badge + referral → student submits feedback |
| 2 | Firestore reads/writes are live | Code review — feedback collection populated, on-leave fields written |
| 3 | Unit tests pass | `./gradlew test` — FeedbackServiceTest |
| 4 | Intent tests pass | `./gradlew connectedAndroidTest` — OnLeaveFlowTest, FeedbackFlowTest |
| 5 | File header comment in every new `.java` file | Code review |
| 6 | Javadoc on all new model/repository public methods | Code review |
| 7 | No hardcoded user-facing strings | All in `strings.xml` |
| 8 | All Firestore calls have `.addOnFailureListener` | Code review |
| 9 | PR linked to GitHub Issue, reviewed by teammate | GitHub |
| 10 | Stories moved to "Done" on Kanban board | Assignee |
| 11 | Repository pattern enforced — no new direct `db.collection()` calls | Code review |
| 12 | **Anonymity verified** — `FeedbackService` has no studentId field at any layer | Code review + unit test |

---

## 11. What Comes Next — Sprint 5 Preview

Per PLAN.md, Sprint 5 (originally Sprint 6) is the **Polish + Deliverables** sprint:

- UML class diagram covering all Phase 3 classes
- Javadoc pass across entire codebase
- Test coverage review (fill gaps)
- Product backlog update in README.md (mark completed stories as Done)
- Kanban board final snapshot
- UI mockup updates to reflect actual implementation
- Demo preparation
- Ensure app builds from clean checkout with no manual steps

All 8 user stories (6 core + 2 secondary) will be implemented by the end of Sprint 4. Sprint 5 focuses entirely on quality, documentation, and deliverable packaging.
