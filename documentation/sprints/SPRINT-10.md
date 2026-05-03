# Sprint 10 — US-16 (Admin Reminders) + US-18 (Secure Messaging) + US-17 Returning Student + Final Polish
### Admin, Messaging, Notifications, and Final Integration Sprint

---

## 0. Pre-Sprint Status Report

### Sprint 9.5 Review — Slot Management Overhaul & Bulk Generation: COMPLETE

Sprint 9.5 delivered the dashboard hardening, session management fixes, a full redesign of the counselor availability management flow, and automated bulk slot generation.

| Deliverable | Status | Notes |
|---|---|---|
| `AddSlotActivity.java` | Done | Calendar-first single slot creation; past dates blocked; existing slot dates highlighted. |
| `GenerateSlotsActivity.java` | Done | Full-day generation with work hours, slot duration (30/45/60 min), and break filtering. |
| `AvailabilityRepository.addSlotsBatch()` | Done | Single `WriteBatch` commit for all generated slots — atomic. |
| `SlotGroupAdapter` (inner class) | Done | Multi-view-type adapter: date headers + slot cards with optimistic deletion. |
| `item_slot_counselor.xml` | Done | Floating card per slot: time, Available/Booked tag, trash button. |
| `item_slot_date_header.xml` | Done | Pink date group header with calendar icon. |
| `activity_add_slot.xml` | Done | Mirrors student booking screen (20dp/8dp card). |
| `activity_generate_slots.xml` | Done | Full-day generation form with calendar, work hours, duration, breaks sections. |
| `dialog_add_break.xml` | Done | Break entry dialog with two time-picker buttons. |
| `item_break.xml` | Done | Break list row with time range and remove button. |
| `ic_sleeping.xml` | Done | Sleeping-person drawable; orange no-show theme applied sitewide. |
| `CounselorProfileEditActivity` M3 text fix | Done | `setTextColor(0xFF0D0D0D)` override post-`findViewById()` for all five fields. |

### Sprint 9.5 Phase A Review — Dashboard Hardening & Session Management: COMPLETE

Sprint 9.5 (Phase A) hardened the counselor dashboard and session note layer:

| Deliverable | Status | Notes |
|---|---|---|
| Session note deletion (US-31) | Done | `SessionNoteRepository.deleteNote()` + confirmation dialog in `SessionNoteActivity`. |
| Active-only appointment filter (US-32) | Done | `filterByTab()` stream-filters `CANCELLED`/`NO_SHOW` before adapter hand-off. |
| Appointment-anchored note timestamps (US-33) | Done | `SessionNoteHistoryAdapter.buildTimestamp()` uses appointment date/time fields. |
| `SessionCache` with TTL + background refresh (US-34) | Done | 2-min TTL for counselor appointments; cache-first render, Firestore refresh in background. |
| Live booking statistics (US-35) | Done | `updateStats()` filters `CONFIRMED` only for all three stat card counts. |
| No-show confirmation guard + orange theme (US-36) | Done | `confirmNoShow()` dialog; `ic_sleeping` drawable; `#E8761A` colour theme. |
| `loadAppointments()` → `subscribeToAppointments()` bug fix | Done | Two stale call sites in `CounselorDashboardActivity` corrected. |

### Current State Before Sprint 10

The app now supports the full end-to-end flow for students and counselors:

```text
Student:  registration → intake quiz → counselor recommendation → directory → booking/waitlist
          home → calendar → history → feedback → emergency → discreet mode

Counselor: dashboard (cache-first, active-only) → student profile → session history
           → notes (upsert, deletion) → crisis escalation → no-show (guarded, orange theme)
           → manage availability (date-grouped list, optimistic delete)
           → add slot (calendar-first, past-blocked, highlights) 
           → generate slots (work hours + duration + breaks → batch write)
           → waitlist queue → on-leave status → availability settings
```

The remaining backlog stories are system-level and final-polish features:

| Story | Current state | Sprint 10 target |
|---|---|---|
| **US-16** Admin automated reminders | Not implemented | Admin configures reminder settings; reminder records are generated/displayed. |
| **US-18** Secure pre-session message | Not implemented | Counselor sends appointment-linked message to student; student can read it. |
| **US-17** Returning student indicator | Not implemented | Dashboard shows returning badge if student has prior completed/no-show appointments. |
| Final polish | Partially done | Javadocs, repository consistency, tests, strings, manifest, build cleanup. |

### Important Scope Decision for Reminders

Android apps cannot reliably send server-side reminders to users when closed unless Firebase Cloud Messaging / Cloud Functions or WorkManager is configured. To keep this project buildable and demo-safe, Sprint 10 implements **prototype-level automated reminders**:

1. Admin configures reminder timing/text in Firestore.
2. `ReminderRepository` can create reminder records for upcoming appointments.
3. Student home shows due/pending reminders in-app.
4. Optional local notification scheduling can be added with WorkManager if already available in Gradle.

Do **not** add Cloud Functions or external server code unless explicitly required.

---

## 1. Sprint 10 Objective

By the end of this sprint:

1. **Admin can configure automated session reminder settings** such as 24-hour and 1-hour reminder toggles/text (US-16).
2. **The app can generate and display reminder records** for upcoming appointments using `ReminderRepository` (US-16 prototype implementation).
3. **Counselors can send secure pre-session messages** attached to an appointment (US-18).
4. **Students can view pre-session messages** from their home/calendar/appointment card (US-18).
5. **Counselor dashboard shows returning-student indicators** based on previous student appointments (US-17).
6. **Final polish:** tests, Javadocs, manifest entries, strings, and repository consistency.

---

## 2. Files to Create or Modify

### 2.1 New Files

```text
src/main/java/com/example/moogerscouncil/
├── AdminDashboardActivity.java          // UI — admin landing page
├── ReminderSettingsActivity.java        // UI — admin reminder configuration
├── ReminderSettings.java                // Model — reminder settings document
├── ReminderRecord.java                  // Model — generated reminder instance
├── ReminderRepository.java              // Repository — reminders/reminderSettings ops
├── SecureMessage.java                   // Model — appointment-linked secure message
├── SecureMessageRepository.java         // Repository — secureMessages collection
├── MessageThreadActivity.java           // UI — student/counselor message view
├── ReturningStudentHelper.java          // Helper — computes returning-student status

src/main/res/layout/
├── activity_admin_dashboard.xml         // Admin home screen
├── activity_reminder_settings.xml       // Reminder config form
├── activity_message_thread.xml          // Secure message thread UI
├── dialog_send_message.xml              // Counselor compose message dialog
├── item_secure_message.xml              // Message bubble row

src/test/java/com/example/moogerscouncil/
├── ReminderSettingsTest.java            // Unit test — defaults and toggles
├── ReminderRecordTest.java              // Unit test — fields/status
├── SecureMessageTest.java               // Unit test — model fields/read state
├── ReturningStudentHelperTest.java      // Unit test — returning detection

src/androidTest/java/com/example/moogerscouncil/
├── AdminReminderSettingsTest.java       // UI test — admin screen and save
├── SecureMessageFlowTest.java           // UI test — send/open message
├── ReturningStudentBadgeTest.java       // UI test — badge visible on appointment card
```

### 2.2 Files to Modify

```text
LoginActivity.java                  // Route ADMIN role to AdminDashboardActivity
UserRole.java                       // Ensure ADMIN constant exists
AppointmentAdapter.java             // Add Send Message button/action and returning badge
StudentAppointmentAdapter.java      // Add View Message action/badge for student side
CounselorDashboardActivity.java     // Use ReturningStudentHelper; refresh badges
StudentHomeActivity.java            // Show pending reminders and unread message count
CalendarActivity.java               // Show message/reminder indicators on appointments
AppointmentRepository.java          // Add helper for completed prior appointments if needed
UserRepository.java                 // Admin-safe role fetch if not already robust
AndroidManifest.xml                 // Register AdminDashboardActivity, ReminderSettingsActivity, MessageThreadActivity
strings.xml                         // All new strings
```

---

## 3. Firestore Data Model

### 3.1 Collection: `reminderSettings`

Use a single document for global settings:

```text
reminderSettings/default
  ├── id: String                     // "default"
  ├── enabled: Boolean
  ├── twentyFourHourEnabled: Boolean
  ├── oneHourEnabled: Boolean
  ├── twentyFourHourText: String
  ├── oneHourText: String
  ├── updatedBy: String              // admin UID
  └── updatedAt: Timestamp
```

### 3.2 Collection: `reminderRecords`

```text
reminderRecords/{recordId}
  ├── id: String
  ├── appointmentId: String
  ├── studentId: String
  ├── counselorId: String
  ├── type: String                   // "24_HOUR" | "1_HOUR"
  ├── message: String
  ├── dueAtLabel: String             // prototype-friendly string date/time
  ├── sent: Boolean
  ├── read: Boolean
  └── createdAt: Timestamp
```

### 3.3 Collection: `secureMessages`

```text
secureMessages/{messageId}
  ├── id: String
  ├── appointmentId: String
  ├── senderId: String
  ├── receiverId: String
  ├── counselorId: String
  ├── studentId: String
  ├── messageText: String
  ├── createdAt: Timestamp
  ├── read: Boolean
  └── deleted: Boolean
```

### 3.4 Existing `appointments` Optional Additions

```text
appointments/{appointmentId}
  ├── returningStudent: Boolean       // optional cached UI helper
  ├── unreadPreSessionMessage: Boolean
  ├── reminderGenerated: Boolean
  └── lastMessageAt: Timestamp
```

Do not require these fields for rendering; compute fallback in repositories/helpers.

---

## 4. Implementation Details — Model Layer

### 4.1 `ReminderSettings.java`

```java
package com.example.moogerscouncil;

import com.google.firebase.Timestamp;

/** Stores admin-configured reminder settings for appointment reminders. */
public class ReminderSettings {

    private String id;
    private boolean enabled;
    private boolean twentyFourHourEnabled;
    private boolean oneHourEnabled;
    private String twentyFourHourText;
    private String oneHourText;
    private String updatedBy;
    private Timestamp updatedAt;

    /** Required empty constructor for Firestore. */
    public ReminderSettings() {
        id = "default";
        enabled = true;
        twentyFourHourEnabled = true;
        oneHourEnabled = true;
        twentyFourHourText = "Reminder: you have a BetterCAPS session tomorrow.";
        oneHourText = "Reminder: your BetterCAPS session starts in one hour.";
    }

    public String getId() { return id; }
    public boolean isEnabled() { return enabled; }
    public boolean isTwentyFourHourEnabled() { return twentyFourHourEnabled; }
    public boolean isOneHourEnabled() { return oneHourEnabled; }
    public String getTwentyFourHourText() { return twentyFourHourText; }
    public String getOneHourText() { return oneHourText; }
    public String getUpdatedBy() { return updatedBy; }
    public Timestamp getUpdatedAt() { return updatedAt; }

    public void setId(String id) { this.id = id; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setTwentyFourHourEnabled(boolean enabled) { this.twentyFourHourEnabled = enabled; }
    public void setOneHourEnabled(boolean enabled) { this.oneHourEnabled = enabled; }
    public void setTwentyFourHourText(String text) { this.twentyFourHourText = text; }
    public void setOneHourText(String text) { this.oneHourText = text; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}
```

### 4.2 `ReminderRecord.java`

```java
package com.example.moogerscouncil;

import com.google.firebase.Timestamp;

/** Represents one generated appointment reminder for a student. */
public class ReminderRecord {

    public static final String TYPE_24_HOUR = "24_HOUR";
    public static final String TYPE_1_HOUR = "1_HOUR";

    private String id;
    private String appointmentId;
    private String studentId;
    private String counselorId;
    private String type;
    private String message;
    private String dueAtLabel;
    private boolean sent;
    private boolean read;
    private Timestamp createdAt;

    /** Required empty constructor for Firestore. */
    public ReminderRecord() {}

    public ReminderRecord(Appointment appointment, String type, String message) {
        this.appointmentId = appointment.getId();
        this.studentId = appointment.getStudentId();
        this.counselorId = appointment.getCounselorId();
        this.type = type;
        this.message = message;
        this.dueAtLabel = appointment.getDate() + " " + appointment.getTime();
        this.sent = false;
        this.read = false;
        this.createdAt = Timestamp.now();
    }

    // getters and setters for all fields
}
```

### 4.3 `SecureMessage.java`

```java
package com.example.moogerscouncil;

import com.google.firebase.Timestamp;

/** Appointment-linked secure pre-session message between counselor and student. */
public class SecureMessage {

    private String id;
    private String appointmentId;
    private String senderId;
    private String receiverId;
    private String counselorId;
    private String studentId;
    private String messageText;
    private Timestamp createdAt;
    private boolean read;
    private boolean deleted;

    /** Required empty constructor for Firestore. */
    public SecureMessage() {}

    public SecureMessage(String appointmentId, String senderId, String receiverId,
                         String counselorId, String studentId, String messageText) {
        this.appointmentId = appointmentId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.counselorId = counselorId;
        this.studentId = studentId;
        this.messageText = messageText;
        this.createdAt = Timestamp.now();
        this.read = false;
        this.deleted = false;
    }

    public String getId() { return id; }
    public String getAppointmentId() { return appointmentId; }
    public String getSenderId() { return senderId; }
    public String getReceiverId() { return receiverId; }
    public String getCounselorId() { return counselorId; }
    public String getStudentId() { return studentId; }
    public String getMessageText() { return messageText; }
    public Timestamp getCreatedAt() { return createdAt; }
    public boolean isRead() { return read; }
    public boolean isDeleted() { return deleted; }

    public void setId(String id) { this.id = id; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }
    public void setCounselorId(String counselorId) { this.counselorId = counselorId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public void setMessageText(String messageText) { this.messageText = messageText; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public void setRead(boolean read) { this.read = read; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
}
```

---

## 5. Repository Layer

### 5.1 `ReminderRepository.java`

```java
package com.example.moogerscouncil;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

/** Repository for reminder settings and generated reminder records. */
public class ReminderRepository {

    private final FirebaseFirestore db;
    private final CollectionReference settingsCollection;
    private final CollectionReference recordsCollection;

    public ReminderRepository() {
        db = FirebaseFirestore.getInstance();
        settingsCollection = db.collection("reminderSettings");
        recordsCollection = db.collection("reminderRecords");
    }

    public interface OnReminderSettingsCallback {
        void onSuccess(ReminderSettings settings);
        void onFailure(Exception e);
    }

    public interface OnReminderActionCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface OnReminderRecordsCallback {
        void onSuccess(List<ReminderRecord> records);
        void onFailure(Exception e);
    }

    public void getSettings(OnReminderSettingsCallback callback) {
        settingsCollection.document("default")
            .get()
            .addOnSuccessListener(doc -> {
                if (!doc.exists()) {
                    callback.onSuccess(new ReminderSettings());
                    return;
                }
                ReminderSettings settings = doc.toObject(ReminderSettings.class);
                if (settings == null) settings = new ReminderSettings();
                callback.onSuccess(settings);
            })
            .addOnFailureListener(callback::onFailure);
    }

    public void saveSettings(ReminderSettings settings, String adminUid,
                             OnReminderActionCallback callback) {
        settings.setId("default");
        settings.setUpdatedBy(adminUid);
        settings.setUpdatedAt(Timestamp.now());
        settingsCollection.document("default")
            .set(settings)
            .addOnSuccessListener(unused -> callback.onSuccess())
            .addOnFailureListener(callback::onFailure);
    }

    public void generateReminderRecords(Appointment appointment,
                                        ReminderSettings settings,
                                        OnReminderActionCallback callback) {
        if (!settings.isEnabled()) {
            callback.onSuccess();
            return;
        }
        db.runBatch(batch -> {
            if (settings.isTwentyFourHourEnabled()) {
                ReminderRecord record = new ReminderRecord(
                    appointment, ReminderRecord.TYPE_24_HOUR,
                    settings.getTwentyFourHourText());
                String id = recordsCollection.document().getId();
                record.setId(id);
                batch.set(recordsCollection.document(id), record);
            }
            if (settings.isOneHourEnabled()) {
                ReminderRecord record = new ReminderRecord(
                    appointment, ReminderRecord.TYPE_1_HOUR,
                    settings.getOneHourText());
                String id = recordsCollection.document().getId();
                record.setId(id);
                batch.set(recordsCollection.document(id), record);
            }
        }).addOnSuccessListener(unused -> callback.onSuccess())
          .addOnFailureListener(callback::onFailure);
    }

    public void getUnreadRemindersForStudent(String studentId,
                                             OnReminderRecordsCallback callback) {
        recordsCollection.whereEqualTo("studentId", studentId)
            .whereEqualTo("read", false)
            .get()
            .addOnSuccessListener(snapshot -> {
                List<ReminderRecord> records = new ArrayList<>();
                for (int i = 0; i < snapshot.size(); i++) {
                    ReminderRecord record = snapshot.getDocuments().get(i).toObject(ReminderRecord.class);
                    if (record != null) {
                        record.setId(snapshot.getDocuments().get(i).getId());
                        records.add(record);
                    }
                }
                callback.onSuccess(records);
            })
            .addOnFailureListener(callback::onFailure);
    }
}
```

### 5.2 `SecureMessageRepository.java`

```java
package com.example.moogerscouncil;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

/** Repository for appointment-linked secure messages. */
public class SecureMessageRepository {

    private final CollectionReference messagesCollection;

    public SecureMessageRepository() {
        messagesCollection = FirebaseFirestore.getInstance().collection("secureMessages");
    }

    public interface OnMessageActionCallback {
        void onSuccess(String messageId);
        void onFailure(Exception e);
    }

    public interface OnMessagesLoadedCallback {
        void onSuccess(List<SecureMessage> messages);
        void onFailure(Exception e);
    }

    public void sendMessage(SecureMessage message, OnMessageActionCallback callback) {
        String id = messagesCollection.document().getId();
        message.setId(id);
        messagesCollection.document(id)
            .set(message)
            .addOnSuccessListener(unused -> callback.onSuccess(id))
            .addOnFailureListener(callback::onFailure);
    }

    public void getMessagesForAppointment(String appointmentId,
                                          OnMessagesLoadedCallback callback) {
        messagesCollection.whereEqualTo("appointmentId", appointmentId)
            .whereEqualTo("deleted", false)
            .get()
            .addOnSuccessListener(snapshot -> {
                List<SecureMessage> messages = new ArrayList<>();
                for (int i = 0; i < snapshot.size(); i++) {
                    SecureMessage message = snapshot.getDocuments().get(i).toObject(SecureMessage.class);
                    if (message != null) {
                        message.setId(snapshot.getDocuments().get(i).getId());
                        messages.add(message);
                    }
                }
                messages.sort((a, b) -> String.valueOf(a.getCreatedAt()).compareTo(String.valueOf(b.getCreatedAt())));
                callback.onSuccess(messages);
            })
            .addOnFailureListener(callback::onFailure);
    }

    public void getUnreadMessagesForStudent(String studentId,
                                            OnMessagesLoadedCallback callback) {
        messagesCollection.whereEqualTo("studentId", studentId)
            .whereEqualTo("read", false)
            .whereEqualTo("deleted", false)
            .get()
            .addOnSuccessListener(snapshot -> {
                List<SecureMessage> messages = new ArrayList<>();
                for (int i = 0; i < snapshot.size(); i++) {
                    SecureMessage message = snapshot.getDocuments().get(i).toObject(SecureMessage.class);
                    if (message != null) {
                        message.setId(snapshot.getDocuments().get(i).getId());
                        messages.add(message);
                    }
                }
                callback.onSuccess(messages);
            })
            .addOnFailureListener(callback::onFailure);
    }
}
```

### 5.3 `ReturningStudentHelper.java`

```java
package com.example.moogerscouncil;

import java.util.List;

/** Helper for determining whether a student has prior session history. */
public final class ReturningStudentHelper {

    private ReturningStudentHelper() {}

    public static boolean isReturningStudent(String currentAppointmentId,
                                             List<Appointment> studentAppointments) {
        if (studentAppointments == null) return false;
        for (Appointment appointment : studentAppointments) {
            if (appointment == null) continue;
            if (currentAppointmentId != null && currentAppointmentId.equals(appointment.getId())) {
                continue;
            }
            String status = appointment.getStatus();
            if ("COMPLETED".equals(status) || "NO_SHOW".equals(status)) {
                return true;
            }
        }
        return false;
    }
}
```

---

## 6. UI Layer

### 6.1 `AdminDashboardActivity.java`

Admin dashboard should be simple:

```text
BetterCAPS Admin
├── Reminder Settings
├── Feedback Overview (optional, anonymous only)
├── Open Counselor Directory (optional)
└── Logout
```

`LoginActivity` already routes based on role. Add:

```java
if (UserRole.ADMIN.equals(role)) {
    startActivity(new Intent(LoginActivity.this, AdminDashboardActivity.class));
} else if (UserRole.COUNSELOR.equals(role)) {
    startActivity(new Intent(LoginActivity.this, CounselorDashboardActivity.class));
} else {
    startActivity(new Intent(LoginActivity.this, StudentHomeActivity.class));
}
```

### 6.2 `ReminderSettingsActivity.java`

Screen fields:

```text
Automated Reminders
├── Enable reminders switch
├── 24-hour reminder switch
├── 24-hour message text field
├── 1-hour reminder switch
├── 1-hour message text field
└── Save Settings
```

Use `ReminderRepository.getSettings()` on load and `saveSettings()` on save.

### 6.3 `AppointmentAdapter.java` — Secure Message and Returning Badge

Add or reuse one existing action button:

| Button | New behavior |
|---|---|
| Message | Opens `dialog_send_message.xml` and sends `SecureMessage` |
| Profile | Still opens `StudentProfileActivity` |
| Notes | Still opens notes dialog |
| Crisis | Still opens crisis escalation |

If adding a new button is too much layout work, repurpose the previous `Join` button as `Message`.

Returning badge logic:

1. In `onBindViewHolder`, call `AppointmentRepository.getAppointmentsForStudentHistory(appointment.getStudentId())`.
2. Run `ReturningStudentHelper.isReturningStudent(appointment.getId(), appointments)`.
3. If true, show badge: `Returning Student`.

### 6.4 `StudentAppointmentAdapter.java` and `MessageThreadActivity.java`

Student-side appointment card should show:

```text
Message from counselor
[View Message]
```

`MessageThreadActivity` receives:

```java
intent.putExtra("APPOINTMENT_ID", appointment.getId());
intent.putExtra("STUDENT_ID", appointment.getStudentId());
intent.putExtra("COUNSELOR_ID", appointment.getCounselorId());
```

It loads messages via `SecureMessageRepository.getMessagesForAppointment(appointmentId)`.

### 6.5 `StudentHomeActivity.java` — Reminders and Unread Messages

On resume:

```text
load unread reminders → show small card if count > 0
load unread messages → show message card if count > 0
```

Do not block the main home screen if these fail. They are supplementary.

---

## 7. Reminder Generation Hook

Best location: after successful booking in `AppointmentRepository.bookAppointment()` or immediately after booking success in `BookingActivity`.

Simpler and safer: do it in `BookingActivity` after `onSuccess()`:

```java
reminderRepository.getSettings(new ReminderRepository.OnReminderSettingsCallback() {
    @Override
    public void onSuccess(ReminderSettings settings) {
        Appointment appointment = new Appointment();
        appointment.setId(createdAppointmentId); // if available; otherwise skip generation
        appointment.setStudentId(currentUser.getUid());
        appointment.setCounselorId(counselorId);
        appointment.setDate(selectedSlot.getDate());
        appointment.setTime(selectedSlot.getTime());
        reminderRepository.generateReminderRecords(appointment, settings, ...);
    }

    @Override
    public void onFailure(Exception e) {
        // Booking already succeeded; do not fail booking because reminders failed.
    }
});
```

If `bookAppointment()` does not return the appointment ID, modify `OnBookingCallback.onSuccess()` to `onSuccess(String appointmentId)` carefully and update all callers. If that is too risky, create reminders later by scanning confirmed appointments from admin screen.

Recommended low-risk approach:

```text
AdminDashboardActivity → Generate Missing Reminders
  ├── AppointmentRepository.getAllConfirmedAppointments() // add helper
  └── ReminderRepository.generateReminderRecords(...)
```

This avoids changing the booking callback signature.

---

## 8. Strings to Add

```xml
<string name="admin_dashboard_title">BetterCAPS Admin</string>
<string name="reminder_settings">Reminder Settings</string>
<string name="automated_reminders_title">Automated Reminders</string>
<string name="enable_reminders">Enable reminders</string>
<string name="enable_24h_reminder">Enable 24-hour reminder</string>
<string name="enable_1h_reminder">Enable 1-hour reminder</string>
<string name="reminder_24h_hint">24-hour reminder message</string>
<string name="reminder_1h_hint">1-hour reminder message</string>
<string name="reminder_settings_saved">Reminder settings saved.</string>
<string name="reminder_settings_error">Could not save reminder settings.</string>
<string name="send_secure_message">Send Message</string>
<string name="secure_message_title">Secure Message</string>
<string name="message_hint">Write a pre-session message...</string>
<string name="message_sent">Message sent.</string>
<string name="message_send_error">Could not send message.</string>
<string name="view_messages">View Messages</string>
<string name="returning_student">Returning Student</string>
<string name="unread_messages">Unread Messages</string>
<string name="appointment_reminders">Appointment Reminders</string>
```

---

## 9. Testing Requirements

### 9.1 Unit Tests

#### `ReminderSettingsTest.java`

1. Empty constructor enables reminders by default.
2. Text fields are non-empty by default.
3. Switch setters update values.

#### `ReminderRecordTest.java`

1. Constructor copies appointment ID/student/counselor.
2. Type constants are correct.
3. Default read/sent values are false.

#### `SecureMessageTest.java`

1. Constructor stores appointment/sender/receiver/message.
2. Default read is false.
3. Default deleted is false.

#### `ReturningStudentHelperTest.java`

1. No appointments → false.
2. Only current appointment → false.
3. Prior completed appointment → true.
4. Prior no-show appointment → true.
5. Prior cancelled appointment only → false.

### 9.2 UI Tests

#### `AdminReminderSettingsTest.java`

1. Admin dashboard opens for admin role.
2. Reminder Settings button opens settings screen.
3. Switches and text fields are visible.
4. Save button shows success.

#### `SecureMessageFlowTest.java`

1. Counselor appointment card opens message dialog.
2. Empty message is rejected.
3. Valid message shows success.
4. Student appointment card opens messages screen.

#### `ReturningStudentBadgeTest.java`

1. Appointment card displays `Returning Student` when helper returns true / seeded history exists.

---

## 10. Final Repository Consistency Pass

Before declaring the sprint complete, check these consistency items:

### 10.1 Firestore Access

No Activity should directly call `FirebaseFirestore.getInstance()` except where already accepted legacy code exists. Prefer repositories:

| Collection | Repository |
|---|---|
| `users` | `UserRepository` |
| `counselors` | `CounselorRepository` |
| `appointments` | `AppointmentRepository` |
| `Slots/{counselorId}/slots` | `AvailabilityRepository` |
| `feedback` | `FeedbackRepository` |
| `intakeAssessments` | `IntakeAssessmentRepository` |
| `waitlist` | `WaitlistRepository` |
| `sessionNotes` | `SessionNoteRepository` |
| `crisisEscalations` | `CrisisEscalationRepository` |
| `availabilitySettings` | `AvailabilitySettingsRepository` |
| `reminderSettings` / `reminderRecords` | `ReminderRepository` |
| `secureMessages` | `SecureMessageRepository` |

### 10.2 Manifest

Register:

```xml
<activity android:name=".AdminDashboardActivity" />
<activity android:name=".ReminderSettingsActivity" />
<activity android:name=".MessageThreadActivity" />
```

Already registered from earlier sprints should include:

```xml
<activity android:name=".StudentProfileActivity" />
<activity android:name=".SessionHistoryActivity" />
<activity android:name=".AvailabilitySettingsActivity" />
```

### 10.3 Javadocs

Every new model class must have class-level Javadoc and public method Javadocs where appropriate:

```text
ReminderSettings.java
ReminderRecord.java
SecureMessage.java
WaitlistOffer.java
AvailabilitySettings.java
SessionNote.java
CrisisEscalation.java
IntakeAssessment.java
WaitlistEntry.java
```

### 10.4 Strings

No new hardcoded user-facing strings in Java/XML. All go in `strings.xml`.

### 10.5 Build Safety

Do not add external dependencies for notifications/OAuth unless Gradle is updated and the app builds. Prefer built-in Android intents and Firestore.

---

## 11. Definition of Done

Sprint 10 is done only when:

- Admin role routes to `AdminDashboardActivity`.
- Admin can configure reminder settings and save them to Firestore.
- Reminder records can be generated or displayed in-app for students.
- Counselor can send appointment-linked secure pre-session messages.
- Student can view secure messages.
- Counselor appointment cards show returning-student badge when applicable.
- All new screens are registered in `AndroidManifest.xml`.
- All new strings are in `strings.xml`.
- Unit and UI tests are added.
- New model classes have Javadocs.
- The app still supports the full end-to-end flow from previous sprints.

---

## 12. Deliverables Checklist

| Deliverable | Status |
|---|---|
| `AdminDashboardActivity.java` | Todo |
| `ReminderSettingsActivity.java` | Todo |
| `ReminderSettings.java` | Todo |
| `ReminderRecord.java` | Todo |
| `ReminderRepository.java` | Todo |
| `SecureMessage.java` | Todo |
| `SecureMessageRepository.java` | Todo |
| `MessageThreadActivity.java` | Todo |
| `ReturningStudentHelper.java` | Todo |
| `AppointmentAdapter.java` message + returning badge | Todo |
| `StudentAppointmentAdapter.java` view messages | Todo |
| `StudentHomeActivity.java` reminders/messages cards | Todo |
| `LoginActivity.java` admin routing | Todo |
| `activity_admin_dashboard.xml` | Todo |
| `activity_reminder_settings.xml` | Todo |
| `activity_message_thread.xml` | Todo |
| `dialog_send_message.xml` | Todo |
| `item_secure_message.xml` | Todo |
| `strings.xml` additions | Todo |
| `AndroidManifest.xml` updates | Todo |
| Unit tests | Todo |
| UI tests | Todo |
