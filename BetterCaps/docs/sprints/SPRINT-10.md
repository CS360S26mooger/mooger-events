# Sprint 10 — Admin Reminders, Secure Pre-Session Messaging, and Returning Student Indicator
### Detailed Implementation Guide

---

## 0. Pre-Sprint Status Report

### Current Codebase Review — Student and Counselor Foundation: MOSTLY COMPLETE

The current `main` branch already contains the student, counselor, booking, waitlist, intake, notes, crisis, buffer-time, and calendar-export foundation. Sprint 10 must **not rebuild those flows**. It should only add the missing admin/system layer and finish the remaining partially implemented counselor indicator.

#### Already Present in the Current Codebase

| Area | Existing Files | Current State |
|---|---|---|
| Authentication and role routing | `LoginActivity.java`, `RegisterActivity.java`, `UserRepository.java`, `UserRole.java`, `PrivacyPolicyActivity.java` | Student/counselor auth exists. Admin role constant exists, but admin routing is not properly implemented because there is no admin destination yet. |
| Student home and privacy | `StudentHomeActivity.java`, `SessionCache.java`, `EmergencyDialogFragment.java` | Student home, discreet mode, emergency dialog, feedback, upcoming session, waitlist entry points, and cache support exist. |
| Counselor directory/profile | `CounselorListActivity.java`, `CounselorProfileActivity.java`, `CounselorProfileEditActivity.java`, `CounselorRepository.java`, `SpecializationTags.java` | Search/filter/profile/edit/on-leave/specialization flows exist. |
| Booking and slots | `BookingActivity.java`, `BookingConfirmationFragment.java`, `AppointmentRepository.java`, `AvailabilityRepository.java`, `AvailabilitySchedule.java`, `TimeSlot.java`, `TimeSlotAdapter.java` | Booking and availability foundation exists. The repo uses the nested slot path `Slots/{counselorId}/slots/{slotId}`. Do not replace this with a top-level `slots` collection. |
| Buffer time and calendar export | `AvailabilitySettings.java`, `AvailabilitySettingsActivity.java`, `AvailabilitySettingsRepository.java`, `BufferTimeValidator.java`, `CalendarSyncHelper.java` | Buffer-time and simplified calendar export/sync support exists. |
| Intake and matching | `IntakeAssessment.java`, `IntakeAssessmentRepository.java`, `IntakeMatcher.java`, `QuizActivity.java` | Intake assessment and matching infrastructure exists. |
| Waitlist | `WaitlistEntry.java`, `WaitlistOffer.java`, `WaitlistRepository.java`, `WaitlistAdapter.java`, `WaitlistMatcher.java`, `WaitlistRequestActivity.java`, `StudentWaitlistActivity.java`, `CounselorWaitlistActivity.java`, `CounselorWaitlistAdapter.java` | Waitlist flow exists or is in progress. Do not rewrite it unless Sprint 10 integration needs a small read-only hook. |
| Counselor workflow | `CounselorDashboardActivity.java`, `AppointmentAdapter.java`, `StudentProfileActivity.java`, `SessionHistoryActivity.java`, `SessionNote.java`, `SessionNoteActivity.java`, `SessionNoteRepository.java`, `NoteTemplate.java`, `CrisisEscalation.java`, `CrisisEscalationDialogFragment.java`, `CrisisEscalationRepository.java` | Notes, session history, student profile, no-show/crisis flows mostly exist. Returning-student indicator is still partial. |
| Feedback | `FeedbackService.java`, `FeedbackRepository.java`, `dialog_feedback.xml` | Anonymous feedback exists. |

---

### Remaining Documentation Scope

The product backlog still leaves three important stories incomplete or partial:

| Story | Current Status | Sprint 10 Responsibility |
|---|---|---|
| **US-16 — Admin automated reminders** | Todo | Build the admin side from scratch: admin dashboard, reminder settings screen, reminder model/repository, and a prototype-safe reminder scheduler/helper. |
| **US-17 — Returning-student indicator** | Partial | Add a visible "Returning Student" badge on counselor appointment cards when the student has a previous completed/confirmed appointment history. |
| **US-18 — Secure pre-session messaging** | Todo | Add appointment-linked in-app messaging between counselor and student. Counselor sends pre-session message; student can read it before the appointment. |

### Important Interpretation

This sprint is a **prototype-safe implementation**, not a production backend.

- US-16 reminders should be implemented as admin-configurable reminder settings plus an in-app reminder record/scheduler helper. Do **not** add Firebase Cloud Functions, OAuth, FCM, WorkManager, or background server infrastructure unless already available and tested.
- US-18 "secure" means app-internal, appointment-linked, Firestore-backed messaging with repository access and no public/external sharing. Do **not** attempt end-to-end encryption.
- US-17 should be lightweight and reliable: query previous appointments, compute whether the student is returning, and show/hide a badge.

---

## 1. Sprint 10 Objective

By the end of Sprint 10, the app delivers:

1. **Admin dashboard and reminder settings** — an admin can log in, reach `AdminDashboardActivity`, open `ReminderSettingsActivity`, and configure 24-hour and 1-hour reminder messages/settings (US-16).
2. **Prototype-safe reminder infrastructure** — reminder settings are saved to Firestore, and a helper/repository can compute reminder candidates from upcoming appointments without breaking the existing appointment flow (US-16).
3. **Returning-student indicator** — counselor appointment cards show a visible "Returning Student" badge when the student has prior appointment history (US-17).
4. **Secure pre-session messaging** — counselors can send appointment-linked messages to students, and students can view messages related to their appointments (US-18).
5. **Admin role routing** — `LoginActivity` routes `UserRole.ADMIN` to `AdminDashboardActivity` instead of sending admins to `StudentHomeActivity`.
6. **Final integration polish** — all new activities are registered, all strings are in `strings.xml`, and existing flows continue to build and run.

### Why These Stories Together

These are the final system-level stories:
- US-16 is purely admin/system functionality.
- US-18 connects counselor and student before sessions.
- US-17 uses existing appointment history to improve counselor context.
- All three are low-risk if implemented through repositories and carefully integrated into `AppointmentAdapter`, `StudentHomeActivity`, and `LoginActivity`.

---

## 2. Files to Create or Modify

### 2.1 New Files

```
src/main/java/com/example/moogerscouncil/
├── AdminDashboardActivity.java          // UI — admin landing page
├── ReminderSettingsActivity.java        // UI — admin edits reminder settings
├── ReminderSettings.java                // Model — admin-configured reminder settings
├── ReminderRepository.java              // Repository — reminderSettings + reminderRecords ops
├── ReminderScheduler.java               // Helper — prototype-safe reminder candidate computation
├── SecureMessage.java                   // Model — appointment-linked in-app message
├── SecureMessageRepository.java         // Repository — secureMessages collection ops
├── MessageThreadActivity.java           // UI — appointment message thread
├── SecureMessageAdapter.java            // Adapter — renders messages in MessageThreadActivity
├── ReturningStudentHelper.java          // Helper — computes returning-student badge status

src/main/res/layout/
├── activity_admin_dashboard.xml         // Admin landing page
├── activity_reminder_settings.xml       // Reminder configuration form
├── activity_message_thread.xml          // Message thread UI
├── item_secure_message.xml              // Single message bubble/card

src/test/java/com/example/moogerscouncil/
├── ReminderSettingsTest.java            // Unit test — defaults, setters
├── SecureMessageTest.java               // Unit test — message model integrity
├── ReturningStudentHelperTest.java      // Unit test — returning student logic

src/androidTest/java/com/example/moogerscouncil/
├── AdminDashboardTest.java              // UI test — admin dashboard core views
├── ReminderSettingsFlowTest.java        // UI test — settings screen opens/saves
├── MessageThreadFlowTest.java           // UI test — message screen displays input/list
```

### 2.2 Files to Modify

```
LoginActivity.java                 // Route UserRole.ADMIN to AdminDashboardActivity
UserRepository.java                // Add lightweight helper if admin display name is needed
AppointmentRepository.java         // Add methods for previous-history and upcoming-reminder queries
AppointmentAdapter.java            // Add message action + returning-student badge
CounselorDashboardActivity.java    // Pass callbacks/dependencies to AppointmentAdapter if needed
StudentHomeActivity.java           // Add student-side entry point/indicator for pre-session messages if safe
StudentAppointmentAdapter.java     // Optional: add "Messages" action for student appointment cards
CalendarActivity.java              // Optional: expose appointment messages from student calendar cards if safe
item_appointment.xml               // Add returning-student badge and/or message action if not already present
item_student_appointment.xml       // Optional: add "View Messages" button
activity_student_home.xml          // Optional: message indicator/card if simple
AndroidManifest.xml                // Register AdminDashboardActivity, ReminderSettingsActivity, MessageThreadActivity
strings.xml                        // Add all new user-facing strings
```

### 2.3 Files Not to Rewrite

Do **not** rewrite these flows:
```
BookingActivity.java
AvailabilityRepository.java
AvailabilitySettingsActivity.java
WaitlistRepository.java
WaitlistRequestActivity.java
StudentWaitlistActivity.java
CounselorWaitlistActivity.java
IntakeAssessmentRepository.java
QuizActivity.java
FeedbackRepository.java
EmergencyDialogFragment.java
CrisisEscalationRepository.java
```

Only touch them if there is a small, unavoidable integration issue.

---

## 3. Firestore Data Model

### 3.1 Collection: `reminderSettings`

Use one default document for global settings.

```
reminderSettings/default
  ├── enabled24Hour: Boolean
  ├── enabled1Hour: Boolean
  ├── message24Hour: String
  ├── message1Hour: String
  ├── updatedBy: String        // admin uid
  └── updatedAt: Timestamp
```

### 3.2 Collection: `reminderRecords`

Prototype-safe reminder records. These allow the app to avoid duplicate reminders and give admins visibility later if needed.

```
reminderRecords/{recordId}
  ├── appointmentId: String
  ├── studentId: String
  ├── counselorId: String
  ├── reminderType: String     // "24_HOUR" | "1_HOUR"
  ├── messageText: String
  ├── scheduledFor: Timestamp
  ├── createdAt: Timestamp
  └── delivered: Boolean       // false by default in prototype
```

### 3.3 Collection: `secureMessages`

Appointment-linked in-app messages.

```
secureMessages/{messageId}
  ├── appointmentId: String
  ├── counselorId: String
  ├── studentId: String
  ├── senderId: String
  ├── receiverId: String
  ├── senderRole: String       // "counselor" | "student"
  ├── messageText: String
  ├── createdAt: Timestamp
  └── read: Boolean
```

### 3.4 Appointment Fields — Optional

Do not require migration. Only add these fields opportunistically if helpful:

```
appointments/{appointmentId}
  ├── hasPreSessionMessage: Boolean
  └── lastMessageAt: Timestamp
```

The message thread must work even if these fields are absent.

---

## 4. Implementation Details — Model Layer

### 4.1 `ReminderSettings.java`

**Purpose:** Firestore-mapped data container for admin reminder configuration.

```java
package com.example.moogerscouncil;

import com.google.firebase.Timestamp;

/**
 * Represents the global reminder configuration controlled by an admin.
 * Stored at reminderSettings/default in Firestore.
 *
 * This class is a Firestore model and must keep an empty constructor.
 */
public class ReminderSettings {

    private boolean enabled24Hour;
    private boolean enabled1Hour;
    private String message24Hour;
    private String message1Hour;
    private String updatedBy;
    private Timestamp updatedAt;

    /** Required empty constructor for Firestore. */
    public ReminderSettings() {}

    /**
     * Creates reminder settings with all fields.
     *
     * @param enabled24Hour Whether 24-hour reminders are enabled.
     * @param enabled1Hour Whether 1-hour reminders are enabled.
     * @param message24Hour Message used for 24-hour reminders.
     * @param message1Hour Message used for 1-hour reminders.
     * @param updatedBy UID of the admin who last changed the settings.
     */
    public ReminderSettings(boolean enabled24Hour, boolean enabled1Hour,
                            String message24Hour, String message1Hour,
                            String updatedBy) {
        this.enabled24Hour = enabled24Hour;
        this.enabled1Hour = enabled1Hour;
        this.message24Hour = message24Hour;
        this.message1Hour = message1Hour;
        this.updatedBy = updatedBy;
        this.updatedAt = Timestamp.now();
    }

    /**
     * Returns default settings used when Firestore has no document yet.
     *
     * @return Default enabled reminder configuration.
     */
    public static ReminderSettings defaultSettings() {
        return new ReminderSettings(
            true,
            true,
            "Reminder: your counseling session is tomorrow. Please check BetterCaps for details.",
            "Reminder: your counseling session starts in about one hour.",
            ""
        );
    }

    public boolean isEnabled24Hour() { return enabled24Hour; }
    public boolean isEnabled1Hour() { return enabled1Hour; }
    public String getMessage24Hour() { return message24Hour; }
    public String getMessage1Hour() { return message1Hour; }
    public String getUpdatedBy() { return updatedBy; }
    public Timestamp getUpdatedAt() { return updatedAt; }

    public void setEnabled24Hour(boolean enabled24Hour) { this.enabled24Hour = enabled24Hour; }
    public void setEnabled1Hour(boolean enabled1Hour) { this.enabled1Hour = enabled1Hour; }
    public void setMessage24Hour(String message24Hour) { this.message24Hour = message24Hour; }
    public void setMessage1Hour(String message1Hour) { this.message1Hour = message1Hour; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}
```

### 4.2 `SecureMessage.java`

**Purpose:** Firestore-mapped model for one appointment-linked message.

```java
package com.example.moogerscouncil;

import com.google.firebase.Timestamp;

/**
 * Represents a secure in-app message linked to one appointment.
 * Used for counselor-to-student pre-session communication.
 */
public class SecureMessage {

    private String id;
    private String appointmentId;
    private String counselorId;
    private String studentId;
    private String senderId;
    private String receiverId;
    private String senderRole;
    private String messageText;
    private Timestamp createdAt;
    private boolean read;

    /** Required empty constructor for Firestore. */
    public SecureMessage() {}

    /**
     * Creates a secure appointment-linked message.
     *
     * @param appointmentId Appointment this message belongs to.
     * @param counselorId Counselor involved in the appointment.
     * @param studentId Student involved in the appointment.
     * @param senderId UID of the sender.
     * @param receiverId UID of the receiver.
     * @param senderRole Role of sender: "counselor" or "student".
     * @param messageText Message body.
     */
    public SecureMessage(String appointmentId, String counselorId, String studentId,
                         String senderId, String receiverId, String senderRole,
                         String messageText) {
        this.appointmentId = appointmentId;
        this.counselorId = counselorId;
        this.studentId = studentId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.senderRole = senderRole;
        this.messageText = messageText;
        this.createdAt = Timestamp.now();
        this.read = false;
    }

    public String getId() { return id; }
    public String getAppointmentId() { return appointmentId; }
    public String getCounselorId() { return counselorId; }
    public String getStudentId() { return studentId; }
    public String getSenderId() { return senderId; }
    public String getReceiverId() { return receiverId; }
    public String getSenderRole() { return senderRole; }
    public String getMessageText() { return messageText; }
    public Timestamp getCreatedAt() { return createdAt; }
    public boolean isRead() { return read; }

    public void setId(String id) { this.id = id; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }
    public void setCounselorId(String counselorId) { this.counselorId = counselorId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }
    public void setSenderRole(String senderRole) { this.senderRole = senderRole; }
    public void setMessageText(String messageText) { this.messageText = messageText; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public void setRead(boolean read) { this.read = read; }
}
```

---

## 5. Implementation Details — Repository Layer

### 5.1 `ReminderRepository.java`

**Purpose:** Single access point for reminder settings and reminder records.

```java
package com.example.moogerscouncil;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

/**
 * Repository for admin reminder configuration and reminder records.
 * Activities must use this class instead of touching Firestore directly.
 */
public class ReminderRepository {

    private static final String SETTINGS_DOC_ID = "default";

    private final CollectionReference settingsCollection;
    private final CollectionReference recordsCollection;

    public ReminderRepository() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        this.settingsCollection = db.collection("reminderSettings");
        this.recordsCollection = db.collection("reminderRecords");
    }

    public interface OnSettingsLoadedCallback {
        void onSuccess(ReminderSettings settings);
        void onFailure(Exception e);
    }

    public interface OnReminderActionCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    /**
     * Loads global reminder settings. If no document exists, returns defaults.
     *
     * @param callback Receives ReminderSettings on success.
     */
    public void getSettings(OnSettingsLoadedCallback callback) {
        settingsCollection.document(SETTINGS_DOC_ID)
            .get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    ReminderSettings settings = doc.toObject(ReminderSettings.class);
                    callback.onSuccess(settings);
                } else {
                    callback.onSuccess(ReminderSettings.defaultSettings());
                }
            })
            .addOnFailureListener(callback::onFailure);
    }

    /**
     * Saves global reminder settings to reminderSettings/default.
     *
     * @param settings Settings to persist.
     * @param callback Success/failure callback.
     */
    public void saveSettings(ReminderSettings settings, OnReminderActionCallback callback) {
        settings.setUpdatedAt(Timestamp.now());
        settingsCollection.document(SETTINGS_DOC_ID)
            .set(settings)
            .addOnSuccessListener(unused -> callback.onSuccess())
            .addOnFailureListener(callback::onFailure);
    }

    /**
     * Creates one reminder record for a specific appointment.
     *
     * @param appointment Appointment to remind about.
     * @param reminderType "24_HOUR" or "1_HOUR".
     * @param messageText Reminder message.
     * @param scheduledFor Timestamp when reminder should be sent.
     * @param callback Success/failure callback.
     */
    public void createReminderRecord(Appointment appointment, String reminderType,
                                     String messageText, Timestamp scheduledFor,
                                     OnReminderActionCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("appointmentId", appointment.getId());
        data.put("studentId", appointment.getStudentId());
        data.put("counselorId", appointment.getCounselorId());
        data.put("reminderType", reminderType);
        data.put("messageText", messageText);
        data.put("scheduledFor", scheduledFor);
        data.put("createdAt", Timestamp.now());
        data.put("delivered", false);

        recordsCollection.add(data)
            .addOnSuccessListener(unused -> callback.onSuccess())
            .addOnFailureListener(callback::onFailure);
    }
}
```

### 5.2 `SecureMessageRepository.java`

**Purpose:** Central Firestore access for `secureMessages`.

```java
package com.example.moogerscouncil;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.List;

/**
 * Repository for secure appointment-linked messages.
 * Used by MessageThreadActivity and appointment cards.
 */
public class SecureMessageRepository {

    private final CollectionReference messagesCollection;

    public SecureMessageRepository() {
        this.messagesCollection = FirebaseFirestore.getInstance().collection("secureMessages");
    }

    public interface OnMessagesLoadedCallback {
        void onSuccess(List<SecureMessage> messages);
        void onFailure(Exception e);
    }

    public interface OnMessageActionCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    /**
     * Loads all messages for one appointment in chronological order.
     *
     * @param appointmentId Appointment ID.
     * @param callback Receives list of messages.
     */
    public void getMessagesForAppointment(String appointmentId,
                                          OnMessagesLoadedCallback callback) {
        messagesCollection
            .whereEqualTo("appointmentId", appointmentId)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<SecureMessage> messages = querySnapshot.toObjects(SecureMessage.class);
                for (int i = 0; i < messages.size(); i++) {
                    messages.get(i).setId(querySnapshot.getDocuments().get(i).getId());
                }
                callback.onSuccess(messages);
            })
            .addOnFailureListener(callback::onFailure);
    }

    /**
     * Sends a new secure message.
     *
     * @param message Message object to persist.
     * @param callback Success/failure callback.
     */
    public void sendMessage(SecureMessage message, OnMessageActionCallback callback) {
        if (message.getCreatedAt() == null) {
            message.setCreatedAt(Timestamp.now());
        }

        messagesCollection.add(message)
            .addOnSuccessListener(unused -> callback.onSuccess())
            .addOnFailureListener(callback::onFailure);
    }

    /**
     * Marks all messages for this appointment addressed to the current user as read.
     *
     * @param appointmentId Appointment ID.
     * @param receiverId Current user's UID.
     * @param callback Success/failure callback.
     */
    public void markMessagesRead(String appointmentId, String receiverId,
                                 OnMessageActionCallback callback) {
        messagesCollection
            .whereEqualTo("appointmentId", appointmentId)
            .whereEqualTo("receiverId", receiverId)
            .whereEqualTo("read", false)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                if (querySnapshot.isEmpty()) {
                    callback.onSuccess();
                    return;
                }

                FirebaseFirestore db = FirebaseFirestore.getInstance();
                com.google.firebase.firestore.WriteBatch batch = db.batch();

                for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    batch.update(doc.getReference(), "read", true);
                }

                batch.commit()
                    .addOnSuccessListener(unused -> callback.onSuccess())
                    .addOnFailureListener(callback::onFailure);
            })
            .addOnFailureListener(callback::onFailure);
    }
}
```

### 5.3 `ReturningStudentHelper.java`

**Purpose:** Compute whether a student is returning using existing appointment history.

```java
package com.example.moogerscouncil;

import java.util.List;

/**
 * Helper for deciding whether a student should be marked as returning.
 * A returning student has at least one previous completed or confirmed
 * appointment before the appointment currently displayed.
 */
public final class ReturningStudentHelper {

    private ReturningStudentHelper() {}

    /**
     * Checks if a student has a previous appointment before the current appointment.
     *
     * @param current Current appointment being rendered.
     * @param allAppointments All appointments for the counselor.
     * @return true if the same student has previous appointment history.
     */
    public static boolean isReturningStudent(Appointment current,
                                             List<Appointment> allAppointments) {
        if (current == null || allAppointments == null) {
            return false;
        }

        String currentStudentId = current.getStudentId();
        String currentDate = current.getDate();

        if (currentStudentId == null || currentDate == null) {
            return false;
        }

        for (Appointment other : allAppointments) {
            if (other == null || other.getId() == null) {
                continue;
            }

            if (other.getId().equals(current.getId())) {
                continue;
            }

            if (!currentStudentId.equals(other.getStudentId())) {
                continue;
            }

            String status = other.getStatus();
            boolean usefulStatus =
                "COMPLETED".equals(status) || "CONFIRMED".equals(status);

            if (!usefulStatus) {
                continue;
            }

            String otherDate = other.getDate();
            if (otherDate != null && otherDate.compareTo(currentDate) < 0) {
                return true;
            }
        }

        return false;
    }
}
```

---

## 6. Implementation Details — Admin UI

### 6.1 `AdminDashboardActivity.java`

**Purpose:** Admin landing page. Keep it simple and focused.

**UI requirements:**
- Greeting/title: "Admin Dashboard"
- Card/button: "Reminder Settings"
- Card/button: "Reminder Records" or small stats placeholder
- Logout button
- Optional simple stats:
  - Total configured reminder types enabled
  - Last updated timestamp
  - Prototype note: "Reminder delivery is simulated in-app for this phase"

**Activity logic:**
1. Load `ReminderRepository.getSettings()`.
2. Show whether 24-hour and 1-hour reminders are enabled.
3. Open `ReminderSettingsActivity` when the settings card is tapped.
4. Logout returns to `LoginActivity`.

Skeleton:

```java
package com.example.moogerscouncil;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Admin landing page for BetterCaps.
 * Provides entry points to admin-only system configuration screens.
 */
public class AdminDashboardActivity extends AppCompatActivity {

    private ReminderRepository reminderRepository;
    private TextView textReminderSummary;
    private MaterialButton btnReminderSettings;
    private MaterialButton btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        reminderRepository = new ReminderRepository();

        textReminderSummary = findViewById(R.id.textReminderSummary);
        btnReminderSettings = findViewById(R.id.btnReminderSettings);
        btnLogout = findViewById(R.id.btnAdminLogout);

        btnReminderSettings.setOnClickListener(v -> {
            startActivity(new Intent(this, ReminderSettingsActivity.class));
        });

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        loadReminderSummary();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadReminderSummary();
    }

    private void loadReminderSummary() {
        reminderRepository.getSettings(new ReminderRepository.OnSettingsLoadedCallback() {
            @Override
            public void onSuccess(ReminderSettings settings) {
                String summary = "24-hour reminders: "
                    + (settings.isEnabled24Hour() ? "Enabled" : "Disabled")
                    + "\n1-hour reminders: "
                    + (settings.isEnabled1Hour() ? "Enabled" : "Disabled");
                textReminderSummary.setText(summary);
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(AdminDashboardActivity.this,
                    getString(R.string.error_loading_reminder_settings),
                    Toast.LENGTH_SHORT).show();
            }
        });
    }
}
```

### 6.2 `activity_admin_dashboard.xml`

Use simple vertical layout. IDs must match Java exactly:

```
textAdminTitle
textAdminSubtitle
cardReminderSettings
textReminderSummary
btnReminderSettings
btnAdminLogout
```

### 6.3 `ReminderSettingsActivity.java`

**Purpose:** Admin can edit reminder settings.

**Fields:**
- `switch24HourReminder`
- `switch1HourReminder`
- `editMessage24Hour`
- `editMessage1Hour`
- `btnSaveReminderSettings`
- `btnBack`

**Activity behavior:**
1. Load current settings on start.
2. If no Firestore settings doc exists, load `ReminderSettings.defaultSettings()`.
3. Admin can toggle reminders and edit messages.
4. Save writes to `reminderSettings/default`.
5. Empty message should show validation error.
6. Save button should show progress or disable while saving.

Skeleton:

```java
package com.example.moogerscouncil;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Admin screen for editing global reminder settings.
 */
public class ReminderSettingsActivity extends AppCompatActivity {

    private ReminderRepository reminderRepository;

    private SwitchMaterial switch24HourReminder;
    private SwitchMaterial switch1HourReminder;
    private TextInputEditText editMessage24Hour;
    private TextInputEditText editMessage1Hour;
    private MaterialButton btnSaveReminderSettings;
    private MaterialButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder_settings);

        reminderRepository = new ReminderRepository();

        switch24HourReminder = findViewById(R.id.switch24HourReminder);
        switch1HourReminder = findViewById(R.id.switch1HourReminder);
        editMessage24Hour = findViewById(R.id.editMessage24Hour);
        editMessage1Hour = findViewById(R.id.editMessage1Hour);
        btnSaveReminderSettings = findViewById(R.id.btnSaveReminderSettings);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());
        btnSaveReminderSettings.setOnClickListener(v -> saveSettings());

        loadSettings();
    }

    private void loadSettings() {
        reminderRepository.getSettings(new ReminderRepository.OnSettingsLoadedCallback() {
            @Override
            public void onSuccess(ReminderSettings settings) {
                switch24HourReminder.setChecked(settings.isEnabled24Hour());
                switch1HourReminder.setChecked(settings.isEnabled1Hour());
                editMessage24Hour.setText(settings.getMessage24Hour());
                editMessage1Hour.setText(settings.getMessage1Hour());
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(ReminderSettingsActivity.this,
                    getString(R.string.error_loading_reminder_settings),
                    Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveSettings() {
        String msg24 = editMessage24Hour.getText() == null
            ? "" : editMessage24Hour.getText().toString().trim();
        String msg1 = editMessage1Hour.getText() == null
            ? "" : editMessage1Hour.getText().toString().trim();

        if (switch24HourReminder.isChecked() && msg24.isEmpty()) {
            editMessage24Hour.setError(getString(R.string.error_required));
            return;
        }

        if (switch1HourReminder.isChecked() && msg1.isEmpty()) {
            editMessage1Hour.setError(getString(R.string.error_required));
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String adminId = user == null ? "" : user.getUid();

        ReminderSettings settings = new ReminderSettings(
            switch24HourReminder.isChecked(),
            switch1HourReminder.isChecked(),
            msg24,
            msg1,
            adminId
        );

        btnSaveReminderSettings.setEnabled(false);

        reminderRepository.saveSettings(settings, new ReminderRepository.OnReminderActionCallback() {
            @Override
            public void onSuccess() {
                btnSaveReminderSettings.setEnabled(true);
                Toast.makeText(ReminderSettingsActivity.this,
                    getString(R.string.reminder_settings_saved),
                    Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                btnSaveReminderSettings.setEnabled(true);
                Toast.makeText(ReminderSettingsActivity.this,
                    getString(R.string.error_saving_reminder_settings),
                    Toast.LENGTH_SHORT).show();
            }
        });
    }
}
```

---

## 7. Implementation Details — Login Routing

### 7.1 `LoginActivity.java`

Find the role routing after login and auto-login. Update it so admin users go to `AdminDashboardActivity`.

Required routing:

```java
private void routeByRole(String role) {
    Intent intent;

    if (UserRole.COUNSELOR.equals(role)) {
        intent = new Intent(LoginActivity.this, CounselorDashboardActivity.class);
    } else if (UserRole.ADMIN.equals(role)) {
        intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
    } else {
        intent = new Intent(LoginActivity.this, StudentHomeActivity.class);
    }

    startActivity(intent);
    finish();
}
```

If `LoginActivity` does not currently have `routeByRole`, add it and use it in both:
- manual login success
- returning session auto-redirect

Do not break existing student/counselor routing.

---

## 8. Implementation Details — Returning Student Indicator

### 8.1 `AppointmentRepository.java`

Add a method that fetches appointments for one counselor and student, or reuse existing counselor appointment list if `AppointmentAdapter` already receives the full list.

Preferred lightweight method:

```java
public interface OnReturningStatusCallback {
    void onSuccess(boolean isReturning);
    void onFailure(Exception e);
}

/**
 * Checks whether a student has previous appointment history with a counselor.
 *
 * @param counselorId Counselor ID.
 * @param studentId Student ID.
 * @param currentAppointmentId Current appointment to exclude.
 * @param currentDate Current appointment date in yyyy-MM-dd format.
 * @param callback Receives true if returning.
 */
public void isReturningStudent(String counselorId, String studentId,
                               String currentAppointmentId, String currentDate,
                               OnReturningStatusCallback callback) {
    appointmentsCollection
        .whereEqualTo("counselorId", counselorId)
        .whereEqualTo("studentId", studentId)
        .get()
        .addOnSuccessListener(querySnapshot -> {
            boolean returning = false;

            for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                Appointment appointment = doc.toObject(Appointment.class);
                if (appointment == null) {
                    continue;
                }

                appointment.setId(doc.getId());

                if (appointment.getId().equals(currentAppointmentId)) {
                    continue;
                }

                String status = appointment.getStatus();
                boolean validStatus = "COMPLETED".equals(status) || "CONFIRMED".equals(status);

                if (!validStatus) {
                    continue;
                }

                String date = appointment.getDate();
                if (date != null && currentDate != null && date.compareTo(currentDate) < 0) {
                    returning = true;
                    break;
                }
            }

            callback.onSuccess(returning);
        })
        .addOnFailureListener(callback::onFailure);
}
```

### 8.2 `item_appointment.xml`

Add a badge view near student name/status:

```xml
<TextView
    android:id="@+id/textReturningBadge"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="@string/returning_student_badge"
    android:textSize="11sp"
    android:textStyle="bold"
    android:textColor="#6A1B9A"
    android:background="@drawable/bg_returning_badge"
    android:paddingStart="8dp"
    android:paddingEnd="8dp"
    android:paddingTop="4dp"
    android:paddingBottom="4dp"
    android:visibility="gone" />
```

Create drawable if needed:

```
res/drawable/bg_returning_badge.xml
```

Simple rounded lavender/purple background.

### 8.3 `AppointmentAdapter.java`

Add `TextView textReturningBadge` to ViewHolder.

In `onBindViewHolder`:
1. Default badge visibility to `GONE`.
2. Call `appointmentRepository.isReturningStudent(...)`.
3. If true, set visible.
4. On failure, keep hidden.

Do not block rendering while this loads.

```java
holder.textReturningBadge.setVisibility(View.GONE);

appointmentRepository.isReturningStudent(
    appointment.getCounselorId(),
    appointment.getStudentId(),
    appointment.getId(),
    appointment.getDate(),
    new AppointmentRepository.OnReturningStatusCallback() {
        @Override
        public void onSuccess(boolean isReturning) {
            holder.textReturningBadge.setVisibility(isReturning ? View.VISIBLE : View.GONE);
        }

        @Override
        public void onFailure(Exception e) {
            holder.textReturningBadge.setVisibility(View.GONE);
        }
    }
);
```

---

## 9. Implementation Details — Secure Pre-Session Messaging

### 9.1 Navigation Sources

Add message entry points in this order:

1. **Counselor side:** appointment card message button in `AppointmentAdapter`.
2. **Student side:** student appointment card message button in `StudentAppointmentAdapter` if simple.
3. **Fallback:** from `StudentHomeActivity` upcoming session card if adding a button is easier.

Do not make messaging dependent on all three sources. At minimum:
- counselor can open `MessageThreadActivity` for a selected appointment,
- student can open `MessageThreadActivity` for a selected appointment from one existing screen.

### 9.2 Intent Extras

Use stable names:

```
MessageThreadActivity.EXTRA_APPOINTMENT_ID = "APPOINTMENT_ID"
MessageThreadActivity.EXTRA_STUDENT_ID = "STUDENT_ID"
MessageThreadActivity.EXTRA_COUNSELOR_ID = "COUNSELOR_ID"
MessageThreadActivity.EXTRA_OTHER_NAME = "OTHER_NAME"
```

### 9.3 `MessageThreadActivity.java`

**Purpose:** Shows messages for one appointment and lets current user send a message.

Behavior:
1. Read appointment ID, student ID, counselor ID from intent.
2. Determine current user via `FirebaseAuth.getInstance().getCurrentUser()`.
3. Determine sender role:
   - if current UID equals counselorId or current user role is counselor → `counselor`
   - else `student`
4. Load messages using `SecureMessageRepository.getMessagesForAppointment()`.
5. Render messages with `SecureMessageAdapter`.
6. Send new message with `SecureMessageRepository.sendMessage()`.
7. Mark received messages read after loading.

UI fields:
```
textMessageTitle
recyclerMessages
editMessage
btnSendMessage
btnBack
progressMessages
textEmptyMessages
```

Skeleton:

```java
package com.example.moogerscouncil;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.ArrayList;
import java.util.List;

/**
 * Appointment-linked secure message thread.
 * Allows counselors and students to exchange pre-session messages.
 */
public class MessageThreadActivity extends AppCompatActivity {

    public static final String EXTRA_APPOINTMENT_ID = "APPOINTMENT_ID";
    public static final String EXTRA_STUDENT_ID = "STUDENT_ID";
    public static final String EXTRA_COUNSELOR_ID = "COUNSELOR_ID";
    public static final String EXTRA_OTHER_NAME = "OTHER_NAME";

    private String appointmentId;
    private String studentId;
    private String counselorId;
    private String currentUid;
    private String senderRole;

    private SecureMessageRepository messageRepository;
    private SecureMessageAdapter adapter;

    private TextView textMessageTitle;
    private RecyclerView recyclerMessages;
    private TextInputEditText editMessage;
    private MaterialButton btnSendMessage;
    private MaterialButton btnBack;
    private ProgressBar progressMessages;
    private TextView textEmptyMessages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_thread);

        appointmentId = getIntent().getStringExtra(EXTRA_APPOINTMENT_ID);
        studentId = getIntent().getStringExtra(EXTRA_STUDENT_ID);
        counselorId = getIntent().getStringExtra(EXTRA_COUNSELOR_ID);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        currentUid = user == null ? "" : user.getUid();

        if (currentUid.equals(counselorId)) {
            senderRole = UserRole.COUNSELOR;
        } else {
            senderRole = UserRole.STUDENT;
        }

        messageRepository = new SecureMessageRepository();

        textMessageTitle = findViewById(R.id.textMessageTitle);
        recyclerMessages = findViewById(R.id.recyclerMessages);
        editMessage = findViewById(R.id.editMessage);
        btnSendMessage = findViewById(R.id.btnSendMessage);
        btnBack = findViewById(R.id.btnBack);
        progressMessages = findViewById(R.id.progressMessages);
        textEmptyMessages = findViewById(R.id.textEmptyMessages);

        adapter = new SecureMessageAdapter(new ArrayList<>(), currentUid);
        recyclerMessages.setLayoutManager(new LinearLayoutManager(this));
        recyclerMessages.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());
        btnSendMessage.setOnClickListener(v -> sendMessage());

        loadMessages();
    }

    private void loadMessages() {
        progressMessages.setVisibility(View.VISIBLE);

        messageRepository.getMessagesForAppointment(
            appointmentId,
            new SecureMessageRepository.OnMessagesLoadedCallback() {
                @Override
                public void onSuccess(List<SecureMessage> messages) {
                    progressMessages.setVisibility(View.GONE);
                    adapter.setMessages(messages);

                    boolean empty = messages.isEmpty();
                    textEmptyMessages.setVisibility(empty ? View.VISIBLE : View.GONE);

                    if (!empty) {
                        recyclerMessages.scrollToPosition(messages.size() - 1);
                    }

                    messageRepository.markMessagesRead(
                        appointmentId,
                        currentUid,
                        new SecureMessageRepository.OnMessageActionCallback() {
                            @Override public void onSuccess() {}
                            @Override public void onFailure(Exception e) {}
                        }
                    );
                }

                @Override
                public void onFailure(Exception e) {
                    progressMessages.setVisibility(View.GONE);
                    Toast.makeText(MessageThreadActivity.this,
                        getString(R.string.error_loading_messages),
                        Toast.LENGTH_SHORT).show();
                }
            }
        );
    }

    private void sendMessage() {
        String text = editMessage.getText() == null
            ? "" : editMessage.getText().toString().trim();

        if (text.isEmpty()) {
            editMessage.setError(getString(R.string.error_required));
            return;
        }

        String receiverId;
        if (UserRole.COUNSELOR.equals(senderRole)) {
            receiverId = studentId;
        } else {
            receiverId = counselorId;
        }

        SecureMessage message = new SecureMessage(
            appointmentId,
            counselorId,
            studentId,
            currentUid,
            receiverId,
            senderRole,
            text
        );

        btnSendMessage.setEnabled(false);

        messageRepository.sendMessage(message, new SecureMessageRepository.OnMessageActionCallback() {
            @Override
            public void onSuccess() {
                btnSendMessage.setEnabled(true);
                editMessage.setText("");
                loadMessages();
            }

            @Override
            public void onFailure(Exception e) {
                btnSendMessage.setEnabled(true);
                Toast.makeText(MessageThreadActivity.this,
                    getString(R.string.error_sending_message),
                    Toast.LENGTH_SHORT).show();
            }
        });
    }
}
```

### 9.4 `SecureMessageAdapter.java`

Simple RecyclerView adapter:
- If `message.senderId.equals(currentUid)`, label "You"
- Otherwise label "Counselor" or "Student"
- Show message text and formatted timestamp
- Keep styling simple, no custom chat bubble dependency

Required ViewHolder IDs in `item_secure_message.xml`:
```
textMessageSender
textMessageBody
textMessageTime
```

### 9.5 `AppointmentAdapter.java` — Counselor Message Entry

If `item_appointment.xml` already has a placeholder action button for notes/profile/crisis, either:
- reuse an existing button if it is labelled "Message", or
- add a new `btnMessage`.

On click:

```java
Intent intent = new Intent(context, MessageThreadActivity.class);
intent.putExtra(MessageThreadActivity.EXTRA_APPOINTMENT_ID, appointment.getId());
intent.putExtra(MessageThreadActivity.EXTRA_STUDENT_ID, appointment.getStudentId());
intent.putExtra(MessageThreadActivity.EXTRA_COUNSELOR_ID, appointment.getCounselorId());
intent.putExtra(MessageThreadActivity.EXTRA_OTHER_NAME, holder.textStudentName.getText().toString());
context.startActivity(intent);
```

### 9.6 Student-Side Message Entry

Prefer `StudentAppointmentAdapter.java` because it already renders student appointments.

Add a "Messages" button or make a small message icon/button visible on appointment cards.

On click, pass the same extras to `MessageThreadActivity`.

If adding this to `StudentAppointmentAdapter` is too invasive, add a simple button on `StudentHomeActivity` upcoming session card:
- visible only if `currentUpcomingAppointment != null`
- opens `MessageThreadActivity` with that appointment's IDs

Do not overbuild this.

---

## 10. Implementation Details — Reminder Scheduler Helper

### 10.1 `ReminderScheduler.java`

**Purpose:** Prototype-safe helper, not a background service.

It should:
1. Load reminder settings.
2. Load upcoming confirmed appointments.
3. Decide which appointments are within 24-hour or 1-hour reminder windows.
4. Create reminder records if needed.

Do not attempt real push notifications. A `NotificationService.java` name is acceptable only if it is a local helper, not Android foreground/background service.

Skeleton concept:

```java
package com.example.moogerscouncil;

import com.google.firebase.Timestamp;
import java.util.List;

/**
 * Prototype-safe reminder scheduler.
 * It computes reminder records when called by the app/admin screen.
 * It does not run in the background or send push notifications.
 */
public class ReminderScheduler {

    private final ReminderRepository reminderRepository;
    private final AppointmentRepository appointmentRepository;

    public ReminderScheduler() {
        this.reminderRepository = new ReminderRepository();
        this.appointmentRepository = new AppointmentRepository();
    }

    public interface OnSchedulerCallback {
        void onSuccess(int recordsCreated);
        void onFailure(Exception e);
    }

    /**
     * Generates reminder records for upcoming confirmed appointments.
     * This method can be triggered manually from AdminDashboardActivity.
     *
     * @param callback Receives number of created records.
     */
    public void generateReminderRecords(OnSchedulerCallback callback) {
        // Keep implementation simple:
        // 1. reminderRepository.getSettings()
        // 2. appointmentRepository.getUpcomingConfirmedAppointments(...)
        // 3. For each appointment, create 24-hour and/or 1-hour records when enabled.
        // 4. Avoid duplicate prevention if complex; otherwise query existing records first.
    }
}
```

### 10.2 `AppointmentRepository.java`

Add method:

```java
public interface OnAppointmentsLoadedCallback {
    void onSuccess(List<Appointment> appointments);
    void onFailure(Exception e);
}

/**
 * Loads upcoming confirmed appointments for reminder scheduling.
 *
 * @param callback Receives appointment list.
 */
public void getUpcomingConfirmedAppointments(OnAppointmentsLoadedCallback callback) {
    appointmentsCollection
        .whereEqualTo("status", "CONFIRMED")
        .get()
        .addOnSuccessListener(querySnapshot -> {
            List<Appointment> appointments = querySnapshot.toObjects(Appointment.class);
            for (int i = 0; i < appointments.size(); i++) {
                appointments.get(i).setId(querySnapshot.getDocuments().get(i).getId());
            }
            callback.onSuccess(appointments);
        })
        .addOnFailureListener(callback::onFailure);
}
```

If `AppointmentRepository` already has a similar callback name, reuse the existing interface instead of duplicating.

---

## 11. Layout Requirements

### 11.1 `activity_admin_dashboard.xml`

Required IDs:

```
textAdminTitle
textAdminSubtitle
textReminderSummary
btnReminderSettings
btnGenerateReminderRecords
btnAdminLogout
```

### 11.2 `activity_reminder_settings.xml`

Required IDs:

```
switch24HourReminder
switch1HourReminder
editMessage24Hour
editMessage1Hour
btnSaveReminderSettings
btnBack
```

### 11.3 `activity_message_thread.xml`

Required IDs:

```
textMessageTitle
recyclerMessages
editMessage
btnSendMessage
btnBack
progressMessages
textEmptyMessages
```

### 11.4 `item_secure_message.xml`

Required IDs:

```
textMessageSender
textMessageBody
textMessageTime
```

### 11.5 `item_appointment.xml`

Add only if missing:

```
textReturningBadge
btnMessage
```

If `btnMessage` conflicts with the layout design, use the existing message/profile action area and document the ID choice in code comments.

---

## 12. String Resources

Add these to `strings.xml`:

```xml
<string name="admin_dashboard_title">Admin Dashboard</string>
<string name="admin_dashboard_subtitle">Manage BetterCaps system settings.</string>
<string name="reminder_settings">Reminder Settings</string>
<string name="reminder_summary_placeholder">Loading reminder settings...</string>
<string name="generate_reminder_records">Generate Reminder Records</string>
<string name="admin_logout">Logout</string>

<string name="reminder_settings_title">Reminder Settings</string>
<string name="enable_24_hour_reminder">Enable 24-hour reminder</string>
<string name="enable_1_hour_reminder">Enable 1-hour reminder</string>
<string name="message_24_hour_hint">24-hour reminder message</string>
<string name="message_1_hour_hint">1-hour reminder message</string>
<string name="save_reminder_settings">Save Settings</string>
<string name="reminder_settings_saved">Reminder settings saved.</string>
<string name="error_loading_reminder_settings">Could not load reminder settings.</string>
<string name="error_saving_reminder_settings">Could not save reminder settings.</string>

<string name="returning_student_badge">Returning Student</string>

<string name="messages">Messages</string>
<string name="message_thread_title">Pre-session Messages</string>
<string name="message_hint">Type a secure message...</string>
<string name="send_message">Send</string>
<string name="no_messages_yet">No messages yet.</string>
<string name="error_loading_messages">Could not load messages.</string>
<string name="error_sending_message">Could not send message.</string>

<string name="error_required">Required</string>
```

Reuse existing strings if they already exist. Do not duplicate names.

---

## 13. Android Manifest

Register:

```xml
<activity android:name=".AdminDashboardActivity" />
<activity android:name=".ReminderSettingsActivity" />
<activity android:name=".MessageThreadActivity" />
```

Do not change launcher activity unless the existing app already requires it.

---

## 14. Testing Strategy

### 14.1 Unit Tests

#### `ReminderSettingsTest.java`

Test:
1. Empty constructor exists.
2. Default settings are enabled.
3. Constructor sets messages correctly.
4. Setters update fields.
5. `updatedAt` is non-null after constructor.

#### `SecureMessageTest.java`

Test:
1. Empty constructor exists.
2. Constructor sets appointment/student/counselor/sender/receiver.
3. `read` defaults to false.
4. `createdAt` is non-null.
5. Setters work.

#### `ReturningStudentHelperTest.java`

Test:
1. Returns false for null list.
2. Returns false when only current appointment exists.
3. Returns true when previous completed appointment exists for same student.
4. Returns false when previous appointment belongs to different student.
5. Returns false when previous appointment is cancelled/no-show.
6. Returns false when previous appointment date is after current date.

### 14.2 Android Tests

#### `AdminDashboardTest.java`

Test:
1. Admin dashboard title is displayed.
2. Reminder settings button is displayed.
3. Logout button is displayed.

#### `ReminderSettingsFlowTest.java`

Test:
1. Reminder settings screen displays switches.
2. Message fields are visible.
3. Save button exists.

#### `MessageThreadFlowTest.java`

Test:
1. Message thread opens with title.
2. Message input is visible.
3. Send button is visible.
4. Empty-state text is shown if no messages.

### 14.3 Manual Testing Checklist

Admin:
- Log in as an admin user.
- Confirm routing opens `AdminDashboardActivity`.
- Open reminder settings.
- Toggle 24-hour and 1-hour reminders.
- Save and reopen; settings persist.

Counselor:
- Open dashboard.
- Existing appointment cards still load.
- Returning student badge appears only for students with prior history.
- Tap message action on appointment.
- Send a pre-session message.

Student:
- Open appointment/home/calendar message entry.
- Read counselor pre-session message.
- Send a reply if enabled.
- Existing booking, waitlist, feedback, and emergency flows still work.

---

## 15. Definition of Done

Sprint 10 is complete only when:

- `AdminDashboardActivity` exists and is reachable through admin login.
- `ReminderSettingsActivity` exists and saves settings to Firestore.
- `ReminderSettings` and `ReminderRepository` exist with Javadocs.
- Prototype reminder record generation exists through `ReminderScheduler` or equivalent.
- `SecureMessage`, `SecureMessageRepository`, `MessageThreadActivity`, and `SecureMessageAdapter` exist.
- Counselor can open a message thread from an appointment card.
- Student can view appointment-linked messages from at least one student-side screen.
- `AppointmentAdapter` shows a "Returning Student" badge where appropriate.
- `LoginActivity` routes `UserRole.ADMIN` to `AdminDashboardActivity`.
- All new activities are registered in `AndroidManifest.xml`.
- All user-visible strings are in `strings.xml`.
- No top-level `slots` collection is introduced.
- Existing student/counselor flows still compile and run.
- `.\gradlew.bat clean assembleDebug` passes.

---

## 16. Deliverables Checklist

### Models

- [ ] `ReminderSettings.java`
- [ ] `SecureMessage.java`

### Repositories / Helpers

- [ ] `ReminderRepository.java`
- [ ] `ReminderScheduler.java`
- [ ] `SecureMessageRepository.java`
- [ ] `ReturningStudentHelper.java`

### UI

- [ ] `AdminDashboardActivity.java`
- [ ] `ReminderSettingsActivity.java`
- [ ] `MessageThreadActivity.java`
- [ ] `SecureMessageAdapter.java`
- [ ] `activity_admin_dashboard.xml`
- [ ] `activity_reminder_settings.xml`
- [ ] `activity_message_thread.xml`
- [ ] `item_secure_message.xml`

### Existing File Updates

- [ ] `LoginActivity.java` routes admin correctly
- [ ] `AppointmentRepository.java` supports returning-student/reminder queries
- [ ] `AppointmentAdapter.java` shows returning badge and message action
- [ ] Student-side message entry point added
- [ ] `item_appointment.xml` updated
- [ ] `AndroidManifest.xml` updated
- [ ] `strings.xml` updated

### Tests

- [ ] `ReminderSettingsTest.java`
- [ ] `SecureMessageTest.java`
- [ ] `ReturningStudentHelperTest.java`
- [ ] `AdminDashboardTest.java`
- [ ] `ReminderSettingsFlowTest.java`
- [ ] `MessageThreadFlowTest.java`

### Build

- [ ] `.\gradlew.bat clean assembleDebug` passes
- [ ] No `.idea`, `.zip`, or `local.properties` files committed
- [ ] No duplicate parallel classes created
- [ ] No existing completed stories broken

---

## 17. Codex / Claude Implementation Prompt

Use this exact prompt when assigning the sprint:

```text
You are working in the Android project BetterCaps inside the repo CS360S26mooger/mooger-events.

Implement ONLY docs/sprints/SPRINT-10.md.

The admin side is currently not implemented. Build it from scratch while preserving existing student and counselor flows.

Remaining required scope:
1. US-16: Admin configures automated session reminders for 24 hours and 1 hour before appointments.
2. US-17: Counselor sees a Returning Student badge for students with prior appointment history.
3. US-18: Counselor sends secure appointment-linked pre-session messages to students.

Critical constraints:
1. Do not rewrite booking, waitlist, intake, buffer time, calendar export, crisis escalation, feedback, or emergency flows.
2. Preserve package name com.example.moogerscouncil.
3. Keep using the repository pattern.
4. Do not rename existing classes, layout IDs, Firestore collections, or intent extras unless required.
5. Keep existing nested slot structure: Slots/{counselorId}/slots/{slotId}. Do not create top-level slots.
6. Add AdminDashboardActivity, ReminderSettingsActivity, ReminderSettings, ReminderRepository, and ReminderScheduler.
7. Fix LoginActivity so UserRole.ADMIN routes to AdminDashboardActivity.
8. Add SecureMessage, SecureMessageRepository, SecureMessageAdapter, and MessageThreadActivity.
9. Integrate counselor-side message entry from AppointmentAdapter.
10. Add at least one student-side way to open appointment messages.
11. Add ReturningStudentHelper or equivalent logic and show a Returning Student badge on counselor appointment cards.
12. Register all new activities in AndroidManifest.xml.
13. Add all required XML layouts and IDs.
14. Put all user-facing strings in strings.xml.
15. Add requested unit/android tests if the existing test setup allows it.
16. Run .\gradlew.bat clean assembleDebug and fix build errors.

Goal:
Complete Sprint 10 without breaking any previously completed user stories.
```
