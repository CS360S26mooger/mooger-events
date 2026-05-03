# Sprint 6 — US-12/US-13/US-14/US-15 + US-11 Hardening + US-25 Completion
### Counselor-Side Workflow Sprint

---

## 0. Pre-Sprint Status Report

### Sprint 5 Review — Student Intake and Waitlist Foundation: COMPLETE

Sprint 5 introduced the student-side data needed by the counselor workflow:

| Deliverable | Expected status | Notes |
|---|---|---|
| `IntakeAssessment.java` | Done | Stores quiz answers, urgency, recommended tags, matched counselor. |
| `IntakeAssessmentRepository.java` | Done | Saves and fetches intake assessment documents. |
| `IntakeMatcher.java` | Done | Scores counselors using specialization overlap and on-leave filtering. |
| `WaitlistEntry.java` | Done | Firestore model for `waitlist` entries. |
| `WaitlistRepository.java` | Done | Supports join, duplicate prevention, counselor active count. |
| `QuizActivity.java` | Hardened | Saves real assessment and routes with `ASSESSMENT_ID`. |
| `CounselorProfileActivity.java` | Hardened | Student can join waitlist when no slots or counselor unavailable. |
| `BookingActivity.java` | Hardened | No-slot empty state routes to waitlist. |

### Current Counselor-Side State Before Sprint 6

The current app already has an appointment dashboard and appointment cards:

| File | Current state | Gap Sprint 6 closes |
|---|---|---|
| `CounselorDashboardActivity.java` | Shows appointments in Today / This Week / This Month tabs | Needs waitlist count card, returning-student badge, and routes into student profile/history. |
| `AppointmentAdapter.java` | Shows appointment cards, status badges, No-Show action, placeholder Crisis/Profile/Notes buttons | Replace placeholders with real profile, notes, history, crisis flows. |
| `AppointmentRepository.java` | Fetches appointments, updates status, cancels, feedback-needed query | Add no-show follow-up fields and helper queries for student history. |
| `UserRepository.java` | Fetches user role/name | Add `getStudent(uid)` or general `getUserDocument()` support for profile view. |
| `IntakeAssessmentRepository.java` | From Sprint 5 | Used to show triage/profile before first session. |
| `WaitlistRepository.java` | From Sprint 5 | Used to show waitlist count in dashboard and profile. |

### Stories Covered in This Sprint

| Story | Current status | Sprint 6 target |
|---|---|---|
| **US-12** Quick-insert note templates | Not implemented | Add templates + note editor + Firestore persistence. |
| **US-13** Chronological session history | Student-side history exists only | Counselor can view a student's chronological history. |
| **US-14** View student triage/profile | Intake data exists after Sprint 5 | Counselor can view student profile + latest intake before first session. |
| **US-15** Crisis escalation workflow | Placeholder button only | Real escalation model, dialog, repository, and dashboard logging. |
| **US-11** No-show follow-up | Status update exists | Add follow-up required/status fields and counselor UI. |
| **US-25** Counselor sees waitlist count | Repository foundation exists | Show count on dashboard and availability/profile screens. |

---

## 1. Sprint 6 Objective

By the end of this sprint:

1. **Counselors can open a student profile from an appointment card** and see name, preferred name, pronouns, latest intake/triage answers, urgency level, and reason for seeking help (US-14).
2. **Counselors can view chronological session history for that student**, including completed, cancelled, no-show, and upcoming appointments (US-13).
3. **Counselors can create session notes using quick-insert templates** for common concerns such as anxiety, academic stress, crisis check-in, and follow-up plan (US-12).
4. **No-show marking creates a follow-up workflow flag** instead of only changing appointment status (US-11 hardening).
5. **Crisis escalation is a real workflow** with severity, action taken, optional call intent, and Firestore record (US-15).
6. **Counselor dashboard shows waitlist count** so counselors can gauge demand and open more slots (US-25).

---

## 2. Files to Create or Modify

### 2.1 New Files

```text
src/main/java/com/example/moogerscouncil/
├── StudentProfileActivity.java          // UI — counselor views student details/intake/history entry
├── SessionHistoryActivity.java          // UI — chronological student appointment history
├── SessionNote.java                     // Model — session note document
├── SessionNoteRepository.java           // Repository — sessionNotes collection
├── NoteTemplate.java                    // Constants/helper — quick-insert note templates
├── CrisisEscalation.java                // Model — crisis escalation record
├── CrisisEscalationRepository.java      // Repository — crisisEscalations collection
├── CrisisEscalationDialogFragment.java  // UI — escalation dialog with severity/actions

src/main/res/layout/
├── activity_student_profile.xml         // Student profile + intake + buttons
├── activity_session_history.xml         // History RecyclerView
├── dialog_session_note.xml              // Note editor with template chips
├── dialog_crisis_escalation.xml         // Crisis workflow dialog
├── item_session_history.xml             // History row

src/test/java/com/example/moogerscouncil/
├── SessionNoteTest.java                 // Unit test — template/name fields
├── NoteTemplateTest.java                // Unit test — constants and lookup
├── CrisisEscalationTest.java            // Unit test — severity/status fields

src/androidTest/java/com/example/moogerscouncil/
├── StudentProfileFlowTest.java          // UI test — profile opens from appointment
├── SessionNotesFlowTest.java            // UI test — template insert and save
├── CrisisEscalationFlowTest.java        // UI test — dialog and confirmation
```

### 2.2 Files to Modify

```text
AppointmentAdapter.java              // Wire Profile, Notes, Crisis, No-Show follow-up actions
AppointmentRepository.java           // Add getAppointmentsForStudentForCounselor(), markNoShowWithFollowUp()
CounselorDashboardActivity.java      // Add waitlist count card and refresh count onResume()
UserRepository.java                  // Add getStudentById(uid, callback)
WaitlistRepository.java              // Add count methods if not already complete
IntakeAssessmentRepository.java      // Add getLatestForStudent(studentId, callback) if missing
StudentAppointmentAdapter.java       // Optional cleanup: use repositories consistently
item_appointment.xml                 // Add returning badge / waitlist demand if needed
activity_counselor_dashboard.xml     // Add waitlist count stat card
strings.xml                          // All new user-facing strings
AndroidManifest.xml                  // Register StudentProfileActivity, SessionHistoryActivity
```

---

## 3. Firestore Data Model

### 3.1 Collection: `sessionNotes`

```text
sessionNotes/{noteId}
  ├── id: String
  ├── appointmentId: String
  ├── counselorId: String
  ├── studentId: String
  ├── templateKey: String              // nullable; e.g. "ACADEMIC_STRESS"
  ├── noteText: String
  ├── privateToCounselor: Boolean
  ├── createdAt: Timestamp
  └── updatedAt: Timestamp
```

### 3.2 Collection: `crisisEscalations`

```text
crisisEscalations/{escalationId}
  ├── id: String
  ├── appointmentId: String
  ├── counselorId: String
  ├── studentId: String
  ├── severity: String                 // "MODERATE" | "HIGH" | "IMMEDIATE"
  ├── actionTaken: String              // "CALLED_SECURITY" | "REFERRED_CAPS" | "SAFETY_PLAN" | "OTHER"
  ├── notes: String
  ├── createdAt: Timestamp
  └── resolved: Boolean
```

### 3.3 Existing `appointments` Additions

Add these fields without breaking existing documents:

```text
appointments/{appointmentId}
  ├── noShowFollowUpRequired: Boolean
  ├── noShowFollowUpStatus: String      // "PENDING" | "CONTACTED" | "RESOLVED"
  ├── noShowMarkedAt: Timestamp
  ├── returningStudent: Boolean         // optional cached helper, Sprint 8 can harden
  └── crisisEscalationId: String        // nullable
```

**Backward compatibility:** Existing appointment docs without these fields should still render normally. Always treat missing Boolean values as `false`.

---

## 4. Implementation Details — Model Layer

### 4.1 `SessionNote.java`

```java
package com.example.moogerscouncil;

import com.google.firebase.Timestamp;

/**
 * Represents a counselor's note for one appointment/session.
 * Stored in the Firestore 'sessionNotes' collection.
 */
public class SessionNote {

    private String id;
    private String appointmentId;
    private String counselorId;
    private String studentId;
    private String templateKey;
    private String noteText;
    private boolean privateToCounselor;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    /** Required empty constructor for Firestore. */
    public SessionNote() {}

    public SessionNote(String appointmentId, String counselorId, String studentId,
                       String templateKey, String noteText) {
        this.appointmentId = appointmentId;
        this.counselorId = counselorId;
        this.studentId = studentId;
        this.templateKey = templateKey;
        this.noteText = noteText;
        this.privateToCounselor = true;
        this.createdAt = Timestamp.now();
        this.updatedAt = Timestamp.now();
    }

    public String getId() { return id; }
    public String getAppointmentId() { return appointmentId; }
    public String getCounselorId() { return counselorId; }
    public String getStudentId() { return studentId; }
    public String getTemplateKey() { return templateKey; }
    public String getNoteText() { return noteText; }
    public boolean isPrivateToCounselor() { return privateToCounselor; }
    public Timestamp getCreatedAt() { return createdAt; }
    public Timestamp getUpdatedAt() { return updatedAt; }

    public void setId(String id) { this.id = id; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }
    public void setCounselorId(String counselorId) { this.counselorId = counselorId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public void setTemplateKey(String templateKey) { this.templateKey = templateKey; }
    public void setNoteText(String noteText) { this.noteText = noteText; }
    public void setPrivateToCounselor(boolean privateToCounselor) { this.privateToCounselor = privateToCounselor; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}
```

### 4.2 `NoteTemplate.java`

```java
package com.example.moogerscouncil;

/** Quick-insert session note templates for counselor documentation. */
public final class NoteTemplate {

    public static final String ACADEMIC_STRESS = "ACADEMIC_STRESS";
    public static final String ANXIETY_CHECK_IN = "ANXIETY_CHECK_IN";
    public static final String FOLLOW_UP_PLAN = "FOLLOW_UP_PLAN";
    public static final String CRISIS_CHECK = "CRISIS_CHECK";
    public static final String GENERAL_SESSION = "GENERAL_SESSION";

    public static final String[] ALL_KEYS = {
        ACADEMIC_STRESS, ANXIETY_CHECK_IN, FOLLOW_UP_PLAN,
        CRISIS_CHECK, GENERAL_SESSION
    };

    private NoteTemplate() {}

    public static String getDisplayName(String key) {
        if (ACADEMIC_STRESS.equals(key)) return "Academic Stress";
        if (ANXIETY_CHECK_IN.equals(key)) return "Anxiety Check-in";
        if (FOLLOW_UP_PLAN.equals(key)) return "Follow-up Plan";
        if (CRISIS_CHECK.equals(key)) return "Crisis Check";
        return "General Session";
    }

    public static String getTemplateText(String key) {
        if (ACADEMIC_STRESS.equals(key)) {
            return "Presenting concern: academic stress.\nMain stressors discussed:\nCoping strategies suggested:\nFollow-up tasks:";
        }
        if (ANXIETY_CHECK_IN.equals(key)) {
            return "Anxiety check-in.\nTriggers identified:\nPhysical/emotional symptoms:\nGrounding or breathing practice:\nNext steps:";
        }
        if (FOLLOW_UP_PLAN.equals(key)) {
            return "Follow-up plan.\nProgress since last session:\nBarriers discussed:\nAgreed action items:\nNext appointment recommendation:";
        }
        if (CRISIS_CHECK.equals(key)) {
            return "Crisis/safety check.\nImmediate risk level:\nProtective factors:\nAction taken:\nEmergency contact/referral notes:";
        }
        return "Session summary:\nKey themes:\nInterventions used:\nPlan before next session:";
    }
}
```

### 4.3 `CrisisEscalation.java`

```java
package com.example.moogerscouncil;

import com.google.firebase.Timestamp;

/** Records a counselor-triggered crisis escalation workflow. */
public class CrisisEscalation {

    public static final String SEVERITY_MODERATE = "MODERATE";
    public static final String SEVERITY_HIGH = "HIGH";
    public static final String SEVERITY_IMMEDIATE = "IMMEDIATE";

    public static final String ACTION_CALLED_SECURITY = "CALLED_SECURITY";
    public static final String ACTION_REFERRED_CAPS = "REFERRED_CAPS";
    public static final String ACTION_SAFETY_PLAN = "SAFETY_PLAN";
    public static final String ACTION_OTHER = "OTHER";

    private String id;
    private String appointmentId;
    private String counselorId;
    private String studentId;
    private String severity;
    private String actionTaken;
    private String notes;
    private Timestamp createdAt;
    private boolean resolved;

    /** Required empty constructor for Firestore. */
    public CrisisEscalation() {}

    public CrisisEscalation(String appointmentId, String counselorId, String studentId,
                            String severity, String actionTaken, String notes) {
        this.appointmentId = appointmentId;
        this.counselorId = counselorId;
        this.studentId = studentId;
        this.severity = severity;
        this.actionTaken = actionTaken;
        this.notes = notes;
        this.createdAt = Timestamp.now();
        this.resolved = false;
    }

    public String getId() { return id; }
    public String getAppointmentId() { return appointmentId; }
    public String getCounselorId() { return counselorId; }
    public String getStudentId() { return studentId; }
    public String getSeverity() { return severity; }
    public String getActionTaken() { return actionTaken; }
    public String getNotes() { return notes; }
    public Timestamp getCreatedAt() { return createdAt; }
    public boolean isResolved() { return resolved; }

    public void setId(String id) { this.id = id; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }
    public void setCounselorId(String counselorId) { this.counselorId = counselorId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public void setSeverity(String severity) { this.severity = severity; }
    public void setActionTaken(String actionTaken) { this.actionTaken = actionTaken; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public void setResolved(boolean resolved) { this.resolved = resolved; }
}
```

---

## 5. Implementation Details — Repository Layer

### 5.1 `SessionNoteRepository.java`

```java
package com.example.moogerscouncil;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

/** Repository for all Firestore operations on session notes. */
public class SessionNoteRepository {

    private final CollectionReference notesCollection;

    public SessionNoteRepository() {
        notesCollection = FirebaseFirestore.getInstance().collection("sessionNotes");
    }

    public interface OnNoteActionCallback {
        void onSuccess(String noteId);
        void onFailure(Exception e);
    }

    public interface OnNotesLoadedCallback {
        void onSuccess(List<SessionNote> notes);
        void onFailure(Exception e);
    }

    public void saveNote(SessionNote note, OnNoteActionCallback callback) {
        String id = notesCollection.document().getId();
        note.setId(id);
        notesCollection.document(id)
            .set(note)
            .addOnSuccessListener(unused -> callback.onSuccess(id))
            .addOnFailureListener(callback::onFailure);
    }

    public void getNotesForStudent(String studentId, OnNotesLoadedCallback callback) {
        notesCollection.whereEqualTo("studentId", studentId)
            .get()
            .addOnSuccessListener(snapshot -> {
                List<SessionNote> notes = new ArrayList<>();
                for (int i = 0; i < snapshot.size(); i++) {
                    SessionNote note = snapshot.getDocuments().get(i).toObject(SessionNote.class);
                    if (note != null) {
                        note.setId(snapshot.getDocuments().get(i).getId());
                        notes.add(note);
                    }
                }
                callback.onSuccess(notes);
            })
            .addOnFailureListener(callback::onFailure);
    }
}
```

### 5.2 `CrisisEscalationRepository.java`

```java
package com.example.moogerscouncil;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

/** Repository for counselor-triggered crisis escalation records. */
public class CrisisEscalationRepository {

    private final CollectionReference crisisCollection;
    private final FirebaseFirestore db;

    public CrisisEscalationRepository() {
        db = FirebaseFirestore.getInstance();
        crisisCollection = db.collection("crisisEscalations");
    }

    public interface OnCrisisActionCallback {
        void onSuccess(String escalationId);
        void onFailure(Exception e);
    }

    public void createEscalation(CrisisEscalation escalation,
                                 OnCrisisActionCallback callback) {
        String id = crisisCollection.document().getId();
        escalation.setId(id);
        db.runBatch(batch -> {
            batch.set(crisisCollection.document(id), escalation);
            if (escalation.getAppointmentId() != null) {
                batch.update(db.collection("appointments").document(escalation.getAppointmentId()),
                    "crisisEscalationId", id);
            }
        }).addOnSuccessListener(unused -> callback.onSuccess(id))
          .addOnFailureListener(callback::onFailure);
    }
}
```

### 5.3 `AppointmentRepository.java` Additions

Add a counselor-side student history query:

```java
public void getAppointmentsForStudentHistory(String studentId,
                                             OnAppointmentsLoadedCallback callback) {
    appointmentsCollection
        .whereEqualTo("studentId", studentId)
        .get()
        .addOnSuccessListener(querySnapshot -> {
            List<Appointment> appointments = querySnapshot.toObjects(Appointment.class);
            for (int i = 0; i < appointments.size(); i++) {
                appointments.get(i).setId(querySnapshot.getDocuments().get(i).getId());
            }
            Collections.sort(appointments, (a, b) ->
                String.valueOf(b.getDate()).compareTo(String.valueOf(a.getDate())));
            callback.onSuccess(appointments);
        })
        .addOnFailureListener(callback::onFailure);
}
```

Add no-show follow-up:

```java
public void markNoShowWithFollowUp(String appointmentId,
                                   OnStatusUpdateCallback callback) {
    appointmentsCollection.document(appointmentId)
        .update("status", "NO_SHOW",
                "noShowFollowUpRequired", true,
                "noShowFollowUpStatus", "PENDING",
                "noShowMarkedAt", com.google.firebase.Timestamp.now())
        .addOnSuccessListener(unused -> callback.onSuccess())
        .addOnFailureListener(callback::onFailure);
}
```

### 5.4 `UserRepository.java` Addition

```java
public interface OnStudentFetchedCallback {
    void onSuccess(Student student);
    void onFailure(Exception e);
}

public void getStudentById(String uid, OnStudentFetchedCallback callback) {
    usersCollection.document(uid)
        .get()
        .addOnSuccessListener(doc -> {
            if (!doc.exists()) {
                callback.onFailure(new IllegalStateException("Student not found"));
                return;
            }
            Student student = doc.toObject(Student.class);
            if (student != null) {
                student.setUid(doc.getId());
                callback.onSuccess(student);
            } else {
                callback.onFailure(new IllegalStateException("Could not parse student"));
            }
        })
        .addOnFailureListener(callback::onFailure);
}
```

---

## 6. Implementation Details — UI Layer

### 6.1 `AppointmentAdapter.java`

Replace placeholder actions:

| Button | Current behavior | New behavior |
|---|---|---|
| Profile | Toast placeholder | Opens `StudentProfileActivity` with `STUDENT_ID`, `APPOINTMENT_ID`, `COUNSELOR_ID` |
| Notes | Toast placeholder | Opens note dialog with template chips and saves `SessionNote` |
| Crisis | Toast / simple dialog | Opens `CrisisEscalationDialogFragment` |
| No-Show | Basic status update | Calls `markNoShowWithFollowUp()` and refreshes card |
| Join | Can remain placeholder if video not in scope | Keep toast or hide if not implemented |

Intent extras:

```java
intent.putExtra("STUDENT_ID", appointment.getStudentId());
intent.putExtra("APPOINTMENT_ID", appointment.getId());
intent.putExtra("COUNSELOR_ID", appointment.getCounselorId());
```

### 6.2 `StudentProfileActivity.java`

Display sections:

```text
Student Profile
├── Preferred name / legal name
├── Pronouns
├── Latest intake assessment
│   ├── Primary concern
│   ├── Duration
│   ├── Support type
│   ├── Urgency level
│   └── Recommended tags
├── Buttons
│   ├── View Session History
│   ├── Add Session Note
│   └── Trigger Crisis Escalation
```

Load sequence:

```
onCreate()
  ├── read STUDENT_ID / APPOINTMENT_ID / COUNSELOR_ID extras
  ├── UserRepository.getStudentById(studentId)
  ├── IntakeAssessmentRepository.getLatestForStudent(studentId)
  └── WaitlistRepository / AppointmentRepository optional stats
```

### 6.3 `SessionHistoryActivity.java`

Use `AppointmentRepository.getAppointmentsForStudentHistory(studentId)`.

History card should show:

```text
Date + time
Counselor ID/name if available
Status badge
Optional note indicator: "Notes added"
```

### 6.4 Notes Dialog

`dialog_session_note.xml` should contain:

```text
Title: Add Session Note
ChipGroup: Academic Stress | Anxiety Check-in | Follow-up Plan | Crisis Check | General
TextInputEditText: note text
Buttons: Cancel | Save Note
```

When a chip is selected, append or replace note text with `NoteTemplate.getTemplateText(key)`.

### 6.5 Crisis Escalation Dialog

`dialog_crisis_escalation.xml` should contain:

```text
Title: Crisis Escalation
RadioGroup severity: Moderate | High | Immediate
RadioGroup action: Call Security | Refer CAPS | Safety Plan | Other
TextInputEditText notes
Buttons: Cancel | Save Escalation | Call Campus Security
```

If action is `Call Security`, use `ACTION_DIAL` with the same campus number used by `EmergencyDialogFragment`.

### 6.6 Counselor Dashboard Waitlist Count

Add a stat card:

```text
Waitlist
[count]
Students waiting
```

In `CounselorDashboardActivity.onResume()`:

```java
waitlistRepository.getActiveWaitlistCountForCounselor(currentCounselorId,
    new WaitlistRepository.OnWaitlistCountCallback() {
        @Override
        public void onSuccess(int count) {
            textWaitlistCount.setText(String.valueOf(count));
        }

        @Override
        public void onFailure(Exception e) {
            textWaitlistCount.setText("0");
        }
    });
```

---

## 7. Strings to Add

```xml
<string name="student_profile_title">Student Profile</string>
<string name="latest_intake_title">Latest Intake</string>
<string name="view_session_history">View Session History</string>
<string name="add_session_note">Add Session Note</string>
<string name="trigger_crisis_escalation">Trigger Crisis Escalation</string>
<string name="session_history_title">Session History</string>
<string name="note_saved">Session note saved.</string>
<string name="note_save_error">Could not save note.</string>
<string name="crisis_escalation_title">Crisis Escalation</string>
<string name="crisis_escalation_saved">Crisis escalation recorded.</string>
<string name="crisis_escalation_error">Could not record crisis escalation.</string>
<string name="waitlist_count_title">Waitlist</string>
<string name="students_waiting">Students waiting</string>
<string name="no_show_followup_created">No-show marked. Follow-up required.</string>
<string name="no_intake_found">No intake assessment found for this student.</string>
```

---

## 8. Testing Requirements

### 8.1 Unit Tests

#### `SessionNoteTest.java`

1. Constructor stores appointment, counselor, student, template, text.
2. Default `privateToCounselor` is true.
3. Setters/getters work for Firestore.

#### `NoteTemplateTest.java`

1. All keys return non-empty display names.
2. All keys return non-empty template text.
3. Unknown key falls back to general session.

#### `CrisisEscalationTest.java`

1. Constructor stores appointment/counselor/student.
2. Default `resolved` is false.
3. Severity/action constants match expected strings.

### 8.2 UI Tests

#### `StudentProfileFlowTest.java`

1. Appointment card profile button opens `StudentProfileActivity`.
2. Student profile screen shows profile section.
3. Intake section displays or shows empty state gracefully.

#### `SessionNotesFlowTest.java`

1. Notes button opens dialog.
2. Selecting template fills text field.
3. Save button closes dialog / shows success.

#### `CrisisEscalationFlowTest.java`

1. Crisis button opens escalation dialog.
2. Severity/action options are visible.
3. Save shows success message.

---

## 9. Definition of Done

Sprint 6 is done only when:

- Appointment card Profile, Notes, Crisis, and No-Show buttons are no longer placeholders.
- `StudentProfileActivity` displays student + latest intake data.
- `SessionHistoryActivity` shows chronological appointment history for a student.
- Notes can be created with quick templates and persisted to `sessionNotes`.
- Crisis escalation creates a `crisisEscalations` document.
- No-show action sets `noShowFollowUpRequired = true` and `noShowFollowUpStatus = "PENDING"`.
- Counselor dashboard shows active waitlist count.
- All new models have Javadoc.
- All user-facing strings are in `strings.xml`.
- Unit and UI tests are added.

---

## 10. Deliverables Checklist

| Deliverable | Status |
|---|---|
| `StudentProfileActivity.java` | Todo |
| `SessionHistoryActivity.java` | Todo |
| `SessionNote.java` | Todo |
| `SessionNoteRepository.java` | Todo |
| `NoteTemplate.java` | Todo |
| `CrisisEscalation.java` | Todo |
| `CrisisEscalationRepository.java` | Todo |
| `CrisisEscalationDialogFragment.java` | Todo |
| `AppointmentAdapter.java` action wiring | Todo |
| `AppointmentRepository.java` no-show/history additions | Todo |
| `CounselorDashboardActivity.java` waitlist card | Todo |
| `activity_student_profile.xml` | Todo |
| `activity_session_history.xml` | Todo |
| `dialog_session_note.xml` | Todo |
| `dialog_crisis_escalation.xml` | Todo |
| Unit tests | Todo |
| UI tests | Todo |
