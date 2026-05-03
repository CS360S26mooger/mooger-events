# Sprint 5 — US-04 (Real Intake Matching) + US-24/US-25 Foundation (Waitlist)
### Detailed Implementation Guide

---

## 0. Pre-Sprint Status Report

### Sprint 4.5 Review — Slide-to-Cancel, Quiz Expansion, Homepage Chips: COMPLETE / NEEDS HARDENING

The current codebase already contains the core Phase 3 loop and the refinement work from Sprint 4.5. The repository has the following relevant classes under `src/main/java/com/example/moogerscouncil/`:

| File | Current role | Sprint 5 relevance |
|---|---|---|
| `QuizActivity.java` | Multi-step quiz UI / prototype recommendation | Needs real persistence and scoring. Currently quiz answers are not stored as a reusable intake record. |
| `CounselorListActivity.java` | Searchable/filterable counselor directory | Will receive improved matching filters and waitlist entry points. |
| `CounselorProfileActivity.java` | Counselor profile, on-leave display, book button | Needs "Join Waitlist" entry point when counselor has no available slots or is unavailable. |
| `BookingActivity.java` | Calendar slot selection and booking | Needs no-slot state that routes to waitlist instead of dead end. |
| `AppointmentRepository.java` | Booking, appointment fetches, status updates, cancellation, feedback-needed fetch | Can remain as-is except for optional integration hooks after waitlist actions. |
| `AvailabilityRepository.java` | Slot fetching and management | Used to determine if a counselor is fully booked. |
| `CounselorRepository.java` | Counselor profile/directory reads and profile updates | Used for matching and waitlist display. |
| `StudentHomeActivity.java` | Student dashboard, upcoming session, search/chips, feedback, emergency | Needs latest intake summary and optional waitlist status card. |
| `Student.java` | Firestore user model for students | Can remain as-is; intake data should live in a separate collection. |
| `TimeSlot.java` | Time slot model | No change needed. |
| `SpecializationTags.java` | Canonical specialization strings | Must be reused by intake scoring. Do not create duplicate tag strings. |

### What Sprint 5 Completes

Sprint 4.5 made `QuizActivity` useful as a guided prototype, but the backlog story **US-04** says the system should match a student with the best-fit counselor based on their concerns. That requires a persisted intake record and a repeatable matching algorithm.

Sprint 5 also creates the foundation for **US-24** and **US-25** by adding a `waitlist` collection and repository. The counselor-facing waitlist count UI is completed in Sprint 6, but the data model and student join flow are built here.

---

## 1. Sprint 5 Objective

By the end of this sprint:

1. **A student can complete the triage questionnaire and have their answers saved to Firestore** as an `IntakeAssessment` document linked to their UID (US-04 / US-14 foundation).
2. **The quiz produces a real counselor recommendation** using specialization overlap, language/gender preferences where available, on-leave filtering, and fallback logic (US-04).
3. **A student can join the waitlist for a counselor** when no slots are available or when they explicitly prefer to wait for that counselor (US-24).
4. **The app has a reusable `WaitlistRepository`** so Sprint 6 can show counselor waitlist counts without reworking the data layer (US-25 foundation).

### Why These Stories Together

The quiz, recommendation result, and waitlist are all student-side pre-booking flows:

```
Student need → IntakeAssessment → Counselor match → Counselor profile → Book or Join Waitlist
```

Building these together prevents duplicated Firestore reads. `QuizActivity`, `CounselorProfileActivity`, and `BookingActivity` will all depend on the same new repositories.

---

## 2. What Already Exists vs. What Sprint 5 Builds

| Component | Already Exists | Sprint 5 Builds |
|---|---|---|
| `QuizActivity.java` | Multi-step UI and final recommendation prototype | Stores answers, creates `IntakeAssessment`, runs real scoring, passes `assessmentId` into profile/booking |
| `SpecializationTags.java` | 8 canonical tags | Add helper mapping from quiz answer categories to specialization tags if not already present |
| `CounselorRepository.java` | `getAllCounselors()`, `getCounselor()`, profile update methods | Add `findBestMatch(IntakeAssessment, callback)` or keep matching in `IntakeMatcher` helper |
| `CounselorProfileActivity.java` | Shows profile and book button | Show "Join Waitlist" button when no slots or counselor is on leave; pass `assessmentId` |
| `BookingActivity.java` | Calendar and slot RecyclerView | Empty-slot state with waitlist CTA; create waitlist entry if no slots |
| `StudentHomeActivity.java` | Dashboard, upcoming session, search, feedback | Show latest intake/recommended counselor shortcut and waitlist status card |
| Firestore | `users`, `counselors`, `appointments`, `Slots/{counselorId}/slots`, `feedback` | Add `intakeAssessments` and `waitlist` collections |

**Important path note:** The current `AppointmentRepository` and cancellation logic use nested slots under:

```text
Slots/{counselorId}/slots/{slotId}
```

Do **not** switch to a top-level `slots` collection in this sprint unless the whole codebase is refactored consistently. Use the existing `AvailabilityRepository` and existing slot path conventions.

---

## 3. Files to Create or Modify

### 3.1 New Files

```text
src/main/java/com/example/moogerscouncil/
├── IntakeAssessment.java              // Model — persisted triage answers and matching tags
├── IntakeAssessmentRepository.java    // Repository — intakeAssessments collection
├── IntakeMatcher.java                 // Helper — scores counselors against assessment
├── WaitlistEntry.java                 // Model — waitlist document
├── WaitlistRepository.java            // Repository — waitlist collection

src/test/java/com/example/moogerscouncil/
├── IntakeAssessmentTest.java          // Unit test — model fields, severity, tags
├── IntakeMatcherTest.java             // Unit test — counselor scoring and fallback rules
├── WaitlistEntryTest.java             // Unit test — default status, timestamps, fields

src/androidTest/java/com/example/moogerscouncil/
├── IntakeQuizFlowTest.java            // UI test — quiz saves and routes to result/profile
├── WaitlistFlowTest.java              // UI test — no slot state and join waitlist dialog
```

### 3.2 Files to Modify

```text
QuizActivity.java                  // Save answers, create assessment, run real matching
CounselorProfileActivity.java      // Add Join Waitlist button and assessment-aware routing
BookingActivity.java               // Empty-slot state + waitlist CTA
StudentHomeActivity.java           // Latest intake shortcut + waitlist status card
CounselorRepository.java           // Optional helper fetches for matching
AvailabilityRepository.java        // Add hasAvailableSlots(counselorId, callback)
SpecializationTags.java            // Add safe mapping helpers if needed
activity_quiz.xml                  // Add result details / recommendation reason if missing
activity_counselor_profile.xml     // Add Join Waitlist button and waitlist state text
activity_booking.xml               // Add empty state layout and waitlist CTA button
activity_student_home.xml          // Add optional latest match / waitlist status card
strings.xml                        // All new user-facing strings
AndroidManifest.xml                // No new Activity needed unless result screen split out
```

---

## 4. Firestore Data Model

### 4.1 Collection: `intakeAssessments`

```text
intakeAssessments/{assessmentId}
  ├── id: String
  ├── studentId: String
  ├── primaryConcern: String
  ├── duration: String
  ├── supportType: String
  ├── urgencyLevel: String              // "LOW" | "MEDIUM" | "HIGH" | "CRISIS"
  ├── recommendedSpecializations: List<String>
  ├── matchedCounselorId: String
  ├── matchedCounselorName: String
  ├── createdAt: Timestamp
  └── active: Boolean
```

### 4.2 Collection: `waitlist`

```text
waitlist/{entryId}
  ├── id: String
  ├── studentId: String
  ├── counselorId: String
  ├── assessmentId: String              // nullable but use when available
  ├── reason: String                    // e.g. "No slots available", "Preferred counselor"
  ├── status: String                    // "ACTIVE" | "OFFERED" | "BOOKED" | "CANCELLED" | "EXPIRED"
  ├── requestedAt: Timestamp
  ├── offeredSlotId: String             // nullable, filled in later sprint
  └── offeredAt: Timestamp              // nullable
```

**Anonymity note:** Unlike feedback, waitlist is operational and must include `studentId`. It is not anonymous because the app needs to notify the student when a slot opens.

---

## 5. Implementation Details — Model Layer

### 5.1 `IntakeAssessment.java`

**Purpose:** Firestore-mapped model for the student's triage quiz answers and matching result.

```java
package com.example.moogerscouncil;

import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a student's completed intake / triage questionnaire.
 * Stored in the Firestore 'intakeAssessments' collection and used by
 * IntakeMatcher to recommend counselors.
 */
public class IntakeAssessment {

    public static final String URGENCY_LOW = "LOW";
    public static final String URGENCY_MEDIUM = "MEDIUM";
    public static final String URGENCY_HIGH = "HIGH";
    public static final String URGENCY_CRISIS = "CRISIS";

    private String id;
    private String studentId;
    private String primaryConcern;
    private String duration;
    private String supportType;
    private String urgencyLevel;
    private List<String> recommendedSpecializations;
    private String matchedCounselorId;
    private String matchedCounselorName;
    private Timestamp createdAt;
    private boolean active;

    /** Required empty constructor for Firestore. */
    public IntakeAssessment() {
        recommendedSpecializations = new ArrayList<>();
        urgencyLevel = URGENCY_LOW;
        active = true;
    }

    public IntakeAssessment(String studentId, String primaryConcern,
                            String duration, String supportType,
                            String urgencyLevel, List<String> tags) {
        this.studentId = studentId;
        this.primaryConcern = primaryConcern;
        this.duration = duration;
        this.supportType = supportType;
        this.urgencyLevel = urgencyLevel;
        this.recommendedSpecializations = tags == null ? new ArrayList<>() : tags;
        this.createdAt = Timestamp.now();
        this.active = true;
    }

    public String getId() { return id; }
    public String getStudentId() { return studentId; }
    public String getPrimaryConcern() { return primaryConcern; }
    public String getDuration() { return duration; }
    public String getSupportType() { return supportType; }
    public String getUrgencyLevel() { return urgencyLevel; }
    public List<String> getRecommendedSpecializations() { return recommendedSpecializations; }
    public String getMatchedCounselorId() { return matchedCounselorId; }
    public String getMatchedCounselorName() { return matchedCounselorName; }
    public Timestamp getCreatedAt() { return createdAt; }
    public boolean isActive() { return active; }

    public void setId(String id) { this.id = id; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public void setPrimaryConcern(String primaryConcern) { this.primaryConcern = primaryConcern; }
    public void setDuration(String duration) { this.duration = duration; }
    public void setSupportType(String supportType) { this.supportType = supportType; }
    public void setUrgencyLevel(String urgencyLevel) { this.urgencyLevel = urgencyLevel; }
    public void setRecommendedSpecializations(List<String> tags) { this.recommendedSpecializations = tags; }
    public void setMatchedCounselorId(String matchedCounselorId) { this.matchedCounselorId = matchedCounselorId; }
    public void setMatchedCounselorName(String matchedCounselorName) { this.matchedCounselorName = matchedCounselorName; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public void setActive(boolean active) { this.active = active; }
}
```

### 5.2 `WaitlistEntry.java`

```java
package com.example.moogerscouncil;

import com.google.firebase.Timestamp;

/**
 * Represents one student's place on a counselor's waitlist.
 * Stored in the Firestore 'waitlist' collection.
 */
public class WaitlistEntry {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_OFFERED = "OFFERED";
    public static final String STATUS_BOOKED = "BOOKED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_EXPIRED = "EXPIRED";

    private String id;
    private String studentId;
    private String counselorId;
    private String assessmentId;
    private String reason;
    private String status;
    private Timestamp requestedAt;
    private String offeredSlotId;
    private Timestamp offeredAt;

    /** Required empty constructor for Firestore. */
    public WaitlistEntry() {
        status = STATUS_ACTIVE;
    }

    public WaitlistEntry(String studentId, String counselorId,
                         String assessmentId, String reason) {
        this.studentId = studentId;
        this.counselorId = counselorId;
        this.assessmentId = assessmentId;
        this.reason = reason;
        this.status = STATUS_ACTIVE;
        this.requestedAt = Timestamp.now();
    }

    public String getId() { return id; }
    public String getStudentId() { return studentId; }
    public String getCounselorId() { return counselorId; }
    public String getAssessmentId() { return assessmentId; }
    public String getReason() { return reason; }
    public String getStatus() { return status; }
    public Timestamp getRequestedAt() { return requestedAt; }
    public String getOfferedSlotId() { return offeredSlotId; }
    public Timestamp getOfferedAt() { return offeredAt; }

    public void setId(String id) { this.id = id; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public void setCounselorId(String counselorId) { this.counselorId = counselorId; }
    public void setAssessmentId(String assessmentId) { this.assessmentId = assessmentId; }
    public void setReason(String reason) { this.reason = reason; }
    public void setStatus(String status) { this.status = status; }
    public void setRequestedAt(Timestamp requestedAt) { this.requestedAt = requestedAt; }
    public void setOfferedSlotId(String offeredSlotId) { this.offeredSlotId = offeredSlotId; }
    public void setOfferedAt(Timestamp offeredAt) { this.offeredAt = offeredAt; }
}
```

---

## 6. Implementation Details — Repository Layer

### 6.1 `IntakeAssessmentRepository.java`

**Purpose:** Single point of access for the `intakeAssessments` collection.

```java
package com.example.moogerscouncil;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;

/** Repository for Firestore intake assessment operations. */
public class IntakeAssessmentRepository {

    private final CollectionReference intakeCollection;

    public IntakeAssessmentRepository() {
        intakeCollection = FirebaseFirestore.getInstance().collection("intakeAssessments");
    }

    public interface OnAssessmentSavedCallback {
        void onSuccess(String assessmentId);
        void onFailure(Exception e);
    }

    public interface OnAssessmentLoadedCallback {
        void onSuccess(IntakeAssessment assessment);
        void onFailure(Exception e);
    }

    public interface OnAssessmentsLoadedCallback {
        void onSuccess(List<IntakeAssessment> assessments);
        void onFailure(Exception e);
    }

    public void saveAssessment(IntakeAssessment assessment,
                               OnAssessmentSavedCallback callback) {
        String id = intakeCollection.document().getId();
        assessment.setId(id);
        intakeCollection.document(id)
            .set(assessment)
            .addOnSuccessListener(unused -> callback.onSuccess(id))
            .addOnFailureListener(callback::onFailure);
    }

    public void updateMatchedCounselor(String assessmentId, String counselorId,
                                       String counselorName, OnAssessmentSavedCallback callback) {
        intakeCollection.document(assessmentId)
            .update("matchedCounselorId", counselorId,
                    "matchedCounselorName", counselorName)
            .addOnSuccessListener(unused -> callback.onSuccess(assessmentId))
            .addOnFailureListener(callback::onFailure);
    }

    public void getAssessment(String assessmentId, OnAssessmentLoadedCallback callback) {
        intakeCollection.document(assessmentId)
            .get()
            .addOnSuccessListener(doc -> {
                if (!doc.exists()) {
                    callback.onFailure(new IllegalStateException("Assessment not found"));
                    return;
                }
                IntakeAssessment assessment = doc.toObject(IntakeAssessment.class);
                if (assessment != null) {
                    assessment.setId(doc.getId());
                    callback.onSuccess(assessment);
                } else {
                    callback.onFailure(new IllegalStateException("Could not parse assessment"));
                }
            })
            .addOnFailureListener(callback::onFailure);
    }

    public void getLatestForStudent(String studentId, OnAssessmentLoadedCallback callback) {
        intakeCollection.whereEqualTo("studentId", studentId)
            .whereEqualTo("active", true)
            .get()
            .addOnSuccessListener(snapshot -> {
                IntakeAssessment latest = null;
                for (int i = 0; i < snapshot.size(); i++) {
                    IntakeAssessment item = snapshot.getDocuments().get(i).toObject(IntakeAssessment.class);
                    if (item == null) continue;
                    item.setId(snapshot.getDocuments().get(i).getId());
                    if (latest == null || String.valueOf(item.getCreatedAt())
                            .compareTo(String.valueOf(latest.getCreatedAt())) > 0) {
                        latest = item;
                    }
                }
                if (latest == null) {
                    callback.onFailure(new IllegalStateException("No intake assessment found"));
                } else {
                    callback.onSuccess(latest);
                }
            })
            .addOnFailureListener(callback::onFailure);
    }
}
```

### 6.2 `WaitlistRepository.java`

```java
package com.example.moogerscouncil;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

/** Repository for Firestore waitlist operations. */
public class WaitlistRepository {

    private final CollectionReference waitlistCollection;

    public WaitlistRepository() {
        waitlistCollection = FirebaseFirestore.getInstance().collection("waitlist");
    }

    public interface OnWaitlistActionCallback {
        void onSuccess();
        void onAlreadyWaitlisted();
        void onFailure(Exception e);
    }

    public interface OnWaitlistLoadedCallback {
        void onSuccess(List<WaitlistEntry> entries);
        void onFailure(Exception e);
    }

    public interface OnWaitlistCountCallback {
        void onSuccess(int count);
        void onFailure(Exception e);
    }

    public void joinWaitlist(WaitlistEntry entry, OnWaitlistActionCallback callback) {
        waitlistCollection
            .whereEqualTo("studentId", entry.getStudentId())
            .whereEqualTo("counselorId", entry.getCounselorId())
            .whereEqualTo("status", WaitlistEntry.STATUS_ACTIVE)
            .get()
            .addOnSuccessListener(snapshot -> {
                if (!snapshot.isEmpty()) {
                    callback.onAlreadyWaitlisted();
                    return;
                }
                String id = waitlistCollection.document().getId();
                entry.setId(id);
                waitlistCollection.document(id)
                    .set(entry)
                    .addOnSuccessListener(unused -> callback.onSuccess())
                    .addOnFailureListener(callback::onFailure);
            })
            .addOnFailureListener(callback::onFailure);
    }

    public void getActiveWaitlistForCounselor(String counselorId,
                                              OnWaitlistLoadedCallback callback) {
        waitlistCollection
            .whereEqualTo("counselorId", counselorId)
            .whereEqualTo("status", WaitlistEntry.STATUS_ACTIVE)
            .get()
            .addOnSuccessListener(snapshot -> {
                List<WaitlistEntry> entries = new ArrayList<>();
                for (int i = 0; i < snapshot.size(); i++) {
                    WaitlistEntry entry = snapshot.getDocuments().get(i).toObject(WaitlistEntry.class);
                    if (entry != null) {
                        entry.setId(snapshot.getDocuments().get(i).getId());
                        entries.add(entry);
                    }
                }
                callback.onSuccess(entries);
            })
            .addOnFailureListener(callback::onFailure);
    }

    public void getActiveWaitlistCountForCounselor(String counselorId,
                                                   OnWaitlistCountCallback callback) {
        getActiveWaitlistForCounselor(counselorId, new OnWaitlistLoadedCallback() {
            @Override
            public void onSuccess(List<WaitlistEntry> entries) {
                callback.onSuccess(entries.size());
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }
}
```

---

## 7. Implementation Details — Matching Logic

### 7.1 `IntakeMatcher.java`

**Purpose:** Keep scoring outside `QuizActivity` so it can be unit tested.

```java
package com.example.moogerscouncil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Helper class that scores counselors against a student's intake assessment. */
public final class IntakeMatcher {

    private IntakeMatcher() {}

    public static List<String> tagsForAnswers(String primaryConcern,
                                              String supportType,
                                              String urgencyLevel) {
        List<String> tags = new ArrayList<>();

        if (primaryConcern == null) primaryConcern = "";
        if (supportType == null) supportType = "";
        if (urgencyLevel == null) urgencyLevel = IntakeAssessment.URGENCY_LOW;

        String concern = primaryConcern.toLowerCase();
        String support = supportType.toLowerCase();

        if (concern.contains("academic")) {
            tags.add(SpecializationTags.ACADEMIC_STRESS);
            tags.add(SpecializationTags.CAREER_GUIDANCE);
        } else if (concern.contains("career")) {
            tags.add(SpecializationTags.CAREER_GUIDANCE);
            tags.add(SpecializationTags.ACADEMIC_STRESS);
        } else if (concern.contains("relationship")) {
            tags.add(SpecializationTags.RELATIONSHIPS);
            tags.add(SpecializationTags.FAMILY_ISSUES);
        } else if (concern.contains("emotional") || concern.contains("personal")) {
            tags.add(SpecializationTags.ANXIETY);
            tags.add(SpecializationTags.DEPRESSION);
        } else {
            tags.add(SpecializationTags.ANXIETY);
            tags.add(SpecializationTags.ACADEMIC_STRESS);
        }

        if (support.contains("crisis") || IntakeAssessment.URGENCY_CRISIS.equals(urgencyLevel)) {
            if (!tags.contains(SpecializationTags.TRAUMA)) tags.add(SpecializationTags.TRAUMA);
        }

        return tags;
    }

    public static int scoreCounselor(Counselor counselor, IntakeAssessment assessment) {
        if (counselor == null || assessment == null) return -1;
        if (Boolean.TRUE.equals(counselor.getOnLeave())) return -1;

        int score = 0;
        List<String> needed = assessment.getRecommendedSpecializations();
        List<String> offered = counselor.getSpecializations();

        if (needed != null && offered != null) {
            for (String tag : needed) {
                if (offered.contains(tag)) score += 10;
            }
        }

        if (counselor.getBio() != null && assessment.getPrimaryConcern() != null) {
            String bio = counselor.getBio().toLowerCase();
            String concern = assessment.getPrimaryConcern().toLowerCase();
            if (bio.contains(concern)) score += 2;
        }

        return score;
    }

    public static Counselor findBestCounselor(List<Counselor> counselors,
                                              IntakeAssessment assessment) {
        Counselor best = null;
        int bestScore = -1;

        for (Counselor counselor : counselors) {
            int score = scoreCounselor(counselor, assessment);
            if (score > bestScore) {
                bestScore = score;
                best = counselor;
            }
        }

        if (best == null && counselors != null && !counselors.isEmpty()) {
            for (Counselor counselor : counselors) {
                if (!Boolean.TRUE.equals(counselor.getOnLeave())) {
                    return counselor;
                }
            }
        }
        return best;
    }
}
```

---

## 8. Implementation Details — UI Layer

### 8.1 `QuizActivity.java`

**Changes required:**

1. Keep existing question UI and answer array.
2. When the final question is answered, create an `IntakeAssessment`.
3. Use `IntakeMatcher.tagsForAnswers()` to create recommended tags.
4. Save assessment using `IntakeAssessmentRepository.saveAssessment()`.
5. Load all counselors using `CounselorRepository.getAllCounselors()`.
6. Use `IntakeMatcher.findBestCounselor()`.
7. Save matched counselor ID/name back to the assessment.
8. Show result screen and route to `CounselorProfileActivity` with extras.

**Intent extras to standardize:**

```java
public static final String EXTRA_ASSESSMENT_ID = "ASSESSMENT_ID";
public static final String EXTRA_MATCHED_COUNSELOR_ID = "COUNSELOR_ID";
public static final String EXTRA_MATCH_REASON = "MATCH_REASON";
```

**Final routing:**

```java
Intent intent = new Intent(QuizActivity.this, CounselorProfileActivity.class);
intent.putExtra("COUNSELOR_ID", matchedCounselor.getId());
intent.putExtra("ASSESSMENT_ID", assessmentId);
intent.putExtra("MATCH_REASON", buildMatchReason(assessment));
startActivity(intent);
```

### 8.2 `CounselorProfileActivity.java`

Add a secondary button under the existing booking button:

```text
[Book Appointment]
[Join Waitlist]
```

Rules:

| Condition | Book button | Waitlist button |
|---|---|---|
| Counselor is not on leave and has slots | Enabled | Optional / visible as secondary |
| Counselor is not on leave and has no slots | Disabled with text "No slots available" | Visible and enabled |
| Counselor is on leave with referral | Disabled | Visible as "Join Waitlist" and referral button remains visible |
| Counselor is on leave without referral | Disabled | Visible and enabled |

Use `AvailabilityRepository.hasAvailableSlots(counselorId, callback)` to decide button state.

### 8.3 `BookingActivity.java`

If the selected counselor has no available slots:

```text
No slots available right now.
You can join this counselor's waitlist and we'll offer you the next available slot.
[Join Waitlist]
```

The button should call the same helper method used by `CounselorProfileActivity`.

### 8.4 Shared Waitlist Helper Pattern

Both `CounselorProfileActivity` and `BookingActivity` should use equivalent logic:

```java
private void joinWaitlist(String counselorId, String assessmentId, String reason) {
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    if (user == null) {
        Toast.makeText(this, R.string.error_login_required, Toast.LENGTH_SHORT).show();
        return;
    }

    WaitlistEntry entry = new WaitlistEntry(user.getUid(), counselorId, assessmentId, reason);
    waitlistRepository.joinWaitlist(entry, new WaitlistRepository.OnWaitlistActionCallback() {
        @Override
        public void onSuccess() {
            Toast.makeText(CurrentActivity.this, R.string.waitlist_joined, Toast.LENGTH_LONG).show();
        }

        @Override
        public void onAlreadyWaitlisted() {
            Toast.makeText(CurrentActivity.this, R.string.waitlist_already_joined, Toast.LENGTH_LONG).show();
        }

        @Override
        public void onFailure(Exception e) {
            Toast.makeText(CurrentActivity.this, R.string.waitlist_error, Toast.LENGTH_LONG).show();
        }
    });
}
```

---

## 9. Strings to Add

```xml
<string name="quiz_saving_assessment">Saving your assessment...</string>
<string name="quiz_matching_counselor">Finding your best match...</string>
<string name="quiz_match_title">We found a counselor for you</string>
<string name="quiz_match_reason">Matched based on your concerns and preferred support type.</string>
<string name="assessment_error_save">Could not save your assessment. Please try again.</string>
<string name="no_slots_available">No slots available right now</string>
<string name="join_waitlist">Join Waitlist</string>
<string name="waitlist_joined">You have joined the waitlist.</string>
<string name="waitlist_already_joined">You are already on this counselor's waitlist.</string>
<string name="waitlist_error">Could not join waitlist. Please try again.</string>
<string name="waitlist_status_title">Your Waitlist</string>
<string name="latest_match_title">Your Latest Match</string>
```

---

## 10. Testing Requirements

### 10.1 Unit Tests

#### `IntakeAssessmentTest.java`

Test cases:

1. Empty constructor initializes `recommendedSpecializations` and active state.
2. Constructor stores student ID and answers correctly.
3. Urgency constants equal expected strings.
4. Matched counselor fields can be set and retrieved.

#### `IntakeMatcherTest.java`

Test cases:

1. Academic concern maps to `Academic Stress` and `Career Guidance`.
2. Relationship concern maps to `Relationships`.
3. Crisis support adds `Trauma`.
4. Counselor with overlapping tags scores higher.
5. On-leave counselor returns score `-1`.
6. `findBestCounselor()` skips on-leave counselors.

#### `WaitlistEntryTest.java`

Test cases:

1. Empty constructor defaults status to `ACTIVE`.
2. Constructor sets `studentId`, `counselorId`, `assessmentId`, reason.
3. Status can move to `OFFERED`, `BOOKED`, `CANCELLED`.

### 10.2 Intent / UI Tests

#### `IntakeQuizFlowTest.java`

Test cases:

1. Quiz questions display in order.
2. Selecting answers reaches result screen.
3. Result screen has `View Profile` button.
4. Browse fallback routes to `CounselorListActivity`.

#### `WaitlistFlowTest.java`

Test cases:

1. Counselor profile displays Join Waitlist button when no slots exist.
2. Tapping Join Waitlist shows confirmation/toast.
3. Rejoining same counselor shows already-waitlisted message.

---

## 11. Definition of Done

Sprint 5 is done only when:

- `QuizActivity` creates a real `IntakeAssessment` document.
- `IntakeMatcher` uses `SpecializationTags` and skips on-leave counselors.
- Matched counselor ID/name are saved back onto the assessment.
- `CounselorProfileActivity` and `BookingActivity` both support joining waitlist.
- Duplicate active waitlist entries are prevented.
- `WaitlistRepository.getActiveWaitlistCountForCounselor()` exists for Sprint 6.
- All new models have Javadoc.
- Unit tests and intent tests are added.
- All user-visible strings are in `strings.xml`.
- The app still supports the original core loop: register → browse → book → dashboard.

---

## 12. Deliverables Checklist

| Deliverable | Status |
|---|---|
| `IntakeAssessment.java` | Todo |
| `IntakeAssessmentRepository.java` | Todo |
| `IntakeMatcher.java` | Todo |
| `WaitlistEntry.java` | Todo |
| `WaitlistRepository.java` | Todo |
| `QuizActivity.java` persistence + matching | Todo |
| `CounselorProfileActivity.java` waitlist CTA | Todo |
| `BookingActivity.java` no-slot waitlist state | Todo |
| `StudentHomeActivity.java` latest match / waitlist status | Todo |
| `strings.xml` additions | Todo |
| `IntakeAssessmentTest.java` | Todo |
| `IntakeMatcherTest.java` | Todo |
| `WaitlistEntryTest.java` | Todo |
| `IntakeQuizFlowTest.java` | Todo |
| `WaitlistFlowTest.java` | Todo |
