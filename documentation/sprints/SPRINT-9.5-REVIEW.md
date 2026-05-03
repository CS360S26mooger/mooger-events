# Sprint 9.5 Review — Dashboard Hardening, Slot Overhaul, Real-Time Listeners, Booking Guards & Profile Polish
### Retrospective: All Rolling Changes Between Sprint 9 and Sprint 10

---

## 0. Context

Sprint 9.5 captures all rolling changes made after Sprint 9 (Counselor Dashboard UI Overhaul) and before Sprint 10 (Admin/Messaging/Returning Student). These were not a single planned sprint but a continuous stream of hardening, fixes, and feature additions that emerged from testing the Sprint 9 deliverables. They are documented here as a single retrospective across three phases:

- **Phase A** — Dashboard hardening and session management
- **Phase B** — Slot management overhaul and bulk generation
- **Phase C** — Real-time listeners, booking guards, past sessions, and profile polish

### State After Sprint 9 — Entering 9.5

| Deliverable | Status |
|---|---|
| Counselor dashboard redesigned (pastel pink/white/purple, rounded cards) | Done |
| `item_appointment.xml` redesigned (4 buttons, conditional no-show visibility, status badges) | Done |
| Session notes — upsert pattern (`getNoteForAppointment` + `updateNote`) | Done |
| Logout confirmation dialog | Done |
| "Total Bookings" stat label (was "Total Patients") | Done |
| `dialog_session_note_styled.xml` — notes dialog redesigned | Done |

---

## 1. Problems Identified & Resolved

### Phase A — Dashboard Hardening & Session Management

| Problem | Location | Resolution |
|---|---|---|
| **No way to delete a session note.** Incorrect notes persisted forever. | `SessionNoteActivity`, `SessionNoteRepository` | Added `deleteNote()` with confirmation dialog. Delete button shown only when editing an existing note. |
| **Cancelled and no-show appointments visible in active list.** Dashboard showed all appointments regardless of status. | `CounselorDashboardActivity.filterByTab()` | Added second stream filter excluding `CANCELLED` and `NO_SHOW` from the adapter hand-off. Master list untouched. |
| **Note history shows save timestamp, not session timestamp.** Notes saved days after a session appeared with the wrong date. | `SessionNoteHistoryAdapter.buildTimestamp()` | Rewired to read `appointmentDate`/`appointmentTime` fields instead of Firestore `createdAt`. |
| **Appointment list causes full Firestore round-trip on every screen visit.** No caching layer. | `CounselorDashboardActivity` | Implemented cache-first rendering via `SessionCache` with 2-min TTL. Background Firestore fetch updates silently. |
| **Booking stat cards count all-time totals, never decrease.** Cancellations and no-shows didn't reduce counts. | `CounselorDashboardActivity.updateStats()` | All three stat cards now stream-filter `CONFIRMED` only. |
| **No-show action fires immediately on tap.** Misclicks permanently mark students no-show. | `AppointmentAdapter` | Added `confirmNoShow()` dialog with orange theme. New `ic_sleeping.xml` drawable. |
| **Two stale `loadAppointments()` calls.** Method renamed in Sprint 9 but two call sites missed. | `CounselorDashboardActivity` lines 159, 166 | Updated to `subscribeToAppointments()`. |

### Phase B — Slot Management Overhaul & Bulk Generation

| Problem | Location | Resolution |
|---|---|---|
| **Slot list is a flat unformatted text list.** No visual grouping, all slots look identical. | `AvailabilitySetupActivity`, old `TimeSlotSetupAdapter` | Replaced with `SlotGroupAdapter` — multi-view-type adapter with date headers (`TYPE_HEADER`) and slot cards (`TYPE_SLOT`). |
| **Deleting a slot has no confirmation.** Swipe-to-delete fires immediately. | `AvailabilitySetupActivity` | All deletions now route through `showConfirmDeleteDialog()` (available slots) or `showBookedSlotDialog()` (informational block for booked slots). |
| **Booked slots can be deleted the same way as available ones.** Leaves dangling appointment documents. | `AvailabilitySetupActivity` | Booked slot deletion shows a blocking informational dialog directing counselor to cancel the appointment first. |
| **Adding a slot is a simple inline time picker with no date awareness.** No calendar, no past-date guard. | `AvailabilitySetupActivity` | Extracted to `AddSlotActivity` — dedicated calendar-first flow with past-date blocking and existing-slot-date highlighting. |
| **No way to generate a full workday of slots.** Each slot requires 3 manual taps. | Not implemented | Built `GenerateSlotsActivity` — work hours + slot duration + break definitions → atomic `WriteBatch` creation. |
| **Edit profile fields near-invisible in idle state.** M3 theme overrides text colour to low-opacity grey. | `CounselorProfileEditActivity` | `setTextColor(0xFF0D0D0D)` on all fields post-`findViewById()` bypasses the `ColorStateList` override. |
| **Referral dropdown has grey surface fill.** M3 `ExposedDropdownMenu` style applies surface tint. | `activity_counselor_profile_edit.xml` | `boxBackgroundColor="#FFFFFF"` + `background="@null"` on the `AutoCompleteTextView`. |

### Phase C — Real-Time Listeners, Booking Guards, Past Sessions & Profile Polish

| Problem | Location | Resolution |
|---|---|---|
| **Dashboard polls Firestore on every onResume.** Cache-first rendering still fires a fresh fetch each time, causing redundant rebuilds. | `CounselorDashboardActivity` | Replaced poll-based fetching with Firestore `addSnapshotListener`. Subscribe once in `onCreate`; Firestore pushes only real changes. |
| **Snapshot listener fires redundant initial callback.** First callback duplicates cache data, causing a UI flicker. | `CounselorDashboardActivity` | Initial snapshot suppressed when `SessionCache` already has data. `appointmentsChanged()` diff check prevents rebuild if IDs and statuses haven't changed. |
| **Waitlist count fetched from Firestore on every onResume.** Unnecessary round-trip for a rarely-changing value. | `CounselorDashboardActivity` | `cachedWaitlistCount` renders instantly; network fetch only fires on first load or after navigating to waitlist screen. |
| **AvailabilitySetupActivity never invalidates slot cache.** After adding/removing slots, stale data persists. | `AvailabilitySetupActivity` | Added `SessionCache.invalidateSlots(counselorId)` in both add and remove success callbacks. |
| **Availability settings fetched from Firestore on every screen open.** Settings rarely change. | `AvailabilitySettingsActivity` | Added settings to `SessionCache` — renders cached value instantly, only fetches on first load. Updated on save. |
| **No-show button hidden entirely before time window.** Counselors couldn't see the button at all for future slots. | `AppointmentAdapter` | Button always visible for CONFIRMED. Tapping before the 10-min window shows an informational "Too Early" dialog instead of hiding the button. |
| **Students can book two appointments on the same day.** No safeguard against same-day double-booking. | `BookingActivity`, `AppointmentRepository` | `hasConfirmedBookingOnDate()` query (2-field: studentId + date, status checked client-side). Blocks booking with "Already Booked" dialog. Fail-closed on query error. |
| **No way to view past booked sessions.** Counselor dashboard only shows current/future appointments. | `CounselorDashboardActivity` | New `PastSessionsActivity` — dedicated screen showing past CONFIRMED/COMPLETED/NO_SHOW appointments, sorted newest-first. |
| **Counselors can't change their display name.** Edit Profile had bio/language/gender but no name field. | `CounselorProfileEditActivity` | Added "Display Name" field at top of profile section. Writes `name` to Firestore via `updateCounselorProfile()`. |
| **Updated name doesn't show on dashboard after save.** Dashboard loads name once in `onCreate`, never refreshes. | `CounselorDashboardActivity` | `refreshCounselorName()` fetches the counselor doc on every `onResume`. `invalidateCounselors()` called on profile save. |
| **Expired unbooked slots visible in counselor slot management.** Past available slots clutter the list. | `AvailabilitySetupActivity` | Filter: `isPast && isAvailable → skip`. Past booked slots kept (counselor needs them for notes/no-show). |
| **Student upcoming session card shows after slot time passes.** A 9:00 AM slot still shows as "upcoming" at 5:00 PM. | `StudentHomeActivity` | `resolveUpcoming()` now compares both date and time. Today's slots that have passed are skipped. |
| **Booking slots not ordered by time on student side.** Slots appear in Firestore insertion order, not chronologically. | `AvailabilitySchedule` | `fromSlots()` now sorts each date's slot list by time after grouping. |
| **Same-day booking query uses 3-field composite index.** Firestore requires a composite index for 3+ equality filters; without it the query silently fails. | `AppointmentRepository` | Simplified to 2-field query (studentId + date). Status checked client-side. |
| **Same-day check fails open.** If query errors (no index, network), booking proceeds anyway. | `BookingActivity` | Changed `onFailure` from `proceedWithBooking()` to error toast. Safeguard is now fail-closed. |
| **Matcher gives same counselor repeatedly.** Ties resolved by arbitrary Firestore ordering. | `IntakeMatcher`, `IntakeAssessmentRepository` | Global `recommendationCounts/{counselorId}` counter in Firestore. Ties broken by lowest recommendation count (equal traffic distribution). |
| **Matcher scoring too flat.** All tag matches scored equally; no position weighting. | `IntakeMatcher` | Weighted: primary tag +20, secondary +12, further tags +6. Bio keyword matches +3. Profile completeness +1. |
| **Back button on Edit Profile crashes/lags.** `android:onClick="finish"` uses reflection to find `finish(View)` which doesn't exist. | `activity_counselor_profile_edit.xml` | Removed `android:onClick="finish"`. Wired via `setOnClickListener` in Java. |
| **"Add Availability Slot" button uses orange accent.** Clashes with the app's pink palette. | `activity_counselor_dashboard.xml` | Changed stroke/text from `#F57242` to `#F0C8DC`/`#C96B8E` (pastel pink). |
| **Toast strings have trailing periods and inconsistent punctuation.** | `strings.xml` | Cleaned 48 strings across the entire app. |

---

## 2. User Stories Delivered

| ID | Story | Phase | Status |
|---|---|---|---|
| US-31 | As a counselor, I want to delete a session note so I can remove entries made in error. | A | Done |
| US-32 | As a counselor, I want cancelled and no-show appointments excluded from my active list. | A | Done |
| US-33 | As a counselor, I want notes to display the appointment date/time, not the save time. | A | Done |
| US-34 | As a counselor, I want my appointment list to load instantly from cache, refreshing in the background. | A | Done |
| US-35 | As a counselor, I want stat cards to count only confirmed appointments. | A | Done |
| US-36 | As a counselor, I want a confirmation dialog before marking a student as no-show. | A | Done |
| US-37 | As a counselor, I want the slot-creation calendar to prevent past date selection. | B | Done |
| US-38 | As a counselor, I want to see which dates I already have slots on, highlighted on the calendar. | B | Done |
| US-39 | As a counselor, I want to generate a full day's slots in one action by setting work hours, duration, and breaks. | B | Done |
| US-43 | As a counselor, I want to view my past booked sessions on a dedicated screen. | C | Done |
| US-44 | As a student, I want the app to prevent me from booking two appointments on the same day. | C | Done |
| US-45 | As a counselor, I want the no-show action to be blocked until 10 minutes after session start time. | C | Done |
| US-46 | As a counselor, I want to change my display name from the Edit Profile screen. | C | Done |
| US-47 | As a counselor, I want expired unbooked slots to be automatically hidden from my availability management screen. | C | Done |
| US-48 | As a student, I want my upcoming session card to automatically disappear once the appointment time has passed. | C | Done |

---

## 3. Implementation Details

---

### Phase A

#### 3.1 Session Note Deletion (US-31)

`SessionNoteRepository.deleteNote(String noteId, OnNoteActionCallback callback)` performs a standard `DocumentReference.delete()` on `sessionNotes/{noteId}`. Delete button visible only when editing an existing note. Confirmation dialog with red trash icon.

**Files modified:** `SessionNoteActivity.java`, `SessionNoteRepository.java`

#### 3.2 Active-Only Appointment Filter (US-32)

`filterByTab()` appends a second stream filter excluding `CANCELLED` and `NO_SHOW` after the tab date-range filter. Master list untouched.

**Files modified:** `CounselorDashboardActivity.java`

#### 3.3 Appointment-Anchored Note Timestamps (US-33)

`SessionNoteHistoryAdapter.buildTimestamp()` reads `appointmentDate`/`appointmentTime` instead of Firestore `createdAt`. Format: `"EEE, MMM d · HH:mm"`.

**Files modified:** `SessionNoteHistoryAdapter.java`

#### 3.4 Session Cache with Background Refresh (US-34)

`SessionCache` gained counselor appointments (2-min TTL), availability settings (session-scoped), and waitlist count caching. Later upgraded to use Firestore snapshot listeners with diff detection and initial snapshot suppression (Phase C).

| Cache key | TTL | Invalidated by |
|---|---|---|
| Student profile | 10 min | Logout |
| Counselor list | 5 min | Profile edit, logout |
| Student appointments | Session-scoped | Booking, cancellation |
| Counselor appointments | 2 min | No-show, cancellation, logout |
| Slot availability | Session-scoped | Slot add, slot delete |
| Availability settings | Session-scoped | Settings save, logout |

**Files modified:** `SessionCache.java`, `CounselorDashboardActivity.java`, `AvailabilitySettingsActivity.java`

#### 3.5 Live Booking Statistics (US-35)

`updateStats()` filters `CONFIRMED` only for all three stat cards.

**Files modified:** `CounselorDashboardActivity.java`

#### 3.6 No-Show Confirmation Guard + Orange Theme (US-36)

`confirmNoShow()` dialog with `ic_sleeping.xml` drawable, tinted orange (`#E8761A`/`#FFF3E8`). Later extended with 10-minute time gate (Phase C).

**Files modified:** `AppointmentAdapter.java`, `item_appointment.xml`
**New drawable:** `ic_sleeping.xml`

---

### Phase B

#### 3.7 Slot List — Multi-View-Type SlotGroupAdapter

Two view types: `TYPE_HEADER` (date string → `item_slot_date_header.xml`) and `TYPE_SLOT` (`TimeSlot` → `item_slot_counselor.xml`). `removeSlotById()` with orphan-header cleanup.

**Files modified:** `AvailabilitySetupActivity.java`
**New layouts:** `item_slot_counselor.xml`, `item_slot_date_header.xml`

#### 3.8 Slot Deletion — Confirmation Dialogs & Optimistic Updates

Available slots → `showConfirmDeleteDialog()`. Booked slots → `showBookedSlotDialog()` (informational only). Optimistic UI update with Firestore rollback on failure.

**Files modified:** `AvailabilitySetupActivity.java`

#### 3.9 Add New Slot — AddSlotActivity (US-37, US-38)

Calendar-first, past-blocked (`setMinDate`), existing slot dates highlighted. Buffer validation via `AvailabilitySettingsRepository`. Waitlist auto-resolution on creation.

**Files created:** `AddSlotActivity.java`, `activity_add_slot.xml`

#### 3.10 Generate Slots — GenerateSlotsActivity (US-39)

Work hours + duration (30/45/60 min) + break definitions → `computeSlotTimesForDate()` algorithm → atomic `WriteBatch.commit()`.

**Files created:** `GenerateSlotsActivity.java`, `activity_generate_slots.xml`, `dialog_add_break.xml`, `item_break.xml`
**Repository change:** `AvailabilityRepository.addSlotsBatch()`

#### 3.11 M3 Theme Fix

`setTextColor(0xFF0D0D0D)` on all fields. Referral dropdown: `boxBackgroundColor="#FFFFFF"` + `background="@null"`.

**Files modified:** `CounselorProfileEditActivity.java`, `activity_counselor_profile_edit.xml`

---

### Phase C

#### 3.12 Real-Time Snapshot Listeners

`AppointmentRepository.listenForCounselorAppointments()` and `listenForStudentAppointments()` using `addSnapshotListener` with scoped `whereEqualTo` filters. `CounselorDashboardActivity` subscribes once; listener removed in `onDestroy`. `appointmentsChanged(current, incoming)` prevents redundant rebuilds. Initial snapshot suppressed when cache is warm.

**Files modified:** `AppointmentRepository.java`, `CounselorDashboardActivity.java`

#### 3.13 Same-Day Booking Guard (US-44)

`hasConfirmedBookingOnDate()` — 2-field Firestore query (studentId + date), status checked client-side. "Already Booked" dialog on match. Fail-closed on error.

**Files modified:** `AppointmentRepository.java`, `BookingActivity.java`

#### 3.14 No-Show Time Gate (US-45)

`isNoShowWindowOpen(date, time)` — slot time + 10 minutes vs `System.currentTimeMillis()`. Button always visible for CONFIRMED. "Too Early" informational dialog before window opens.

**Files modified:** `AppointmentAdapter.java`

#### 3.15 Past Sessions Activity (US-43)

`PastSessionsActivity` — filters to `date < today`, status in (CONFIRMED, COMPLETED, NO_SHOW), sorted newest-first. Same `AppointmentAdapter` for card actions.

**Files created:** `PastSessionsActivity.java`, `activity_past_sessions.xml`

#### 3.16 Display Name Editing (US-46)

"Display Name" field above Bio. Reads/writes `name` on Counselor model. `refreshCounselorName()` on dashboard `onResume`. `invalidateCounselors()` on save.

**Files modified:** `CounselorProfileEditActivity.java`, `activity_counselor_profile_edit.xml`, `CounselorRepository.java`, `CounselorDashboardActivity.java`

#### 3.17 Expired Slot Filtering (US-47)

`loadSlots()`: `isPast && isAvailable → skip`. Past booked slots kept.

**Files modified:** `AvailabilitySetupActivity.java`

#### 3.18 Student Upcoming Session Time-Awareness (US-48)

`resolveUpcoming()` checks both date and time. Same-day slots past their time are skipped.

**Files modified:** `StudentHomeActivity.java`

#### 3.19 Slot Ordering

`AvailabilitySchedule.fromSlots()` sorts each date's slot list by time ascending after grouping.

**Files modified:** `AvailabilitySchedule.java`

#### 3.20 Matcher Improvements

Weighted scoring: primary tag +20, secondary +12, further +6, bio keyword +3, completeness +1. Tiebreaker: global `recommendationCounts/{counselorId}` counter (lowest count wins). `IntakeAssessmentRepository.incrementRecommendationCount()` via `FieldValue.increment(1)`.

**Files modified:** `IntakeMatcher.java`, `IntakeAssessmentRepository.java`, `QuizActivity.java`

---

## 4. Files Created & Modified

### New Files

| File | Purpose |
|---|---|
| `AddSlotActivity.java` | Calendar-first single slot creation |
| `GenerateSlotsActivity.java` | Full-day bulk slot generation |
| `PastSessionsActivity.java` | Past booked sessions screen |
| `activity_add_slot.xml` | Layout for AddSlotActivity |
| `activity_generate_slots.xml` | Layout for GenerateSlotsActivity |
| `activity_past_sessions.xml` | Layout for PastSessionsActivity |
| `dialog_add_break.xml` | Break entry dialog |
| `item_break.xml` | Break list row |
| `item_slot_counselor.xml` | Slot card for SlotGroupAdapter |
| `item_slot_date_header.xml` | Date group header for SlotGroupAdapter |
| `ic_sleeping.xml` | Sleeping-person drawable for no-show theme |
| `NoShowTimeWindowTest.java` | Unit tests for 10-min gate |
| `SlotGenerationTest.java` | Unit tests for bulk slot algorithm |
| `DashboardFilterTest.java` | Unit tests for filters + diff detection |
| `PastSessionsFlowTest.java` | UI test for past sessions screen |

### Modified Files

| File | Changes |
|---|---|
| `AppointmentRepository.java` | Snapshot listeners, `hasConfirmedBookingOnDate()` |
| `AppointmentAdapter.java` | `isNoShowWindowOpen()`, `showNoShowTooEarlyDialog()`, `confirmNoShow()`, orange theme |
| `CounselorDashboardActivity.java` | Snapshot listener, `appointmentsChanged()`, `refreshCounselorName()`, `cachedWaitlistCount`, cache-first load, active-only filter, CONFIRMED stats, Past Sessions button, section label, stale call fixes |
| `BookingActivity.java` | Same-day guard, `showAlreadyBookedDialog()`, `proceedWithBooking()` |
| `StudentHomeActivity.java` | `resolveUpcoming()` time-aware filtering |
| `AvailabilitySetupActivity.java` | `SlotGroupAdapter`, optimistic delete, confirmation dialogs, past slot filtering, cache invalidation, Generate Slots button |
| `AvailabilitySettingsActivity.java` | Settings caching via SessionCache |
| `AvailabilitySchedule.java` | Per-date time sorting in `fromSlots()` |
| `AvailabilityRepository.java` | `addSlotsBatch()` |
| `CounselorProfileEditActivity.java` | Display name field, M3 text colour override, cache invalidation, back button fix |
| `CounselorRepository.java` | `name` field in `updateCounselorProfile()` |
| `IntakeMatcher.java` | Weighted scoring, recommendation count tiebreaker |
| `IntakeAssessmentRepository.java` | `recommendationCounts` collection access |
| `QuizActivity.java` | Fetch counts → match → increment |
| `SessionNoteActivity.java` | Delete button + confirmation dialog |
| `SessionNoteRepository.java` | `deleteNote()` method |
| `SessionNoteHistoryAdapter.java` | Timestamp reads appointment date/time |
| `SessionCache.java` | Counselor appointment cache, availability settings cache |
| `activity_counselor_dashboard.xml` | Past Sessions button, section label, slot banner colour |
| `activity_counselor_profile_edit.xml` | Display name field, referral dropdown fix, back button onClick removed |
| `activity_availability_setup.xml` | Generate Slots button |
| `item_appointment.xml` | Orange no-show button styling |
| `strings.xml` | 48 strings cleaned, new strings for guards/dialogs/labels |
| `AndroidManifest.xml` | `AddSlotActivity`, `GenerateSlotsActivity`, `PastSessionsActivity` registered |
| `NoShowGuardTest.java` | +3 tests for time gate |
| `AppointmentTest.java` | +3 tests for same-day guard |
| `IntakeMatcherTest.java` | +3 tests for weighted scoring + recommendation count |

---

## 5. Firestore Impact

### New Collection

| Collection | Purpose |
|---|---|
| `recommendationCounts/{counselorId}` | Global counter (`count` field) for matcher traffic distribution. Atomically incremented via `FieldValue.increment(1)`. |

### Existing Collections

No schema changes. `addSlotsBatch()` writes to `Slots/{counselorId}/slots`. `deleteNote()` deletes from `sessionNotes/{noteId}`. `hasConfirmedBookingOnDate()` queries `appointments`. Snapshot listeners attach to `appointments` with scoped filters.

---

## 6. Test Coverage Added

| Test File | Tests | Covers |
|---|---|---|
| `NoShowTimeWindowTest.java` | 8 | Null inputs, future/past slots, 10-min boundary, invalid formats |
| `SlotGenerationTest.java` | 10 | Simple workday, breaks, buffer conflicts, durations, edge cases |
| `DashboardFilterTest.java` | 10 | Active-only filter, CONFIRMED stats, past sessions, sort order, `appointmentsChanged()` diff |
| `PastSessionsFlowTest.java` | 2 | Screen renders, back button visible |
| `NoShowGuardTest.java` | +3 | Time gate blocks future, allows past, button always visible |
| `AppointmentTest.java` | +3 | Same-day confirmed blocks, different date allows, cancelled allows |
| `IntakeMatcherTest.java` | +3 | Primary vs secondary weighting, recommendation count tiebreaker, bio bonus |

**Total new test methods: 39**

---

## 7. Definition of Done — Assessment

| Criterion | Status |
|---|---|
| Session note deletion with confirmation | Done |
| Active-only appointment filter in dashboard and stats | Done |
| Appointment-anchored note timestamps | Done |
| Cache-first dashboard rendering with background refresh | Done |
| CONFIRMED-only booking stat counts | Done |
| No-show confirmation guard + orange theme | Done |
| Stale `loadAppointments()` calls fixed | Done |
| Slot list grouped by date with section headers | Done |
| Slot deletion requires confirmation | Done |
| Booked slots show informational block (no destructive action) | Done |
| Calendar-first slot creation with past-date blocking | Done |
| Existing slot dates highlighted on add-slot calendar | Done |
| Full-day bulk slot generation with breaks | Done |
| Atomic WriteBatch for generated slots | Done |
| Profile edit fields visible in idle state | Done |
| Referral dropdown grey fill removed | Done |
| Real-time snapshot listeners replace polling | Done |
| Diff detection prevents redundant UI rebuilds | Done |
| Initial snapshot suppressed when cache is warm | Done |
| Waitlist count cached with invalidation | Done |
| Availability settings cached | Done |
| Slot cache invalidated on add/remove | Done |
| Same-day booking blocked with dialog (fail-closed) | Done |
| No-show button visible with time-gated dialog | Done |
| Past Sessions as dedicated activity | Done |
| Counselor display name editable | Done |
| Name refreshes on dashboard return | Done |
| Expired unbooked slots hidden | Done |
| Student upcoming session hides after slot passes | Done |
| Slots ordered by time on student side | Done |
| Matcher scoring weighted + counter tiebreaker | Done |
| Back button crash fixed | Done |
| Toast punctuation cleaned app-wide | Done |
| 39 new test methods added | Done |
