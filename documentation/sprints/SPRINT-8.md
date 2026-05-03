# Sprint 8 — Waitlist Hardening: Preference-Based Requests, Counselor Resolution, and Auto-Booking
### Full Waitlist Lifecycle Sprint

---

## 0. Pre-Sprint Status Report

### Sprint 7 Review — Scheduling Hardening: COMPLETE

Sprint 7 completed buffer time, calendar export, and the basic waitlist plumbing (join + offer on cancellation). The current waitlist implementation is **structurally present but functionally minimal**:

#### Current Waitlist State — Student Side

| Component | Current State | What's Missing |
|---|---|---|
| `CounselorProfileActivity.java` | "Join Waitlist" button shown when counselor has no slots or is on-leave. Creates a `WaitlistEntry` with a generic reason string ("No slots available" / "Preferred counselor"). | No date/time preference input — student cannot express *when* they want an appointment. |
| `BookingActivity.java` | No-slot empty state shows "Join Waitlist" button. Same generic entry creation. | Same — no scheduling preference captured. |
| `StudentHomeActivity.java` | A `cardWaitlistStatus` card shows "You are on X waitlists" or "No active waitlists". | Only shows a count. No list of individual entries, no resolved/historical entries, no detail view. |
| `WaitlistEntry.java` | Model: id, studentId, counselorId, assessmentId, reason, status, requestedAt, offeredSlotId, offeredAt. | No `preferredDates`, `preferredStartTime`, `preferredEndTime` fields. No note field. |

#### Current Waitlist State — Counselor Side

| Component | Current State | What's Missing |
|---|---|---|
| `CounselorDashboardActivity.java` | A `waitlistCount` stat card shows the number of active waitlist entries. | No list view, no individual entry details, no student preference info, no route to resolve. |
| `WaitlistRepository.java` | `getActiveWaitlistForCounselor()`, `getActiveWaitlistCountForCounselor()`, `getNextActiveEntry()`, `markOffered()`, `markBooked()`. | No method to resolve an entry by matching against a newly-created slot. No preference-matching logic. |
| `AppointmentRepository.java` | `cancelAppointment()` triggers `getNextActiveEntry()` → `markOffered()`. | The auto-offer on cancel is a legacy mechanism that will be replaced by the new preference-matching auto-resolution. |

#### Current Waitlist State — Data Model

| Field | Present | Required for Sprint 8 |
|---|---|---|
| `id` | Yes | Keep |
| `studentId` | Yes | Keep |
| `counselorId` | Yes | Keep |
| `assessmentId` | Yes | Keep (optional link to intake quiz) |
| `reason` | Yes | Keep as a student note/comment field |
| `status` | Yes (ACTIVE/OFFERED/BOOKED/CANCELLED/EXPIRED) | Simplify to ACTIVE/RESOLVED/CANCELLED |
| `requestedAt` | Yes | Keep — **FIFO ordering key** |
| `offeredSlotId` | Yes | Repurpose → `resolvedSlotId` (the slot that was booked) |
| `offeredAt` | Yes | Repurpose → `resolvedAt` (when the entry was resolved) |
| `preferredDates` | **No** | **Add**: `List<String>` of ISO dates ("2026-05-10", "2026-05-12") |
| `preferredStartTime` | **No** | **Add**: `String` — "HH:mm" start of preferred window |
| `preferredEndTime` | **No** | **Add**: `String` — "HH:mm" end of preferred window |
| `note` | **No** | **Add**: `String` — free-text note from student to counselor |
| `resolvedAppointmentId` | **No** | **Add**: `String` — the appointment ID created on resolution |

---

## 1. Sprint 8 Objective

By the end of this sprint:

1. **A student can submit a waitlist request with scheduling preferences** — selecting preferred dates on a calendar and specifying a start/end time window via dropdowns, plus an optional note to the counselor.
2. **Waitlist requests are only allowed when no available slots already match the student's specified preferences** — the app checks for matching existing availability before allowing the waitlist submission.
3. **A student can view all their waitlist applications (active and resolved) in a dedicated tab** that shows status, counselor name, preferred dates/times, and resolution details when applicable.
4. **A counselor can view their waitlist queue** with full detail — student name, preferred dates, time windows, notes, and FIFO ordering.
5. **From the waitlist tab, a counselor is routed to the slot-creation screen** pre-informed by the student's preference. When the counselor creates a slot that falls within *any* active waitlist entry's preferred dates + time window, **that entry is automatically resolved and the appointment is atomically booked for the student** — in FIFO order if multiple entries match.
6. **When a counselor creates any general slot** (not necessarily from the waitlist tab), the system checks all active waitlist entries for that counselor and auto-resolves the earliest matching one.
7. **Race conditions are prevented** by resolving entries strictly in `requestedAt` order and using Firestore transactions for the resolution + booking atomic operation.

### Why This Sprint

The existing waitlist is a dead-end — students join but nothing actionable happens unless a cancellation triggers an offer (which the student cannot even accept from the UI). The entire workflow needs to be rebuilt as a **request → preference → counselor action → auto-resolve** pipeline that actually completes the loop.

---

## 2. What Already Exists vs. What Sprint 8 Builds

| Component | Already Exists | Sprint 8 Replaces/Extends |
|---|---|---|
| `WaitlistEntry.java` | Basic model (no preferences) | Add preferredDates, preferredStartTime, preferredEndTime, note, resolvedSlotId, resolvedAppointmentId, resolvedAt. Simplify statuses to ACTIVE/RESOLVED/CANCELLED. |
| `WaitlistRepository.java` | joinWaitlist (dedup), getActive, getNextActive, markOffered, markBooked | Replace markOffered/markBooked with `resolveEntry()`. Add `getMatchingActiveEntries(counselorId, date, time)`. Add `getAllWaitlistForStudent()` (active + resolved). Add `getActiveWaitlistForCounselorOrdered()`. Add `cancelEntry()`. |
| `CounselorProfileActivity.java` | "Join Waitlist" button with no preferences | Replace with navigation to `WaitlistRequestActivity` |
| `BookingActivity.java` | "Join Waitlist" button with no preferences | Replace with navigation to `WaitlistRequestActivity` |
| `StudentHomeActivity.java` | Count card only | Replace card with tap → `StudentWaitlistActivity` |
| `CounselorDashboardActivity.java` | Count stat card only | Replace/add tap → `CounselorWaitlistActivity` |
| `AvailabilitySetupActivity.java` | Add/delete slots with buffer validation | After adding a slot, call waitlist matching to auto-resolve |
| `AppointmentRepository.java` | cancelAppointment triggers legacy offer flow | Remove legacy offer flow; slot availability is handled by new matching on creation |
| `dialog_waitlist_offer.xml` | Minimal stub (title text only) | Replace with proper request form |
| `WaitlistOffer.java` | Offer model (unused effectively) | Remove or deprecate — no longer needed |

---

## 3. Files to Create or Modify

### 3.1 New Files

```text
src/main/java/com/example/moogerscouncil/
├── WaitlistRequestActivity.java         // UI — student fills preferences (calendar + time dropdowns + note)
├── StudentWaitlistActivity.java         // UI — student views active + resolved waitlist entries
├── CounselorWaitlistActivity.java       // UI — counselor views waitlist queue with preferences
├── WaitlistAdapter.java                 // Adapter — renders waitlist entries (student side)
├── CounselorWaitlistAdapter.java        // Adapter — renders waitlist entries (counselor side)
├── WaitlistMatcher.java                 // Helper — determines if a slot falls within an entry's preferences

src/main/res/layout/
├── activity_waitlist_request.xml        // Calendar + time dropdowns + note + submit
├── activity_student_waitlist.xml        // RecyclerView of student's waitlist entries
├── activity_counselor_waitlist.xml      // RecyclerView of counselor's waitlist queue
├── item_waitlist_student.xml            // Student-side entry row (counselor name, dates, status)
├── item_waitlist_counselor.xml          // Counselor-side entry row (student name, preferences, resolve CTA)

src/test/java/com/example/moogerscouncil/
├── WaitlistMatcherTest.java             // Unit test — slot-within-preferences logic
├── WaitlistEntryPreferencesTest.java    // Unit test — new fields, validation

src/androidTest/java/com/example/moogerscouncil/
├── WaitlistRequestFlowTest.java         // UI test — student submits request with preferences
├── WaitlistResolutionFlowTest.java      // UI test — counselor creates slot and entry auto-resolves
├── WaitlistStudentViewTest.java         // UI test — student sees active + resolved entries
├── WaitlistCounselorViewTest.java       // UI test — counselor views queue and resolves
```

### 3.2 Files to Modify

```text
WaitlistEntry.java                  // Add preferredDates, time window, note, resolved fields; simplify statuses
WaitlistRepository.java             // Major overhaul — new matching methods, resolution logic, ordered queries
AvailabilitySetupActivity.java      // After slot creation success, call WaitlistMatcher → auto-resolve
BookingActivity.java                // Replace joinWaitlist inline button with route to WaitlistRequestActivity
CounselorProfileActivity.java       // Replace joinWaitlist inline button with route to WaitlistRequestActivity
StudentHomeActivity.java            // Replace count card with tap → StudentWaitlistActivity
CounselorDashboardActivity.java     // Replace/extend waitlist count to tap → CounselorWaitlistActivity
AppointmentRepository.java          // Remove legacy waitlist offer on cancel; keep slot restoration only
activity_student_home.xml           // Update waitlist card to be tappable with navigation hint
activity_counselor_dashboard.xml    // Add waitlist queue button/card
strings.xml                         // All new user-facing strings
AndroidManifest.xml                 // Register WaitlistRequestActivity, StudentWaitlistActivity, CounselorWaitlistActivity
```

---

## 4. Firestore Data Model

### 4.1 Collection: `waitlist` (Updated Schema)

```text
waitlist/{entryId}
  ├── id: String
  ├── studentId: String
  ├── counselorId: String
  ├── assessmentId: String              // nullable — link to intake quiz if triggered from there
  ├── note: String                      // student's free-text note to the counselor
  ├── preferredDates: List<String>      // ["2026-05-10", "2026-05-12", "2026-05-14"]
  ├── preferredStartTime: String        // "09:00" — start of daily time window
  ├── preferredEndTime: String          // "14:00" — end of daily time window
  ├── status: String                    // "ACTIVE" | "RESOLVED" | "CANCELLED"
  ├── requestedAt: Timestamp            // FIFO ordering key
  ├── resolvedSlotId: String            // nullable — filled on resolution
  ├── resolvedAppointmentId: String     // nullable — filled on resolution
  └── resolvedAt: Timestamp             // nullable — filled on resolution
```

**Key design decisions:**

- `preferredDates` is a list — the student picks multiple dates they're available on.
- `preferredStartTime` and `preferredEndTime` apply to ALL selected dates uniformly. A student who wants 9–12 on Monday and 14–17 on Wednesday must submit two separate waitlist entries. This keeps the model simple and avoids a nested map of date → time ranges.
- `status` is simplified to three states: ACTIVE (pending), RESOLVED (booked), CANCELLED (withdrawn by student or expired). The previous OFFERED/BOOKED/EXPIRED states are removed because the new flow is **instant resolution** — there is no "offer and wait for acceptance" step.
- `resolvedSlotId` + `resolvedAppointmentId` together record exactly what happened when the entry was resolved. This replaces the old `offeredSlotId`/`offeredAt` fields.

### 4.2 Matching Rule

A slot **matches** a waitlist entry if and only if:

```
slot.date ∈ entry.preferredDates
AND slot.time >= entry.preferredStartTime
AND slot.time < entry.preferredEndTime
```

When multiple entries match a single new slot, resolution order is strictly by `requestedAt` ascending (first-come, first-served). Only the **first** matching entry is resolved per slot — one slot, one booking.

### 4.3 Guard: No Waitlist If Slots Exist

Before allowing a waitlist submission, the app must check:

```
For each date in preferredDates:
    Are there any available slots where:
        slot.counselorId == counselorId
        AND slot.date == date
        AND slot.time >= preferredStartTime
        AND slot.time < preferredEndTime
        AND slot.available == true
```

If **any** matching available slot exists across any of the preferred dates, the waitlist submission is blocked with a message: "Slots matching your preferences are available — please book directly."

---

## 5. Implementation Details — Model Layer

### 5.1 `WaitlistEntry.java` — Updated

```java
package com.example.moogerscouncil;

import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a student's waitlist request for a specific counselor.
 * Stored in the Firestore 'waitlist' collection.
 *
 * A waitlist entry captures the student's preferred dates and time window.
 * Resolution happens automatically when the counselor creates a slot
 * that falls within the entry's preferences. Entries are resolved
 * in requestedAt order (FIFO) to prevent race conditions.
 */
public class WaitlistEntry {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_RESOLVED = "RESOLVED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    private String id;
    private String studentId;
    private String counselorId;
    private String assessmentId;
    private String note;
    private List<String> preferredDates;
    private String preferredStartTime;
    private String preferredEndTime;
    private String status;
    private Timestamp requestedAt;
    private String resolvedSlotId;
    private String resolvedAppointmentId;
    private Timestamp resolvedAt;

    /** Required empty constructor for Firestore. */
    public WaitlistEntry() {
        status = STATUS_ACTIVE;
        preferredDates = new ArrayList<>();
    }

    /**
     * Creates an active waitlist entry with scheduling preferences.
     *
     * @param studentId     Firebase Auth UID of the requesting student.
     * @param counselorId   Counselor identifier the student wants to see.
     * @param preferredDates List of ISO date strings the student is available on.
     * @param startTime     Start of preferred time window ("HH:mm").
     * @param endTime       End of preferred time window ("HH:mm").
     * @param note          Optional free-text note from the student.
     * @param assessmentId  Optional intake assessment ID.
     */
    public WaitlistEntry(String studentId, String counselorId,
                         List<String> preferredDates, String startTime,
                         String endTime, String note, String assessmentId) {
        this.studentId = studentId;
        this.counselorId = counselorId;
        this.preferredDates = preferredDates != null ? preferredDates : new ArrayList<>();
        this.preferredStartTime = startTime;
        this.preferredEndTime = endTime;
        this.note = note;
        this.assessmentId = assessmentId;
        this.status = STATUS_ACTIVE;
        this.requestedAt = Timestamp.now();
    }

    // --- Getters ---
    public String getId() { return id; }
    public String getStudentId() { return studentId; }
    public String getCounselorId() { return counselorId; }
    public String getAssessmentId() { return assessmentId; }
    public String getNote() { return note; }
    public List<String> getPreferredDates() { return preferredDates; }
    public String getPreferredStartTime() { return preferredStartTime; }
    public String getPreferredEndTime() { return preferredEndTime; }
    public String getStatus() { return status; }
    public Timestamp getRequestedAt() { return requestedAt; }
    public String getResolvedSlotId() { return resolvedSlotId; }
    public String getResolvedAppointmentId() { return resolvedAppointmentId; }
    public Timestamp getResolvedAt() { return resolvedAt; }

    // --- Setters ---
    public void setId(String id) { this.id = id; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public void setCounselorId(String counselorId) { this.counselorId = counselorId; }
    public void setAssessmentId(String assessmentId) { this.assessmentId = assessmentId; }
    public void setNote(String note) { this.note = note; }
    public void setPreferredDates(List<String> preferredDates) { this.preferredDates = preferredDates; }
    public void setPreferredStartTime(String preferredStartTime) { this.preferredStartTime = preferredStartTime; }
    public void setPreferredEndTime(String preferredEndTime) { this.preferredEndTime = preferredEndTime; }
    public void setStatus(String status) { this.status = status; }
    public void setRequestedAt(Timestamp requestedAt) { this.requestedAt = requestedAt; }
    public void setResolvedSlotId(String resolvedSlotId) { this.resolvedSlotId = resolvedSlotId; }
    public void setResolvedAppointmentId(String resolvedAppointmentId) { this.resolvedAppointmentId = resolvedAppointmentId; }
    public void setResolvedAt(Timestamp resolvedAt) { this.resolvedAt = resolvedAt; }
}
```

### 5.2 `WaitlistMatcher.java` — New Helper

```java
package com.example.moogerscouncil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Determines which waitlist entries match a newly-created slot.
 * Used by AvailabilitySetupActivity after slot creation to auto-resolve entries.
 *
 * Matching rule: slot.date ∈ entry.preferredDates
 *   AND slot.time >= entry.preferredStartTime
 *   AND slot.time < entry.preferredEndTime
 *
 * When multiple entries match, the earliest requestedAt wins (FIFO).
 */
public final class WaitlistMatcher {

    private WaitlistMatcher() {}

    /**
     * Determines if a slot falls within an entry's preferences.
     *
     * @param entry The waitlist entry to check against.
     * @param slotDate The date of the new slot ("yyyy-MM-dd").
     * @param slotTime The time of the new slot ("HH:mm").
     * @return true if the slot matches the entry's preferences.
     */
    public static boolean matches(WaitlistEntry entry, String slotDate, String slotTime) {
        if (entry == null || slotDate == null || slotTime == null) return false;
        if (entry.getPreferredDates() == null || entry.getPreferredDates().isEmpty()) return false;
        if (!entry.getPreferredDates().contains(slotDate)) return false;

        String startTime = entry.getPreferredStartTime();
        String endTime = entry.getPreferredEndTime();
        if (startTime == null || endTime == null) return false;

        // slot.time >= startTime AND slot.time < endTime
        return slotTime.compareTo(startTime) >= 0 && slotTime.compareTo(endTime) < 0;
    }

    /**
     * Finds the first matching entry from a list, ordered by requestedAt (FIFO).
     *
     * @param entries Active waitlist entries for the counselor.
     * @param slotDate The date of the new slot.
     * @param slotTime The time of the new slot.
     * @return The earliest matching entry, or null if no match.
     */
    public static WaitlistEntry findFirstMatch(List<WaitlistEntry> entries,
                                                String slotDate, String slotTime) {
        if (entries == null || entries.isEmpty()) return null;

        List<WaitlistEntry> sorted = new ArrayList<>(entries);
        Collections.sort(sorted, (a, b) ->
                String.valueOf(a.getRequestedAt()).compareTo(String.valueOf(b.getRequestedAt())));

        for (WaitlistEntry entry : sorted) {
            if (WaitlistEntry.STATUS_ACTIVE.equals(entry.getStatus()) && matches(entry, slotDate, slotTime)) {
                return entry;
            }
        }
        return null;
    }

    /**
     * Checks if any available slots already cover the requested preferences.
     * Used as a guard before allowing waitlist submission.
     *
     * @param availableSlots All available slots for the counselor.
     * @param preferredDates Dates the student wants.
     * @param startTime Start of preferred window.
     * @param endTime End of preferred window.
     * @return true if at least one existing slot covers the preferences (waitlist should be blocked).
     */
    public static boolean existingSlotsMatchPreferences(List<TimeSlot> availableSlots,
                                                        List<String> preferredDates,
                                                        String startTime, String endTime) {
        if (availableSlots == null || preferredDates == null) return false;
        for (TimeSlot slot : availableSlots) {
            if (!slot.isAvailable()) continue;
            if (!preferredDates.contains(slot.getDate())) continue;
            if (slot.getTime().compareTo(startTime) >= 0 && slot.getTime().compareTo(endTime) < 0) {
                return true;
            }
        }
        return false;
    }
}
```

---

## 6. Implementation Details — Repository Layer

### 6.1 `WaitlistRepository.java` — Major Overhaul

The repository retains its collection references and `joinWaitlist()` dedup logic, but the method signatures change significantly:

#### New/Changed Methods

| Method | Purpose |
|---|---|
| `joinWaitlist(entry, callback)` | **Modified.** Same dedup check (student + counselor + ACTIVE), but now stores preference fields. |
| `getAllWaitlistForStudent(studentId, callback)` | **New.** Fetches ALL entries (active + resolved + cancelled) for the student's list view. |
| `getActiveWaitlistForCounselorOrdered(counselorId, callback)` | **New.** Returns ACTIVE entries sorted by `requestedAt` ascending. |
| `resolveEntry(entryId, slotId, appointmentId, callback)` | **New.** Atomically sets status=RESOLVED, resolvedSlotId, resolvedAppointmentId, resolvedAt. |
| `cancelEntry(entryId, callback)` | **New.** Sets status=CANCELLED. Used when student withdraws or entry expires. |
| `getActiveWaitlistCountForCounselor(counselorId, callback)` | **Unchanged.** Still works for dashboard count. |

#### Removed Methods

| Method | Reason |
|---|---|
| `markOffered()` | No more offer step — resolution is instant. |
| `markBooked()` | Replaced by `resolveEntry()`. |
| `getNextActiveEntry()` | Replaced by `WaitlistMatcher.findFirstMatch()` operating on the full ordered list. |

#### `resolveEntry()` Implementation

```java
/**
 * Atomically resolves a waitlist entry — marks it RESOLVED and records the
 * slot and appointment that fulfilled it.
 *
 * @param entryId The waitlist entry to resolve.
 * @param slotId The slot that was created/matched.
 * @param appointmentId The appointment booked for the student.
 * @param callback Success/failure callback.
 */
public void resolveEntry(String entryId, String slotId, String appointmentId,
                         OnWaitlistSimpleCallback callback) {
    java.util.Map<String, Object> updates = new java.util.HashMap<>();
    updates.put("status", WaitlistEntry.STATUS_RESOLVED);
    updates.put("resolvedSlotId", slotId);
    updates.put("resolvedAppointmentId", appointmentId);
    updates.put("resolvedAt", com.google.firebase.Timestamp.now());

    waitlistCollection.document(entryId)
            .update(updates)
            .addOnSuccessListener(unused -> callback.onSuccess())
            .addOnFailureListener(callback::onFailure);
}
```

### 6.2 Auto-Resolution Flow (in `AvailabilitySetupActivity`)

After a slot is successfully created via `AvailabilityRepository.addSlot()`:

```java
// Slot created successfully — now check for waitlist matches
waitlistRepository.getActiveWaitlistForCounselorOrdered(counselorId,
        new WaitlistRepository.OnWaitlistLoadedCallback() {
    @Override
    public void onSuccess(List<WaitlistEntry> entries) {
        WaitlistEntry match = WaitlistMatcher.findFirstMatch(entries, slotDate, slotTime);
        if (match == null) return; // No match — slot remains available for regular booking

        // Auto-book: create appointment atomically for the matched student
        appointmentRepository.bookAppointmentForWaitlist(
                match.getStudentId(), counselorId, slot,
                new AppointmentRepository.OnBookingCallback() {
            @Override
            public void onSuccess(String appointmentId) {
                waitlistRepository.resolveEntry(match.getId(), slot.getId(), appointmentId,
                        new WaitlistRepository.OnWaitlistSimpleCallback() {
                    @Override public void onSuccess() {
                        Toast.makeText(context,
                            getString(R.string.waitlist_auto_resolved, match.getStudentId()),
                            Toast.LENGTH_LONG).show();
                        loadSlots(); // refresh list
                    }
                    @Override public void onFailure(Exception e) { /* log */ }
                });
            }

            @Override
            public void onSlotTaken() { /* Race condition — slot taken between check and book */ }

            @Override
            public void onFailure(Exception e) { /* log error */ }
        });
    }

    @Override
    public void onFailure(Exception e) { /* Non-critical — slot was still created */ }
});
```

### 6.3 `AppointmentRepository.java` — New Helper

Add `bookAppointmentForWaitlist()` — identical to `bookAppointment()` but takes a `studentId` parameter directly (the booking is on behalf of the waitlisted student, not the current user):

```java
/**
 * Books an appointment on behalf of a waitlisted student.
 * Uses the same atomic Firestore transaction as regular booking.
 *
 * @param studentId The student whose waitlist entry is being resolved.
 * @param counselorId The counselor.
 * @param slot The slot to book.
 * @param callback Three-way result callback.
 */
public void bookAppointmentForWaitlist(String studentId, String counselorId,
                                        TimeSlot slot, OnBookingCallback callback) {
    // Same transaction logic as bookAppointment():
    // 1. Read slot — check available == true
    // 2. Write slot.available = false
    // 3. Write new appointment doc {studentId, counselorId, slotId, date, time, "CONFIRMED"}
    // Return appointmentId on success
}
```

### 6.4 `AppointmentRepository.java` — Legacy Removal

The existing `cancelAppointment()` method currently contains waitlist offer logic after slot restoration:

```java
// REMOVE THIS BLOCK from cancelAppointment():
WaitlistRepository waitlistRepository = new WaitlistRepository();
waitlistRepository.getNextActiveEntry(counselorId, ...);
    // → markOffered(...)
```

Replace with: **nothing.** Slot restoration is sufficient. The next time the counselor opens the waitlist tab or adds any slot, matching runs naturally. The old "offer on cancel" flow is superseded.

---

## 7. Implementation Details — UI Layer

### 7.1 `WaitlistRequestActivity.java` — Student Preference Form

**Purpose:** The student fills out their scheduling preferences before submitting a waitlist request.

**Screen layout (`activity_waitlist_request.xml`):**

```text
┌─────────────────────────────────────────────┐
│ ← Back          Request Appointment         │
├─────────────────────────────────────────────┤
│                                             │
│  Counselor: Dr. Baz Shahbaz                 │
│                                             │
│  Select your preferred dates:               │
│  ┌─────────────────────────────────────┐    │
│  │         [Calendar View]             │    │
│  │   Multi-select dates (tap to toggle)│    │
│  └─────────────────────────────────────┘    │
│                                             │
│  Selected: May 10, May 12, May 14           │
│                                             │
│  Preferred time window:                     │
│  ┌──────────────┐  ┌──────────────┐        │
│  │ Start: 09:00 ▼│  │ End: 14:00  ▼│       │
│  └──────────────┘  └──────────────┘        │
│                                             │
│  Note to counselor (optional):              │
│  ┌─────────────────────────────────────┐    │
│  │                                     │    │
│  └─────────────────────────────────────┘    │
│                                             │
│  ┌─────────────────────────────────────┐    │
│  │       Submit Waitlist Request       │    │
│  └─────────────────────────────────────┘    │
│                                             │
└─────────────────────────────────────────────┘
```

**Data flow:**

```
Student taps "Join Waitlist" (from CounselorProfileActivity or BookingActivity)
    → Intent with COUNSELOR_ID + SLOT_COUNSELOR_ID + optional ASSESSMENT_ID
    → WaitlistRequestActivity opens

Student selects dates on calendar (multi-select mode)
    → selectedDates list populated

Student picks start time and end time from Spinner/dropdown
    → Time options: 08:00, 08:30, 09:00, ... , 18:00 (30-min intervals)

Student types optional note

Student taps "Submit Waitlist Request"
    → GUARD CHECK: AvailabilityRepository.getAvailableSlotsForCounselor(slotCounselorId)
        → WaitlistMatcher.existingSlotsMatchPreferences(slots, selectedDates, startTime, endTime)
            → If TRUE: show error "Matching slots exist — please book directly"
            → If FALSE: proceed to submit

    → DEDUP CHECK: WaitlistRepository.joinWaitlist(entry, callback)
        → onAlreadyWaitlisted(): show "You already have an active request for this counselor"
        → onSuccess(): Toast + finish() → return to previous screen
```

**Validation rules:**
- At least one date must be selected.
- End time must be after start time.
- End time - start time must be at least 30 minutes (one possible slot).
- The calendar only allows future dates.

### 7.2 `StudentWaitlistActivity.java` — Student's Waitlist Tab

**Purpose:** Shows all of the student's waitlist entries — active ones at the top, resolved/cancelled below.

**Screen layout (`activity_student_waitlist.xml`):**

```text
┌─────────────────────────────────────────────┐
│ ← Back          My Waitlist Requests        │
├─────────────────────────────────────────────┤
│                                             │
│  ┌─ Active Requests ───────────────────┐    │
│  │ Dr. Baz • May 10, 12 • 09:00–14:00 │    │
│  │ Status: Waiting       [Cancel]      │    │
│  └─────────────────────────────────────┘    │
│                                             │
│  ┌─ Resolved ─────────────────────────┐    │
│  │ Dr. Baz • May 8 • 10:00–12:00      │    │
│  │ Status: Booked ✓  Slot: May 8 10:30│    │
│  └─────────────────────────────────────┘    │
│                                             │
│  ┌─ Cancelled ────────────────────────┐    │
│  │ Dr. Baz • May 5, 6 • 14:00–17:00   │    │
│  │ Status: Cancelled                   │    │
│  └─────────────────────────────────────┘    │
│                                             │
└─────────────────────────────────────────────┘
```

**Data flow:**

```
StudentWaitlistActivity.onCreate()
    → WaitlistRepository.getAllWaitlistForStudent(studentId, callback)
        → onSuccess(entries):
            → separate into active / resolved / cancelled
            → sort each group by requestedAt descending (newest first)
            → adapter.setData(all entries, grouped)
            → empty state if no entries at all

Cancel button on ACTIVE entries:
    → Confirm dialog: "Cancel this waitlist request?"
    → WaitlistRepository.cancelEntry(entryId, callback)
        → onSuccess: remove from list, refresh
```

### 7.3 `CounselorWaitlistActivity.java` — Counselor's Waitlist Queue

**Purpose:** Counselor views the waitlist queue for their profile, sees student names, preferences, notes. Can navigate to slot creation to resolve entries.

**Screen layout (`activity_counselor_waitlist.xml`):**

```text
┌─────────────────────────────────────────────┐
│ ← Back          Waitlist Queue (3)          │
├─────────────────────────────────────────────┤
│                                             │
│  ┌─────────────────────────────────────┐    │
│  │ #1  Student: Mujeeb Asad            │    │
│  │ Dates: May 10, May 12, May 14      │    │
│  │ Time: 09:00 – 14:00                │    │
│  │ Note: "Prefer mornings if possible" │    │
│  │ Requested: May 3, 2026             │    │
│  │                                     │    │
│  │ [Create Slot for This Student]      │    │
│  └─────────────────────────────────────┘    │
│                                             │
│  ┌─────────────────────────────────────┐    │
│  │ #2  Student: Hannan Mustafa         │    │
│  │ Dates: May 11, May 13              │    │
│  │ Time: 14:00 – 17:00                │    │
│  │ Note: —                             │    │
│  │ Requested: May 4, 2026             │    │
│  │                                     │    │
│  │ [Create Slot for This Student]      │    │
│  └─────────────────────────────────────┘    │
│                                             │
└─────────────────────────────────────────────┘
```

**Data flow:**

```
CounselorWaitlistActivity.onCreate()
    → WaitlistRepository.getActiveWaitlistForCounselorOrdered(counselorId, callback)
        → onSuccess(entries):
            → For each entry: UserRepository.getUserName(entry.getStudentId())
            → adapter.setData(entries)
            → empty state if no entries

"Create Slot for This Student" button tap:
    → Navigate to AvailabilitySetupActivity with intent extras:
        EXTRA_WAITLIST_ENTRY_ID = entry.getId()
        EXTRA_SUGGESTED_DATES = entry.getPreferredDates()  // for counselor reference
        EXTRA_SUGGESTED_START = entry.getPreferredStartTime()
        EXTRA_SUGGESTED_END = entry.getPreferredEndTime()
    → The counselor is taken to the general slot-making screen (AvailabilitySetupActivity)
    → They manually create a slot on one of the suggested dates within the window
    → On slot creation success, the auto-resolution hook fires and resolves the entry
```

### 7.4 `AvailabilitySetupActivity.java` — Auto-Resolution Hook

After `AvailabilityRepository.addSlot()` succeeds, append the matching logic:

```java
private void onSlotCreatedSuccess(TimeSlot createdSlot) {
    Toast.makeText(this, R.string.slot_added, Toast.LENGTH_SHORT).show();
    loadSlots(); // refresh slot list

    // Auto-resolve matching waitlist entries
    waitlistRepository.getActiveWaitlistForCounselorOrdered(counselorId,
            new WaitlistRepository.OnWaitlistLoadedCallback() {
        @Override
        public void onSuccess(List<WaitlistEntry> entries) {
            WaitlistEntry match = WaitlistMatcher.findFirstMatch(
                    entries, createdSlot.getDate(), createdSlot.getTime());
            if (match == null) return;

            appointmentRepository.bookAppointmentForWaitlist(
                    match.getStudentId(), counselorId, createdSlot,
                    new AppointmentRepository.OnBookingCallback() {
                @Override
                public void onSuccess(String appointmentId) {
                    waitlistRepository.resolveEntry(match.getId(),
                            createdSlot.getId(), appointmentId,
                            new WaitlistRepository.OnWaitlistSimpleCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(AvailabilitySetupActivity.this,
                                    getString(R.string.waitlist_auto_resolved_student),
                                    Toast.LENGTH_LONG).show();
                            loadSlots();
                        }
                        @Override
                        public void onFailure(Exception e) { /* log */ }
                    });
                }

                @Override public void onSlotTaken() { /* Shouldn't happen on fresh slot */ }
                @Override public void onFailure(Exception e) { /* log */ }
            });
        }

        @Override
        public void onFailure(Exception e) { /* Non-critical — slot was still created */ }
    });
}
```

### 7.5 Navigation Changes

| From | Action | Destination |
|---|---|---|
| `CounselorProfileActivity` | "Join Waitlist" button | `WaitlistRequestActivity` (with counselorId, slotCounselorId, assessmentId) |
| `BookingActivity` | "Join Waitlist" button (no-slot state) | `WaitlistRequestActivity` (with counselorId, slotCounselorId, assessmentId) |
| `StudentHomeActivity` | Tap waitlist status card | `StudentWaitlistActivity` |
| `CounselorDashboardActivity` | Tap waitlist count / queue button | `CounselorWaitlistActivity` |
| `CounselorWaitlistActivity` | "Create Slot for This Student" | `AvailabilitySetupActivity` (with suggested dates/times) |

---

## 8. Strings to Add

```xml
<!-- Waitlist Request Screen -->
<string name="waitlist_request_title">Request Appointment</string>
<string name="waitlist_select_dates">Select your preferred dates:</string>
<string name="waitlist_selected_dates">Selected: %s</string>
<string name="waitlist_preferred_time">Preferred time window:</string>
<string name="waitlist_start_time">Start time</string>
<string name="waitlist_end_time">End time</string>
<string name="waitlist_note_hint">Note to counselor (optional)</string>
<string name="waitlist_submit_request">Submit Waitlist Request</string>
<string name="waitlist_request_submitted">Waitlist request submitted.</string>
<string name="waitlist_slots_exist">Matching slots are available — please book directly.</string>
<string name="waitlist_already_active">You already have an active request for this counselor.</string>
<string name="waitlist_no_dates_selected">Please select at least one date.</string>
<string name="waitlist_invalid_time_window">End time must be after start time.</string>
<string name="waitlist_time_window_too_short">Time window must be at least 30 minutes.</string>

<!-- Student Waitlist Tab -->
<string name="student_waitlist_title">My Waitlist Requests</string>
<string name="waitlist_status_active">Waiting</string>
<string name="waitlist_status_resolved">Booked</string>
<string name="waitlist_status_cancelled">Cancelled</string>
<string name="waitlist_cancel_confirm">Cancel this waitlist request?</string>
<string name="waitlist_cancel_confirm_yes">Yes, cancel</string>
<string name="waitlist_cancelled_success">Request cancelled.</string>
<string name="waitlist_no_requests">No waitlist requests yet.</string>
<string name="waitlist_resolved_slot">Slot: %1$s at %2$s</string>

<!-- Counselor Waitlist Queue -->
<string name="counselor_waitlist_title">Waitlist Queue (%d)</string>
<string name="waitlist_queue_empty">No students are waitlisted.</string>
<string name="waitlist_create_slot_for_student">Create Slot for This Student</string>
<string name="waitlist_student_dates">Dates: %s</string>
<string name="waitlist_student_time_window">Time: %1$s – %2$s</string>
<string name="waitlist_student_note">Note: "%s"</string>
<string name="waitlist_requested_on">Requested: %s</string>

<!-- Auto-Resolution -->
<string name="waitlist_auto_resolved_student">Waitlist resolved — appointment booked automatically.</string>
<string name="waitlist_auto_resolved_detail">Slot on %1$s at %2$s booked for waitlisted student.</string>
```

---

## 9. Testing Requirements

### 9.1 Unit Tests

#### `WaitlistMatcherTest.java`

| Test | Assertion |
|---|---|
| `slotMatchesEntry_dateAndTimeWithinWindow_returnsTrue` | Slot on preferred date within time window → true |
| `slotDoesNotMatch_wrongDate_returnsFalse` | Slot on date not in preferredDates → false |
| `slotDoesNotMatch_beforeStartTime_returnsFalse` | Slot time before startTime → false |
| `slotDoesNotMatch_atEndTime_returnsFalse` | Slot time == endTime (exclusive boundary) → false |
| `slotMatchesEntry_atExactStartTime_returnsTrue` | Slot time == startTime (inclusive boundary) → true |
| `findFirstMatch_multipleEntries_returnsFIFO` | Entry with earlier requestedAt is returned first |
| `findFirstMatch_noMatches_returnsNull` | No entry matches → null |
| `findFirstMatch_emptyList_returnsNull` | Empty list → null |
| `existingSlotsMatch_slotAvailable_returnsTrue` | Available slot within preferences → true |
| `existingSlotsMatch_slotBooked_returnsFalse` | Booked slot within preferences → false (available=false) |
| `existingSlotsMatch_wrongDate_returnsFalse` | No slot on preferred dates → false |

#### `WaitlistEntryPreferencesTest.java`

| Test | Assertion |
|---|---|
| `emptyConstructor_defaultsToActive` | status == ACTIVE |
| `emptyConstructor_emptyPreferredDates` | preferredDates is empty list, not null |
| `fullConstructor_setsAllFields` | All preference fields populated |
| `resolvedFields_nullByDefault` | resolvedSlotId, resolvedAppointmentId, resolvedAt all null on creation |
| `statusConstants_correctValues` | ACTIVE/RESOLVED/CANCELLED |

### 9.2 UI Tests (Espresso)

#### `WaitlistRequestFlowTest.java`

| Test | Assertion |
|---|---|
| `requestScreen_displaysAllFormElements` | Calendar, spinners, note field, submit button visible |
| `submitWithoutDates_showsError` | No dates selected → validation error toast/message |
| `submitWithInvalidTimeWindow_showsError` | endTime <= startTime → validation error |
| `submitWhenSlotsExist_showsBlockMessage` | If available slots match preferences → "book directly" message |
| `validSubmission_createsEntryAndFinishes` | Valid form → success toast → activity finishes |

#### `WaitlistResolutionFlowTest.java`

| Test | Assertion |
|---|---|
| `slotCreationMatchingWaitlist_resolvesEntry` | Creating a slot within an active entry's window resolves it |
| `slotCreationNoMatch_doesNotResolve` | Slot outside all entries' windows → no resolution |
| `multipleMatchingEntries_earliestResolved` | FIFO — earliest requestedAt entry is the one resolved |

#### `WaitlistStudentViewTest.java`

| Test | Assertion |
|---|---|
| `studentWaitlist_displaysActiveAndResolved` | Both sections visible when entries exist |
| `activeEntry_showsCancelButton` | Cancel button present on ACTIVE entries |
| `resolvedEntry_showsBookedSlotInfo` | Resolved entries show slot date/time |
| `emptyState_showsNoRequestsMessage` | No entries → empty state message |

#### `WaitlistCounselorViewTest.java`

| Test | Assertion |
|---|---|
| `counselorQueue_displaysEntriesInOrder` | Entries shown by requestedAt ascending |
| `entryCard_showsStudentPreferences` | Dates, time window, note visible |
| `createSlotButton_navigatesToSetup` | Tap → AvailabilitySetupActivity opens |
| `emptyQueue_showsEmptyMessage` | No active entries → empty state |

---

## 10. Backward Compatibility

### 10.1 Existing `WaitlistEntry` Documents in Firestore

Old entries have: `reason`, `offeredSlotId`, `offeredAt` fields but no preference fields. These entries should render gracefully:

- **Student side:** Old entries without `preferredDates` show "No preference specified" in the dates column.
- **Counselor side:** Old entries without preferences show "Open availability — no specific dates requested."
- **Matching:** `WaitlistMatcher.matches()` returns `false` if `preferredDates` is null or empty — old entries are never auto-resolved. They remain until manually cancelled.

### 10.2 Removed Status Values

Old entries may have `status: "OFFERED"`, `"BOOKED"`, or `"EXPIRED"`. Treat these as:
- `OFFERED` → render as `RESOLVED` (it was in-progress under the old system)
- `BOOKED` → render as `RESOLVED`
- `EXPIRED` → render as `CANCELLED`

Handle this in the adapter display logic, not by migrating data.

---

## 11. Definition of Done

Sprint 8 is done only when:

- [ ] `WaitlistEntry.java` has preference fields and simplified statuses.
- [ ] `WaitlistMatcher.java` correctly identifies slot-entry matches.
- [ ] `WaitlistRepository.java` supports resolution, ordered queries, and student history.
- [ ] `WaitlistRequestActivity` lets students pick dates, time window, and note.
- [ ] Waitlist submission is **blocked** when matching available slots exist.
- [ ] `StudentWaitlistActivity` shows active + resolved + cancelled entries.
- [ ] Active entries can be cancelled by the student.
- [ ] `CounselorWaitlistActivity` shows FIFO-ordered queue with student preferences.
- [ ] "Create Slot for This Student" routes to `AvailabilitySetupActivity` with suggested info.
- [ ] `AvailabilitySetupActivity` auto-resolves the earliest matching waitlist entry after any slot creation.
- [ ] Auto-resolution atomically books the appointment for the waitlisted student.
- [ ] FIFO ordering is respected — no race conditions on who gets resolved.
- [ ] `AppointmentRepository` legacy offer-on-cancel flow is removed.
- [ ] All new screens registered in `AndroidManifest.xml`.
- [ ] All new strings in `strings.xml`.
- [ ] Unit tests pass for `WaitlistMatcher` and `WaitlistEntry`.
- [ ] UI tests pass for request flow, resolution flow, student view, and counselor view.
- [ ] Backward compatibility with old waitlist documents (graceful rendering, no crashes).
- [ ] The app still supports the full end-to-end flow from previous sprints.

---

## 12. Deliverables Checklist

| Deliverable | Type | Status |
|---|---|---|
| `WaitlistEntry.java` (updated) | Model | Todo |
| `WaitlistMatcher.java` | Helper | Todo |
| `WaitlistRepository.java` (overhauled) | Repository | Todo |
| `WaitlistRequestActivity.java` | Activity | Todo |
| `StudentWaitlistActivity.java` | Activity | Todo |
| `CounselorWaitlistActivity.java` | Activity | Todo |
| `WaitlistAdapter.java` | Adapter | Todo |
| `CounselorWaitlistAdapter.java` | Adapter | Todo |
| `AppointmentRepository.java` — `bookAppointmentForWaitlist()` + legacy removal | Repository | Todo |
| `AvailabilitySetupActivity.java` — auto-resolution hook | Activity | Todo |
| `CounselorProfileActivity.java` — route to request screen | Activity | Todo |
| `BookingActivity.java` — route to request screen | Activity | Todo |
| `StudentHomeActivity.java` — tap → student waitlist | Activity | Todo |
| `CounselorDashboardActivity.java` — tap → counselor waitlist | Activity | Todo |
| `activity_waitlist_request.xml` | Layout | Todo |
| `activity_student_waitlist.xml` | Layout | Todo |
| `activity_counselor_waitlist.xml` | Layout | Todo |
| `item_waitlist_student.xml` | Layout | Todo |
| `item_waitlist_counselor.xml` | Layout | Todo |
| `strings.xml` additions | Resources | Todo |
| `AndroidManifest.xml` updates | Config | Todo |
| `WaitlistMatcherTest.java` | Unit Test | Todo |
| `WaitlistEntryPreferencesTest.java` | Unit Test | Todo |
| `WaitlistRequestFlowTest.java` | UI Test | Todo |
| `WaitlistResolutionFlowTest.java` | UI Test | Todo |
| `WaitlistStudentViewTest.java` | UI Test | Todo |
| `WaitlistCounselorViewTest.java` | UI Test | Todo |
