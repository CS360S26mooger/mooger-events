# Sprint 11 — Messaging Thread Overhaul + Counselor UX Declutter + Crisis Escalation Hardening
### Architecture Fixes, Admin Visibility, and Session Management Redesign

---

## 0. Pre-Sprint Status Report

### Sprint 10 Review — Admin Reminders + Secure Messaging + Returning Student: COMPLETE

Sprint 10 delivered the first full implementation of admin-configurable reminders, appointment-linked secure messaging, and the returning student indicator on the counselor dashboard. The following is now in place:

| Deliverable | Status | Notes |
|---|---|---|
| `ReminderSettings.java` model | Done | `enabled24Hour`, `enabled1Hour` boolean flags; Firestore-safe empty constructor. |
| `ReminderRepository.java` | Done | `getSettings()`, `saveSettings()`, `createReminderRecords()` for upcoming confirmed appointments. |
| `ReminderScheduler.java` | Done | Collects confirmed appointments within 48h window; generates `Reminder` documents in batch. |
| `ReminderSettingsActivity.java` | Done | Admin screen for toggling 24h / 1h reminder switches; saves to `reminderSettings` Firestore doc. |
| `AdminDashboardActivity.java` | Done | Landing screen: reminder summary card, "Generate Reminder Records" button, reminder settings nav. |
| `SecureMessage.java` model | Done | `appointmentId`, `counselorId`, `studentId`, `senderId`, `senderRole`, `messageText`, `createdAt`, `read` fields. |
| `SecureMessageRepository.java` | Done | `sendMessage()`, `listenForMessagesForAppointment()`, `markMessagesRead()`, `hasUnreadMessagesForAppointment()`. |
| `MessageThreadActivity.java` | Done | Appointment-linked message view with real-time Firestore listener; unread badge on activity re-open. |
| `SecureMessageAdapter.java` | Done | Bubble-style adapter: outgoing right-aligned pink, incoming left-aligned grey. |
| `AppointmentAdapter.java` — message button | Done | `btnMessage` visible for non-cancelled, non-no-show; "New Message" label when unread. |
| `StudentHomeActivity.java` — message button | Done | `buttonUpcomingMessages` wired to `MessageThreadActivity` for the student's next appointment. |
| `CrisisEscalation.java` model | Done | Severity constants (`MODERATE`/`HIGH`/`IMMEDIATE`), action constants, `resolved` flag. |
| `CrisisEscalationRepository.java` | Done | `createEscalation()` — batch-writes escalation doc + links `crisisEscalationId` back to appointment. |
| `CrisisEscalationDialogFragment.java` | Done | AlertDialog-based; severity radio group, action radio group, notes field, single "Call Security" button. |
| `Appointment.java` — `isReturningStudent()` | Done | Field populated by `IntakeMatcher` when a student has prior COMPLETED or NO_SHOW appointments. |
| `returningStudentBadge` on appointment cards | Done | Orange badge visible when `appointment.isReturningStudent() == true`. |

### Problems Identified — Sprint 10 Deliverables Requiring Fix

The Sprint 10 implementation is functional but contains three architectural and UX issues that must be resolved in Sprint 11:

| Problem | Root Cause | Impact |
|---|---|---|
| **Messages stored per appointment, not per counselor-student pair.** `secureMessages` is a flat collection queried with `whereEqualTo("appointmentId", …)`. Loading all messages for a student across sessions requires one Firestore query per appointment — O(n) reads with no thread continuity. | `SecureMessageRepository` uses a flat root collection keyed by appointment. | No cross-session thread view; can't show full conversation history in one place; database grows as unbounded scattered documents. |
| **Counselor appointment cards are cluttered with 6 action buttons.** No-Show, Crisis, Profile, Notes, Messages, and Mark Attended all appear as buttons directly on the card face in `item_appointment.xml`, making each card dense and hard to scan quickly. | All actions were added inline to `AppointmentAdapter.onBindViewHolder()` with per-button visibility logic. | Counselors cannot quickly skim their appointment list; there is no single focused "session management" workspace per appointment. |
| **Crisis escalation is saved silently — no admin sees it.** `CrisisEscalationRepository.createEscalation()` writes to `crisisEscalations` collection successfully, but `AdminDashboardActivity` only shows reminder settings. Escalations are invisible after save. The crisis dialog also uses a single phone number for all action types and looks like a standard system dialog. | `AdminDashboardActivity` was not wired to read `crisisEscalations`. `CrisisEscalationDialogFragment` uses `AlertDialog.Builder` with a single hardcoded dialer button. | Escalations are filed but never actioned. No admin oversight. Counselor has no confirmation that the escalation reached anyone. |

### Current State Before Sprint 11

```text
Student:  registration → intake quiz → counselor recommendation → directory → booking/waitlist
          home → calendar → history → feedback → emergency → discreet mode
          upcoming messages button (per appointment, not per counselor pair)

Counselor: dashboard (cache-first, active-only, returning badge) → cluttered appointment cards
           → student profile → session history → notes (upsert, deletion)
           → crisis escalation (saved silently, ugly system dialog, one number)
           → no-show (guarded, orange theme) → manage availability
           → add slot → generate slots (multi-day, conflict-aware, break rules)
           → waitlist queue → on-leave status → availability settings

Admin:     dashboard → reminder settings → generate reminder records
           (no crisis escalation visibility)
```

---

## 1. Sprint 11 Objective

By the end of this sprint:

1. **Messages are stored in a counselor+student thread**, not scattered per appointment. The Firestore path `messageThreads/{counselorId_studentId}/messages/{msgId}` replaces the flat `secureMessages` collection. Every message in the sub-collection carries a `sessionDate` and `sessionTime` field that drives date-divider grouping in the UI. Looking up all messages between two people is a single O(1) path read — no collection scan needed.

2. **The message thread view shows the complete conversation across all past sessions**, ordered by `createdAt`, with session date dividers (e.g. "Mon, Apr 7 — 10:00 AM") grouping messages by the appointment they were sent during.

3. **Counselor appointment cards are decluttered.** The card face shows only: student name, date/time, status badge, returning badge. Tapping the card opens a dedicated `ManageSessionActivity` that contains all notes and all action buttons in one focused screen per appointment.

4. **Crisis escalation is a one-time-per-appointment action.** Once an escalation is saved for an appointment, the Crisis button in `ManageSessionActivity` is permanently disabled and labelled "Escalation Filed". The check is done by reading `appointment.crisisEscalationId`.

5. **The crisis escalation dialog is redesigned** using the app's custom dialog design language (pastel pink/white, rounded, no native AlertDialog chrome). It displays three distinct dial buttons with the same two numbers already in `strings.xml` (campus security + crisis line), each labelled, plus notes and severity/action fields, matching the visual style of the student-side `EmergencyDialogFragment`.

6. **Admin sees a Crisis Alerts tab.** `AdminDashboardActivity` gains a `TabLayout` with two tabs: "Reminders" (existing content) and "Crisis Alerts" (new `RecyclerView` of unresolved escalations). Each escalation row shows severity badge, counselor name (async lookup), student name (async lookup), date filed, action taken, notes, and a "Mark Resolved" button.

---

## 2. User Stories Addressed

| Story | Fix Type | Details |
|---|---|---|
| **US-18** Secure pre-session messaging | Architecture fix + UX improvement | Thread-based storage; full session history view; dividers |
| **US-15** Crisis escalation workflow | Design overhaul + hardening | Custom dialog; multiple numbers; one-time guard; admin visibility |
| **US-16** Admin automation | Extension | Crisis Alerts tab added alongside existing reminder settings tab |
| **New** Counselor session management UX | Declutter + consolidation | `ManageSessionActivity` replaces inline card buttons |

---

## 3. Feature Specifications

### 3.1 — Messaging Thread Architecture

#### Firestore Schema Change

**Old (flat, appointment-keyed):**
```
secureMessages/{messageId}
  appointmentId, counselorId, studentId, senderId, receiverId,
  senderRole, messageText, createdAt, read
```

**New (thread-keyed sub-collection):**
```
messageThreads/{counselorId_studentId}/           ← thread document
  counselorId       : String
  studentId         : String
  lastMessageAt     : Timestamp
  unreadByCounselor : int
  unreadByStudent   : int

messageThreads/{counselorId_studentId}/messages/{messageId}
  senderId     : String
  senderRole   : String  ("COUNSELOR" | "STUDENT")
  messageText  : String
  sessionDate  : String  ("yyyy-MM-dd" — the appointment's date, for divider grouping)
  sessionTime  : String  ("HH:mm" — the appointment's time, for divider label)
  createdAt    : Timestamp
  read         : boolean
```

`threadId = counselorId + "_" + studentId` — always counselor first, student second. This guarantees a single canonical document for every pair regardless of which side initiates.

#### `SecureMessage.java` Changes
- Remove `appointmentId` field (no longer needed for retrieval)
- Remove `receiverId` field (derivable from `senderRole` + thread context)
- Add `sessionDate` (String) and `sessionTime` (String) fields
- Add `threadId` helper: `return counselorId + "_" + studentId`

#### `SecureMessageRepository.java` — Full Rewrite

```java
// Path helper
private CollectionReference messagesFor(String counselorId, String studentId) {
    return db.collection("messageThreads")
             .document(counselorId + "_" + studentId)
             .collection("messages");
}

// Get all messages for a counselor-student pair (single path lookup)
public void getThreadMessages(String counselorId, String studentId,
                               OnMessagesLoadedCallback callback)

// Real-time listener for the same thread (replaces listenForMessagesForAppointment)
public ListenerRegistration listenToThread(String counselorId, String studentId,
                                            OnMessagesLoadedCallback callback)

// Send a message — also updates thread document's lastMessageAt + unread counter
public void sendMessage(String counselorId, String studentId,
                        String sessionDate, String sessionTime,
                        String senderId, String senderRole,
                        String text, OnMessageActionCallback callback)

// Mark all unread messages by receiverId as read; decrements thread unread counter
public void markThreadRead(String counselorId, String studentId,
                           String readerRole, OnMessageActionCallback callback)

// Check if thread has unread for a given role (used by both home screens for badge)
public void hasUnreadInThread(String counselorId, String studentId,
                              String readerRole, OnUnreadStatusCallback callback)
```

**Migration note:** Existing `secureMessages` documents are legacy. New sends go to `messageThreads`. Both activity callers must be updated to pass `counselorId + studentId + sessionDate + sessionTime` instead of `appointmentId`.

#### `MessageThreadActivity.java` — Refactor

- Change `EXTRA_APPOINTMENT_ID` → `EXTRA_SESSION_DATE` + `EXTRA_SESSION_TIME` (used only for the current send, to label new messages)
- `subscribeToMessages()` calls `listenToThread(counselorId, studentId, …)` instead of `listenForMessagesForAppointment`
- Adapter receives messages including `sessionDate`/`sessionTime`; adapter groups by session, inserting `TYPE_DATE_DIVIDER` view types between session groups
- "Send" stores the current `sessionDate` + `sessionTime` on the message so it stays grouped correctly

#### `SecureMessageAdapter.java` — Add Session Dividers

Two view types:
- `TYPE_MESSAGE` — existing pink/grey bubble rows
- `TYPE_DATE_DIVIDER` — centered light-grey chip: "Mon, Apr 7 · 10:00" styled like `item_slot_date_header.xml`

Divider logic: on `setMessages(List<SecureMessage>)`, insert a synthetic divider item whenever `sessionDate` changes from one message to the next.

---

### 3.2 — Counselor Appointment Card Declutter

#### `item_appointment.xml` — Simplified Card Face

Remove all action buttons (`noShowButton`, `crisisButton`, `profileButton`, `notesButton`, `btnMessage`, `btnMarkAttended`). Keep:
- `studentName` (TextView)
- `sessionDate` + `sessionTime` (TextViews)
- `sessionTopic` — status badge (CONFIRMED/COMPLETED/etc.)
- `returningStudentBadge` (TextView, GONE when not applicable)
- A subtle trailing chevron `›` on the right edge to hint the card is tappable

The entire card is the click target (set on `itemView`, not per-button).

#### `AppointmentAdapter.java` — Strip to Info Only

- `onBindViewHolder()` drops all per-button wiring
- `itemView.setOnClickListener()` → opens `ManageSessionActivity`
- Remove `showCrisisDialog()`, `showNoteDialog()`, `confirmNoShow()`, `openStudentProfile()`, `openMessageThread()`, `configureMarkAttendedButton()` from adapter — they move to `ManageSessionActivity`
- Keep: `applyStatusBadge()`, `normalizeTime()`, `formatDate()`, `isNoShowWindowOpen()`, async student name lookup

#### `ManageSessionActivity.java` — New

Entry point extras:
```java
String EXTRA_APPOINTMENT_ID
String EXTRA_STUDENT_ID
String EXTRA_COUNSELOR_ID
String EXTRA_DATE
String EXTRA_TIME
String EXTRA_STUDENT_NAME
```

Layout sections (top to bottom in `activity_manage_session.xml`):

```
[Pink header bar]
  ← back     "Manage Session"

[Student + session info card]
  {studentName}     {status badge}
  {date}  ·  {time}    [returning badge if applicable]

[SESSION NOTES  (section label)]
  RecyclerView: full SessionNote list (same as StudentProfileActivity notes)
  — adapter: SessionNoteHistoryAdapter (reused)
  — empty state: "No notes yet"

[SESSION ACTIONS  (section label)]
  [Open Messages]       outlined pink, always shown for non-cancelled
  [View Student Profile] outlined pink, always shown
  ─────────────────────────────────────────
  [Mark No-Show]        orange, guarded by isNoShowWindowOpen()
                        hidden for CANCELLED / COMPLETED / NO_SHOW
  [Mark Attended]       green, shown only if CONFIRMED + slot has passed
  ─────────────────────────────────────────
  [Crisis Escalation]   red outlined
                        DISABLED + text "Escalation Filed" if appointment.crisisEscalationId != null
```

**One-time crisis guard:** On `onCreate`, read `appointment.crisisEscalationId` from the intent extras (populated by `AppointmentAdapter` when building the intent). If non-null, the crisis button is disabled immediately — no additional Firestore read needed.

The crisis button `setOnClickListener` opens `CrisisEscalationDialogFragment` exactly as before, but after `onSuccess`, the button disables itself and changes text to "Escalation Filed" so the counselor gets immediate feedback.

---

### 3.3 — Crisis Escalation Dialog Redesign

#### Design Language

Matches `EmergencyDialogFragment` / `dialog_emergency.xml` style:
- Full custom `Dialog` (not `AlertDialog.Builder`) with `Window.FEATURE_NO_TITLE`
- Root: white rounded card (20dp corners), `#FFF0F5` soft pink header strip
- Width: 88% of screen width, centred

#### `dialog_crisis_escalation.xml` — Full Redesign

```
[Header — #FFF0F5 background]
  ⚠ Crisis Escalation     (pink icon + bold title)

[Body — white]
  ── SEVERITY ──────────────────────────────
  RadioGroup (horizontal chips):
    ○ Moderate   ○ High   ● Immediate

  ── ACTION TAKEN ──────────────────────────
  RadioGroup (vertical):
    ○ Completed safety plan
    ○ Called security
    ○ Referred to CAPS
    ○ Other

  ── EMERGENCY CONTACTS ────────────────────
  [📞 Campus Security — 042-35608000]  [Call]
  [📞 Crisis Line — 0311-7786264    ]  [Call]

  ── NOTES ─────────────────────────────────
  TextInputLayout (multiline, 3 lines min)

[Footer]
  [Cancel]   [Save Escalation]  ← pink filled
```

Each "Call" button uses `Intent.ACTION_DIAL` (matching `EmergencyDialogFragment` — no CALL_PHONE permission; user confirms before dialing).

#### `CrisisEscalationDialogFragment.java` — Redesign

- Replace `AlertDialog.Builder` with manual `Dialog` + `setContentView(R.layout.dialog_crisis_escalation)`
- Wire two `btnCallSecurity` / `btnCallCrisis` buttons to their respective numbers from `strings.xml`
- "Save Escalation" button: same `saveEscalation()` logic, but callback also calls `listener.onEscalationSaved(escalationId)` so `ManageSessionActivity` can disable its crisis button inline

Add callback interface:
```java
public interface OnEscalationSavedListener {
    void onEscalationSaved(String escalationId);
}
```

`ManageSessionActivity` implements this and updates the UI on success.

---

### 3.4 — Admin Crisis Alerts Tab

#### `AdminDashboardActivity.java` — Add TabLayout

Replace the current single-screen layout with a `TabLayout` + `ViewPager2` (or manual tab switching with `FrameLayout` container) holding two tabs:

- **Tab 0 — Reminders:** existing content (reminder summary + generate button + settings nav)
- **Tab 1 — Crisis Alerts:** new `RecyclerView` driven by `CrisisAlertAdapter`

#### `CrisisEscalationRepository.java` — Add Read Methods

```java
// Fetches all unresolved escalations ordered by createdAt descending
public void getUnresolvedEscalations(OnEscalationsLoadedCallback callback)

// Marks a single escalation resolved
public void markResolved(String escalationId, OnCrisisActionCallback callback)

interface OnEscalationsLoadedCallback {
    void onSuccess(List<CrisisEscalation> escalations);
    void onFailure(Exception e);
}
```

#### `CrisisAlertAdapter.java` — New

ViewHolder binds:
- **Severity badge**: pill TextView — red `#FFEBEE`/`#D32F2F` for IMMEDIATE, orange `#FFF3E8`/`#E8761A` for HIGH, yellow `#FFFDE7`/`#F57F17` for MODERATE
- **Counselor name**: async `UserRepository.getUserName(counselorId, …)`
- **Student name**: async `UserRepository.getUserName(studentId, …)`
- **Date filed**: formatted from `createdAt` Timestamp
- **Action taken**: human-readable string from `actionTaken` constant
- **Notes**: truncated to 2 lines
- **"Mark Resolved" button**: visible; on tap calls `markResolved(escalation.getId(), …)`, removes item from list on success

#### `item_crisis_alert.xml` — New

Horizontal card with:
- Left edge: colored severity strip (4dp wide, full height)
- Body: counselor name + student name + date + action + notes (2-line ellipsis)
- Right: "Mark Resolved" outlined red button (28sp, compact)

---

## 4. Files Created / Modified

### 4.1 New Files

```text
src/main/java/com/example/moogerscouncil/
├── ManageSessionActivity.java           // Appointment action hub (notes + all actions)
├── CrisisAlertAdapter.java              // Admin crisis feed adapter

src/main/res/layout/
├── activity_manage_session.xml          // Manage session screen layout
├── item_crisis_alert.xml                // Single crisis alert row for admin feed
├── item_message_date_divider.xml        // Session date divider for message thread
```

### 4.2 Modified Files

| File | Changes |
|---|---|
| `SecureMessage.java` | Remove `appointmentId`, `receiverId`; add `sessionDate`, `sessionTime` |
| `SecureMessageRepository.java` | Full rewrite — thread-based path `messageThreads/{cId_sId}/messages`; new API surface |
| `MessageThreadActivity.java` | Use thread lookup (counselorId + studentId); pass sessionDate/sessionTime on send; remove appointmentId dependency |
| `SecureMessageAdapter.java` | Add `TYPE_DATE_DIVIDER` view type; `setMessages()` inserts synthetic dividers on session boundary |
| `AppointmentAdapter.java` | Strip all action buttons + handlers; `itemView` click → `ManageSessionActivity` |
| `item_appointment.xml` | Remove all action buttons; add trailing chevron; keep info fields |
| `dialog_crisis_escalation.xml` | Full redesign — custom white/pink layout, dual call buttons, styled radio groups |
| `CrisisEscalationDialogFragment.java` | Replace `AlertDialog.Builder` with custom dialog; dual dial buttons; `OnEscalationSavedListener` callback |
| `CrisisEscalationRepository.java` | Add `getUnresolvedEscalations()`, `markResolved()` |
| `AdminDashboardActivity.java` | Add TabLayout; Crisis Alerts tab with `RecyclerView` driven by `CrisisAlertAdapter` |
| `AndroidManifest.xml` | Register `ManageSessionActivity` with `android:windowSoftInputMode="adjustResize"` |

---

## 5. Firestore Impact

| Collection | Change |
|---|---|
| `messageThreads/{cId_sId}` | **New** — one document per counselor-student pair; created on first message send |
| `messageThreads/{cId_sId}/messages` | **New** — all messages between the pair; keyed by auto-ID; indexed by `createdAt` |
| `secureMessages` | **Legacy** — not deleted; no new writes; callers migrated away |
| `crisisEscalations` | **Existing** — no schema change; new read operations added (`resolved == false` query) |

---

## 6. What Was Left in Sprint 10 / Spillover

| Item | Sprint 10 Status | Sprint 11 Action |
|---|---|---|
| US-17 Returning student indicator | Done (badge wired, `isReturningStudent()`) | No change needed |
| US-18 Secure messaging | Done (per-appointment model) | Full architecture replacement in Sprint 11 |
| US-16 Admin reminders | Done (settings + generate records) | Preserved; Crisis Alerts tab added alongside |
| US-29 Student waitlist history tab | In Progress | Carry to Sprint 12 — not Sprint 11 scope |
| US-30 Instant booking from waitlist dialog | In Progress | Carry to Sprint 12 — not Sprint 11 scope |

---

## 7. Definition of Done

- [ ] Sending a message from counselor side writes to `messageThreads/{cId_sId}/messages` — confirmed in Firestore console
- [ ] Sending a message from student side writes to the same path
- [ ] Opening a message thread shows all messages across all past sessions in chronological order
- [ ] Session date dividers appear between message groups from different appointment dates
- [ ] Appointment cards show no action buttons — tapping a card opens `ManageSessionActivity`
- [ ] `ManageSessionActivity` shows the full note list for the appointment
- [ ] No-Show button in `ManageSessionActivity` is hidden for CANCELLED/COMPLETED/NO_SHOW statuses
- [ ] Mark Attended button only appears if appointment is CONFIRMED and slot has passed
- [ ] Crisis Escalation button is disabled ("Escalation Filed") if `crisisEscalationId` is already set on the appointment
- [ ] Disabling is immediate in the UI after a successful `createEscalation()` — no second dialog possible
- [ ] Crisis escalation dialog is custom (not system AlertDialog chrome) — pastel pink/white
- [ ] Crisis dialog shows at least two distinct dial buttons with correct numbers from `strings.xml`
- [ ] Admin Crisis Alerts tab loads unresolved escalations from Firestore
- [ ] "Mark Resolved" button removes the escalation from the admin list
- [ ] Build passes: `./gradlew assembleDebug` with `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64`
- [ ] No existing regression: student booking flow, waitlist flow, note save/delete, slot generation all unaffected
