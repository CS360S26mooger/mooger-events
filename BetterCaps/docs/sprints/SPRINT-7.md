# Sprint 7 — US-08 (Buffer Time) + US-09 (Calendar Sync) + Scheduling Hardening
### Availability and Connecting Logic Sprint

---

## 0. Pre-Sprint Status Report

### Sprint 6 Review — Counselor Workflow: COMPLETE

Sprint 6 completed counselor-side workflows on top of the existing dashboard:

| Deliverable | Expected status | Notes |
|---|---|---|
| `StudentProfileActivity.java` | Done | Counselor can view student and latest intake. |
| `SessionHistoryActivity.java` | Done | Counselor sees chronological appointment history. |
| `SessionNoteRepository.java` | Done | Notes saved in `sessionNotes`. |
| `CrisisEscalationRepository.java` | Done | Crisis escalation records saved. |
| `AppointmentAdapter.java` | Hardened | Profile, notes, crisis, no-show actions wired. |
| `CounselorDashboardActivity.java` | Hardened | Waitlist count added. |

### Current Scheduling State Before Sprint 7

| File | Current state | Sprint 7 gap |
|---|---|---|
| `AvailabilitySetupActivity.java` | Counselor can add/delete slots | No buffer-time settings. No calendar import/export. |
| `AvailabilityRepository.java` | Reads/writes nested slot documents | Needs buffer-aware slot creation and availability checks. |
| `BookingActivity.java` | Shows available slots and books atomically | Must respect buffer rules, on-leave redirect, and waitlist offers. |
| `AppointmentRepository.java` | Transaction booking and cancellation | Must notify/use waitlist when slot opens after cancellation. |
| `CounselorProfileEditActivity.java` | Edits bio, tags, language, gender, on-leave | Needs scheduling preferences section. |
| `WaitlistRepository.java` | Active waitlist entries/counts | Needs offer-next-student helper. |

### Stories Covered in This Sprint

| Story | Current status | Sprint 7 target |
|---|---|---|
| **US-08** Buffer time | Not implemented | Counselor can set buffer minutes; slot creation/booking respects buffer. |
| **US-09** External calendar sync | Not implemented | Add safe, simplified calendar export/import/intents without complex OAuth. |
| **US-01 hardening** | Booking works | Booking respects buffer, waitlist offers, on-leave, and no-slot UX. |
| **US-19 hardening** | On-leave works | On-leave redirects to referral/waitlist consistently. |
| **US-24 hardening** | Waitlist join works | Cancellation/slot opening can offer next student. |

---

## 1. Sprint 7 Objective

By the end of this sprint:

1. **Counselors can set buffer time between sessions** from their profile/availability settings (US-08).
2. **Availability creation prevents too-close slots** based on the counselor's configured buffer minutes (US-08).
3. **Booking displays only buffer-valid slots** and handles race conditions without breaking the waitlist flow (US-01 hardening).
4. **When a slot becomes available after cancellation, the next waitlisted student can be offered that slot** (US-24 hardening).
5. **Counselors can export a session to an external calendar or open an external calendar intent** as a simplified version of calendar sync (US-09).

### Calendar Sync Scope Decision

Full Google Calendar / Outlook two-way sync requires OAuth, provider APIs, token storage, and background refresh. That is high-risk for the course timeline.

This sprint implements a **safe D4-compatible simplified sync**:

| Feature | Implemented now | Deferred |
|---|---|---|
| Add appointment to external calendar | Yes, via Android calendar insert intent | Full OAuth create event API |
| Export all appointments | Yes, optional `.ics` text generation helper | Automatic background syncing |
| External commitments blocking slots | Manual import/block slots from counselor UI | Real provider conflict detection |
| Calendar provider preference | Stored in Firestore | Token management |

This is acceptable as a working prototype and can be clearly explained in the demo.

---

## 2. Files to Create or Modify

### 2.1 New Files

```text
src/main/java/com/example/moogerscouncil/
├── AvailabilitySettings.java             // Model — counselor scheduling settings
├── AvailabilitySettingsRepository.java   // Repository — availabilitySettings collection
├── CalendarSyncHelper.java               // Helper — Android calendar intent + ICS text
├── WaitlistOffer.java                    // Model — slot offered to waitlisted student

src/main/res/layout/
├── activity_availability_settings.xml    // Buffer time + calendar preferences
├── dialog_waitlist_offer.xml             // Student offer prompt / optional for future

src/test/java/com/example/moogerscouncil/
├── AvailabilitySettingsTest.java         // Unit test — defaults and fields
├── BufferTimeValidatorTest.java          // Unit test — conflict detection
├── CalendarSyncHelperTest.java           // Unit test — ICS text/intent data helpers

src/androidTest/java/com/example/moogerscouncil/
├── AvailabilitySettingsFlowTest.java     // UI test — buffer settings screen
├── BufferBookingFlowTest.java            // UI test — close slots blocked/hidden
```

### 2.2 Files to Modify

```text
AvailabilitySetupActivity.java       // Link settings screen; validate new slots against buffer
AvailabilityRepository.java          // Add buffer-aware checks and getSlotsForDate()
BookingActivity.java                 // Hide buffer-invalid slots; waitlist empty state hardening
AppointmentRepository.java           // On cancel, offer slot to next waitlisted student
CounselorProfileEditActivity.java    // Add scheduling settings entry point
CounselorDashboardActivity.java      // Add Export to Calendar / availability settings button
WaitlistRepository.java              // Add getNextActiveEntry(), markOffered(), markBooked()
StudentHomeActivity.java             // Show offered waitlist slot if implemented immediately
TimeSlot.java                        // No schema change required; optional helper methods only
strings.xml                          // All new user-facing strings
AndroidManifest.xml                  // Register AvailabilitySettingsActivity if created
```

---

## 3. Firestore Data Model

### 3.1 Collection: `availabilitySettings`

```text
availabilitySettings/{counselorId}
  ├── counselorId: String
  ├── bufferMinutes: int              // 0, 10, 15, 30
  ├── externalCalendarEnabled: Boolean
  ├── calendarProvider: String        // "NONE" | "GOOGLE" | "OUTLOOK" | "DEVICE"
  ├── exportIcsEnabled: Boolean
  └── updatedAt: Timestamp
```

### 3.2 Collection: `waitlistOffers`

```text
waitlistOffers/{offerId}
  ├── id: String
  ├── waitlistEntryId: String
  ├── studentId: String
  ├── counselorId: String
  ├── slotId: String
  ├── date: String
  ├── time: String
  ├── status: String                  // "OFFERED" | "ACCEPTED" | "DECLINED" | "EXPIRED"
  ├── offeredAt: Timestamp
  └── expiresAt: Timestamp            // optional; can be null in prototype
```

### 3.3 Existing `waitlist` Updates

```text
waitlist/{entryId}
  ├── status: "ACTIVE" | "OFFERED" | "BOOKED" | "CANCELLED" | "EXPIRED"
  ├── offeredSlotId: String
  └── offeredAt: Timestamp
```

---

## 4. Implementation Details — Model Layer

### 4.1 `AvailabilitySettings.java`

```java
package com.example.moogerscouncil;

import com.google.firebase.Timestamp;

/** Stores counselor-level scheduling preferences such as buffer time. */
public class AvailabilitySettings {

    public static final String PROVIDER_NONE = "NONE";
    public static final String PROVIDER_GOOGLE = "GOOGLE";
    public static final String PROVIDER_OUTLOOK = "OUTLOOK";
    public static final String PROVIDER_DEVICE = "DEVICE";

    private String counselorId;
    private int bufferMinutes;
    private boolean externalCalendarEnabled;
    private String calendarProvider;
    private boolean exportIcsEnabled;
    private Timestamp updatedAt;

    /** Required empty constructor for Firestore. */
    public AvailabilitySettings() {
        bufferMinutes = 0;
        calendarProvider = PROVIDER_NONE;
        externalCalendarEnabled = false;
        exportIcsEnabled = false;
    }

    public AvailabilitySettings(String counselorId, int bufferMinutes) {
        this.counselorId = counselorId;
        this.bufferMinutes = bufferMinutes;
        this.calendarProvider = PROVIDER_DEVICE;
        this.externalCalendarEnabled = false;
        this.exportIcsEnabled = true;
        this.updatedAt = Timestamp.now();
    }

    public String getCounselorId() { return counselorId; }
    public int getBufferMinutes() { return bufferMinutes; }
    public boolean isExternalCalendarEnabled() { return externalCalendarEnabled; }
    public String getCalendarProvider() { return calendarProvider; }
    public boolean isExportIcsEnabled() { return exportIcsEnabled; }
    public Timestamp getUpdatedAt() { return updatedAt; }

    public void setCounselorId(String counselorId) { this.counselorId = counselorId; }
    public void setBufferMinutes(int bufferMinutes) { this.bufferMinutes = bufferMinutes; }
    public void setExternalCalendarEnabled(boolean externalCalendarEnabled) { this.externalCalendarEnabled = externalCalendarEnabled; }
    public void setCalendarProvider(String calendarProvider) { this.calendarProvider = calendarProvider; }
    public void setExportIcsEnabled(boolean exportIcsEnabled) { this.exportIcsEnabled = exportIcsEnabled; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}
```

### 4.2 `WaitlistOffer.java`

```java
package com.example.moogerscouncil;

import com.google.firebase.Timestamp;

/** Represents a slot offered to a waitlisted student. */
public class WaitlistOffer {

    public static final String STATUS_OFFERED = "OFFERED";
    public static final String STATUS_ACCEPTED = "ACCEPTED";
    public static final String STATUS_DECLINED = "DECLINED";
    public static final String STATUS_EXPIRED = "EXPIRED";

    private String id;
    private String waitlistEntryId;
    private String studentId;
    private String counselorId;
    private String slotId;
    private String date;
    private String time;
    private String status;
    private Timestamp offeredAt;
    private Timestamp expiresAt;

    /** Required empty constructor for Firestore. */
    public WaitlistOffer() {}

    public WaitlistOffer(WaitlistEntry entry, TimeSlot slot) {
        this.waitlistEntryId = entry.getId();
        this.studentId = entry.getStudentId();
        this.counselorId = entry.getCounselorId();
        this.slotId = slot.getId();
        this.date = slot.getDate();
        this.time = slot.getTime();
        this.status = STATUS_OFFERED;
        this.offeredAt = Timestamp.now();
    }

    // getters and setters for all fields
}
```

---

## 5. Implementation Details — Repository Layer

### 5.1 `AvailabilitySettingsRepository.java`

```java
package com.example.moogerscouncil;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

/** Repository for counselor availability settings. */
public class AvailabilitySettingsRepository {

    private final CollectionReference settingsCollection;

    public AvailabilitySettingsRepository() {
        settingsCollection = FirebaseFirestore.getInstance().collection("availabilitySettings");
    }

    public interface OnSettingsLoadedCallback {
        void onSuccess(AvailabilitySettings settings);
        void onFailure(Exception e);
    }

    public interface OnSettingsSavedCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public void getSettings(String counselorId, OnSettingsLoadedCallback callback) {
        settingsCollection.document(counselorId)
            .get()
            .addOnSuccessListener(doc -> {
                if (!doc.exists()) {
                    callback.onSuccess(new AvailabilitySettings(counselorId, 0));
                    return;
                }
                AvailabilitySettings settings = doc.toObject(AvailabilitySettings.class);
                if (settings == null) settings = new AvailabilitySettings(counselorId, 0);
                settings.setCounselorId(counselorId);
                callback.onSuccess(settings);
            })
            .addOnFailureListener(callback::onFailure);
    }

    public void saveSettings(AvailabilitySettings settings,
                             OnSettingsSavedCallback callback) {
        settings.setUpdatedAt(Timestamp.now());
        settingsCollection.document(settings.getCounselorId())
            .set(settings)
            .addOnSuccessListener(unused -> callback.onSuccess())
            .addOnFailureListener(callback::onFailure);
    }
}
```

### 5.2 `AvailabilityRepository.java` Additions

Add date query and buffer validation helper.

```java
public interface OnAvailabilityCheckCallback {
    void onAvailable();
    void onConflict(String reason);
    void onFailure(Exception e);
}

public void canAddSlotWithBuffer(String counselorId, String date, String time,
                                 int bufferMinutes,
                                 OnAvailabilityCheckCallback callback) {
    getSlotsForCounselor(counselorId, new OnSlotsLoadedCallback() {
        @Override
        public void onSuccess(List<TimeSlot> slots) {
            for (TimeSlot slot : slots) {
                if (!date.equals(slot.getDate())) continue;
                if (isWithinBuffer(slot.getTime(), time, bufferMinutes)) {
                    callback.onConflict("Slot conflicts with buffer time around " + slot.getTime());
                    return;
                }
            }
            callback.onAvailable();
        }

        @Override
        public void onFailure(Exception e) {
            callback.onFailure(e);
        }
    });
}

private boolean isWithinBuffer(String existingTime, String newTime, int bufferMinutes) {
    int existing = parseMinutes(existingTime);
    int proposed = parseMinutes(newTime);
    return Math.abs(existing - proposed) < bufferMinutes;
}

private int parseMinutes(String hhmm) {
    String[] parts = hhmm.split(":");
    return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
}
```

**Important:** If sessions are assumed to be 60 minutes, use:

```java
return Math.abs(existing - proposed) < (60 + bufferMinutes);
```

Choose one rule and document it. For BetterCAPS, use **60-minute default session length + buffer** because two sessions at 10:00 and 10:15 should never both be valid.

### 5.3 `WaitlistRepository.java` Additions

```java
public interface OnNextWaitlistCallback {
    void onSuccess(WaitlistEntry entry);
    void onEmpty();
    void onFailure(Exception e);
}

public void getNextActiveEntry(String counselorId, OnNextWaitlistCallback callback) {
    getActiveWaitlistForCounselor(counselorId, new OnWaitlistLoadedCallback() {
        @Override
        public void onSuccess(List<WaitlistEntry> entries) {
            if (entries.isEmpty()) {
                callback.onEmpty();
                return;
            }
            // Keep simple: earliest requestedAt wins. Firestore index not required.
            WaitlistEntry next = entries.get(0);
            for (WaitlistEntry entry : entries) {
                if (String.valueOf(entry.getRequestedAt())
                        .compareTo(String.valueOf(next.getRequestedAt())) < 0) {
                    next = entry;
                }
            }
            callback.onSuccess(next);
        }

        @Override
        public void onFailure(Exception e) { callback.onFailure(e); }
    });
}

public void markOffered(String entryId, String slotId, OnWaitlistSimpleCallback callback) {
    waitlistCollection.document(entryId)
        .update("status", WaitlistEntry.STATUS_OFFERED,
                "offeredSlotId", slotId,
                "offeredAt", com.google.firebase.Timestamp.now())
        .addOnSuccessListener(unused -> callback.onSuccess())
        .addOnFailureListener(callback::onFailure);
}
```

### 5.4 `AppointmentRepository.cancelAppointment()` Hardening

After slot is restored to available, call into `WaitlistRepository.getNextActiveEntry()` and mark first entry `OFFERED`.

Do not auto-book the student. Offer only. This avoids booking a student without consent.

```text
cancelAppointment()
  ├── appointment.status = CANCELLED
  ├── slot.available = true
  ├── WaitlistRepository.getNextActiveEntry(counselorId)
  └── markOffered(entryId, slotId)
```

If waitlist offer fails, cancellation should still succeed.

---

## 6. Calendar Sync Helper

### 6.1 `CalendarSyncHelper.java`

```java
package com.example.moogerscouncil;

import android.content.Context;
import android.content.Intent;
import android.provider.CalendarContract;

/** Helper for prototype-level external calendar integration. */
public final class CalendarSyncHelper {

    private CalendarSyncHelper() {}

    public static Intent buildInsertEventIntent(Appointment appointment, String counselorName) {
        Intent intent = new Intent(Intent.ACTION_INSERT);
        intent.setData(CalendarContract.Events.CONTENT_URI);
        intent.putExtra(CalendarContract.Events.TITLE,
            "BetterCAPS session with " + counselorName);
        intent.putExtra(CalendarContract.Events.DESCRIPTION,
            "Counseling appointment booked through BetterCAPS.");
        intent.putExtra(CalendarContract.Events.EVENT_LOCATION, "LUMS CAPS");
        return intent;
    }

    public static String buildIcsLine(Appointment appointment, String counselorName) {
        return "BEGIN:VEVENT\n" +
            "SUMMARY:BetterCAPS session with " + counselorName + "\n" +
            "DESCRIPTION:Counseling appointment booked through BetterCAPS\n" +
            "END:VEVENT";
    }
}
```

Use this from `CounselorDashboardActivity` appointment card or dashboard export action.

---

## 7. UI Layer

### 7.1 `AvailabilitySettingsActivity.java`

Screen sections:

```text
Availability Settings
├── Buffer Time
│   ├── 0 minutes
│   ├── 10 minutes
│   ├── 15 minutes
│   └── 30 minutes
├── Calendar Export
│   ├── Enable device calendar export
│   └── Provider dropdown: Device / Google / Outlook
└── Save Settings
```

### 7.2 `AvailabilitySetupActivity.java`

When counselor adds a slot:

```
DatePicker → TimePicker → load AvailabilitySettings → canAddSlotWithBuffer()
  ├── available → addSlot()
  └── conflict → show reason and do not add slot
```

### 7.3 `BookingActivity.java`

When loading slots:

1. Load counselor settings.
2. Load available slots.
3. Remove slots that violate buffer around already booked appointments if necessary.
4. If list empty, show waitlist CTA.

### 7.4 `StudentHomeActivity.java`

If waitlist offers are included immediately:

```text
A slot opened with Dr. ___
Date/time
[Accept Slot] [Decline]
```

Accept can call existing `AppointmentRepository.bookAppointment()` using the offered slot and then mark waitlist `BOOKED`.

---

## 8. Strings to Add

```xml
<string name="availability_settings_title">Availability Settings</string>
<string name="buffer_time_title">Buffer time between sessions</string>
<string name="buffer_0">No buffer</string>
<string name="buffer_10">10 minutes</string>
<string name="buffer_15">15 minutes</string>
<string name="buffer_30">30 minutes</string>
<string name="slot_conflicts_buffer">This slot is too close to another session.</string>
<string name="settings_saved">Settings saved.</string>
<string name="settings_save_error">Could not save settings.</string>
<string name="calendar_export_title">Calendar Export</string>
<string name="export_to_calendar">Export to Calendar</string>
<string name="calendar_export_unavailable">No calendar app found.</string>
<string name="waitlist_slot_offered">A slot has opened for you.</string>
<string name="accept_slot">Accept Slot</string>
<string name="decline_slot">Decline</string>
```

---

## 9. Testing Requirements

### 9.1 Unit Tests

#### `AvailabilitySettingsTest.java`

1. Empty constructor defaults buffer to 0.
2. Constructor stores counselor ID and buffer.
3. Calendar provider constants match expected strings.

#### `BufferTimeValidatorTest.java`

1. Slot 10 minutes after existing session is rejected with 15-min buffer.
2. Slot outside buffer is accepted.
3. Different dates do not conflict.
4. Zero buffer allows adjacent valid slots if session-length rule permits.

#### `CalendarSyncHelperTest.java`

1. Insert intent has `ACTION_INSERT`.
2. ICS string contains `BEGIN:VEVENT` and `SUMMARY`.

### 9.2 UI Tests

#### `AvailabilitySettingsFlowTest.java`

1. Settings screen opens from dashboard/profile edit.
2. Buffer options visible.
3. Save shows success message.

#### `BufferBookingFlowTest.java`

1. Close conflicting slot does not appear or cannot be added.
2. Empty slot state shows Join Waitlist.

---

## 10. Definition of Done

Sprint 7 is done only when:

- Counselors can save buffer time in `availabilitySettings/{counselorId}`.
- `AvailabilitySetupActivity` blocks slots that violate buffer/session spacing.
- `BookingActivity` handles no-slot state with waitlist CTA.
- Cancellation restores slot and offers it to the next waitlisted student if one exists.
- Calendar export uses Android calendar intent or ICS helper.
- No external OAuth credentials are required for the app to compile/run.
- All new user-facing strings are in `strings.xml`.
- All new models have Javadoc.
- Unit and UI tests are added.

---

## 11. Deliverables Checklist

| Deliverable | Status |
|---|---|
| `AvailabilitySettings.java` | Todo |
| `AvailabilitySettingsRepository.java` | Todo |
| `CalendarSyncHelper.java` | Todo |
| `WaitlistOffer.java` | Todo |
| `AvailabilitySettingsActivity.java` | Todo |
| `AvailabilityRepository.java` buffer methods | Todo |
| `AvailabilitySetupActivity.java` buffer validation | Todo |
| `AppointmentRepository.java` waitlist offer hook | Todo |
| `BookingActivity.java` empty-slot/waitlist hardening | Todo |
| `CounselorDashboardActivity.java` calendar export entry | Todo |
| `strings.xml` additions | Todo |
| Unit tests | Todo |
| UI tests | Todo |
