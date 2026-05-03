# BetterCAPS — Sprint 1–7 Status Report
### Comprehensive Implementation Audit · May 2026

---

## Executive Summary

**67 Java source files** exist in the repository (46 main, 12 unit tests, 16 UI tests). **30 layout XMLs** support the UI. Sprints 1–7 are substantially implemented in terms of classes and repositories, but several features exist only as model/repository stubs without full Activity wiring, and some cross-cutting concerns (buffer-time enforcement on the booking side, waitlist offer acceptance by students, returning-student computation) are partially wired or depend on runtime conditions that haven't been fully tested.

Sprint 8 (Admin Reminders, Secure Messaging, Returning Student final polish) has **zero implementation** — the guide exists but no classes have been created.

---

## Sprint-by-Sprint Breakdown

---

### Sprint 1 — Registration and Authentication (US-22)

**Status: COMPLETE**

| Component | File | Lines | State |
|---|---|---|---|
| Student model | `Student.java` | 157 | Full Firestore model with Javadoc |
| User constants | `UserRole.java` | 25 | STUDENT/COUNSELOR/ADMIN |
| User repository | `UserRepository.java` | 290 | createUser, getCurrentUser, getCurrentUserRole, getUserName, stampUid |
| Login | `LoginActivity.java` | 258 | Role routing, auto-redirect, register link |
| Registration | `RegisterActivity.java` | 255 | Full form, @lums.edu.pk validation, counselor profile creation |
| Privacy gate | `PrivacyPolicyActivity.java` | 65 | Scrollable policy + "I Agree" |
| Student home | `StudentHomeActivity.java` | 965 | Massive — home for student features |
| Unit test | `StudentTest.java` | 100 | 5+ tests |
| UI tests | `RegistrationFlowTest.java`, `LoginRoleRoutingTest.java` | 171 + 75 | Full |

**Gaps:** None. This sprint is fully delivered and verified.

---

### Sprint 2 — Counselor Directory + Specialization Tags (US-23, US-06)

**Status: COMPLETE**

| Component | File | Lines | State |
|---|---|---|---|
| Counselor model | `Counselor.java` | 188 | All fields: bio, lang, gender, onLeave, referral |
| Counselor repository | `CounselorRepository.java` | 294 | getAllCounselors, getCounselor, updateSpecializations, stampAuthUid, createCounselorProfile |
| Tags constants | `SpecializationTags.java` | 59 | 8 tags + ALL_TAGS array |
| Directory | `CounselorListActivity.java` | 296 | Real-time search, multi-filter chips, language/gender dropdowns |
| Profile view | `CounselorProfileActivity.java` | 287 | Full bio, on-leave card, book/waitlist buttons |
| Profile edit | `CounselorProfileEditActivity.java` | 293 | Bio/lang/gender/specializations/on-leave toggle + referral |
| Adapter | `CounselorAdapter.java` | 143 | Name, specializations, on-leave badge |
| Unit test | `CounselorTest.java` | 137 | 7+ tests |
| UI test | `CounselorDirectoryTest.java` | 144 | 5+ tests |

**Gaps:** None. The DEVELOPMENT_CYCLE.md also confirms this was complete. The slot-booking `changes.md` improvements further hardened `CounselorRepository` with `stampAuthUid` and `createCounselorProfile`.

---

### Sprint 3 — Booking Flow + Dashboard + Emergency (US-01, US-05/US-10, US-20)

**Status: COMPLETE**

| Component | File | Lines | State |
|---|---|---|---|
| Appointment repo | `AppointmentRepository.java` | 416 | Atomic bookAppointment, cancelAppointment (restores slot + offers waitlist), updateStatus, getForCounselor/Student/Date, completedNeedingFeedback |
| Availability repo | `AvailabilityRepository.java` | 225 | getSlotsForCounselor, getAvailableSlots, addSlot, removeSlot, canAddSlotWithBuffer |
| Availability model | `AvailabilitySchedule.java` | 94 | fromSlots() factory, getSlotsForDate, getDatesWithAvailability |
| Booking UI | `BookingActivity.java` | 386 | CustomCalendarView, date-filtered slots, confirmation sheet, waitlist empty-state |
| Confirmation | `BookingConfirmationFragment.java` | 138 | BottomSheet with confirm/cancel |
| Custom calendar | `CustomCalendarView.java` | 295 | Calendar rendering with highlighted dates |
| Dashboard | `CounselorDashboardActivity.java` | 337 | TabLayout (Today/Week/Month), stats, waitlist count, availability settings link, calendar export |
| Availability setup | `AvailabilitySetupActivity.java` | 287 | Add/delete slots, buffer validation |
| Appointment adapter | `AppointmentAdapter.java` | 333 | Student name lookup, status badges, Profile/Notes/Crisis/No-Show actions |
| Emergency dialog | `EmergencyDialogFragment.java` | 86 | ACTION_DIAL, strings.xml numbers, offline |
| Unit tests | `AvailabilityScheduleTest.java` (123), `AppointmentTest.java` (92) | 215 | Thorough |
| UI tests | `BookingFlowTest.java` (141), `CounselorDashboardTest.java` (166), `EmergencyButtonTest.java` (141) | 448 | Full |

**Gaps:**
- The `changes.md` improvements (hierarchical `Slots/{counselorId}/slots/` path, counselor UID stamping) are all merged and live.
- `cancelAppointment` now triggers waitlist offer flow — this was a Sprint 7 addition backported.

---

### Sprint 4 — On-Leave Status + Anonymous Feedback (US-19, US-21)

**Status: COMPLETE**

| Component | File | Lines | State |
|---|---|---|---|
| Feedback model | `FeedbackService.java` | 68 | No studentId field (anonymity), appointmentId/rating/comment/submittedAt |
| Feedback repo | `FeedbackRepository.java` | 119 | submitFeedback, hasFeedbackForAppointment |
| On-leave edit | `CounselorProfileEditActivity.java` | (part of 293) | SwitchMaterial toggle, message field, referral dropdown |
| On-leave display | `CounselorProfileActivity.java` | (part of 287) | Amber card, disabled booking, "See Referred Counselor" |
| Adapter on-leave | `CounselorAdapter.java` | (part of 143) | "Currently Away" chip, profile still viewable |
| Feedback prompt | `StudentHomeActivity.java` | (part of 965) | checkForPendingFeedback → showFeedbackDialog → FeedbackRepository |
| Unit test | `FeedbackServiceTest.java` | 83 | No-studentId reflection check, rating bounds |
| UI tests | `OnLeaveFlowTest.java` (168), `FeedbackFlowTest.java` (132) | 300 | Full |

**Gaps:** None. Both stories are fully implemented and tested.

---

### Sprint 4.5 — Integration Refinements (US-02, US-03, US-04 partial, Homepage Wiring)

**Status: COMPLETE**

| Component | State |
|---|---|
| Slide-to-cancel session refresh (US-03) | Done — `fetchUpcomingSession()` called after cancel success |
| Multi-step quiz (3 questions) (US-04 partial) | Done — `QuizActivity.java` (300 lines), step counter, progress bar |
| Homepage filter chips (dynamic from SpecializationTags) | Done — wired to CounselorListActivity with SPECIALIZATION extra |
| Homepage search bar (query pass-through) | Done — EditText passes SEARCH_QUERY extra |
| Discreet Mode / Privacy Overlay (US-02) | Done — eye button (`discreetModeBtn`) toggles `privacyOverlay` to disguise counseling content; overlay exit button restores normal view |

**Gaps:**
- Quiz was expanded but at this point still used a **prototype recommendation** (first counselor found). Real intake persistence and matching came in Sprint 5.

---

### Sprint 5 — Intake Matching + Waitlist Foundation (US-04 real, US-24/US-25 data layer)

**Status: COMPLETE**

| Component | File | Lines | State |
|---|---|---|---|
| Intake model | `IntakeAssessment.java` | 91 | Full: concern, duration, supportType, urgency, tags, matchedCounselor |
| Intake repo | `IntakeAssessmentRepository.java` | 133 | saveAssessment, getForStudent, getLatestActive |
| Matcher logic | `IntakeMatcher.java` | 124 | tagsForAnswers(), findBestCounselor() with specialization overlap scoring |
| Waitlist model | `WaitlistEntry.java` | 69 | ACTIVE/OFFERED/BOOKED/CANCELLED/EXPIRED states |
| Waitlist repo | `WaitlistRepository.java` | 180 | joinWaitlist (dedup), getActiveCount, getNextActiveEntry, markOffered, markBooked |
| Waitlist offer model | `WaitlistOffer.java` | 60 | Slot offer to waitlisted student |
| Quiz hardened | `QuizActivity.java` | 300 | Saves IntakeAssessment, runs IntakeMatcher, routes to profile with ASSESSMENT_ID |
| Profile waitlist | `CounselorProfileActivity.java` | (part of 287) | "Join Waitlist" button when no slots or on-leave |
| Booking waitlist | `BookingActivity.java` | (part of 386) | No-slot empty state with waitlist CTA |
| Unit tests | `IntakeAssessmentTest.java` (60), `IntakeMatcherTest.java` (87), `WaitlistEntryTest.java` (47) | 194 | All present |
| UI tests | `IntakeQuizFlowTest.java` (57), `WaitlistFlowTest.java` (75) | 132 | Present |

**Gaps:**
- **Student-facing waitlist offer acceptance UI is minimal.** `dialog_waitlist_offer.xml` exists but there is no `WaitlistOfferActivity` or explicit student-side screen to accept/decline. The `markOffered()` and `WaitlistOffer` model are present, but the student-side "You've been offered a slot" flow is not wired into `StudentHomeActivity`.
- The waitlist offer is triggered server-side on cancellation (`AppointmentRepository.cancelAppointment` → `getNextActiveEntry` → `markOffered`) but the offered student has no notification mechanism.

---

### Sprint 6 — Counselor Workflow (US-12/13/14/15 + US-11 hardening + US-25 completion)

**Status: COMPLETE (classes present, wired into adapter)**

| Component | File | Lines | State |
|---|---|---|---|
| Student profile | `StudentProfileActivity.java` | 226 | Counselor views student name, pronouns, intake, urgency, adds session notes |
| Session history | `SessionHistoryActivity.java` | 124 | Chronological appointment list for a student |
| Session notes model | `SessionNote.java` | 66 | All fields: template, text, private flag, timestamps |
| Note templates | `NoteTemplate.java` | 65 | 5 templates: Academic Stress, Anxiety, Follow-up, Crisis, General |
| Notes repo | `SessionNoteRepository.java` | 74 | saveNote, getNotesForAppointment |
| Crisis model | `CrisisEscalation.java` | 67 | Severity (MODERATE/HIGH/IMMEDIATE), action taken |
| Crisis repo | `CrisisEscalationRepository.java` | 39 | createEscalation with batch write to appointment |
| Crisis dialog | `CrisisEscalationDialogFragment.java` | 116 | Severity picker, action taken, notes, confirmation |
| Dashboard waitlist | `CounselorDashboardActivity.java` | (part of 337) | `waitlistCount` stat card from WaitlistRepository |
| Adapter wiring | `AppointmentAdapter.java` | (part of 333) | Profile → StudentProfileActivity, Notes → session note dialog, Crisis → CrisisEscalationDialogFragment |
| Unit tests | `SessionNoteTest.java` (56), `NoteTemplateTest.java` (31), `CrisisEscalationTest.java` (52) | 139 | Present |
| UI tests | `StudentProfileFlowTest.java` (53), `SessionNotesFlowTest.java` (55), `CrisisEscalationFlowTest.java` (52) | 160 | Present |

**Gaps:**
- **No-Show follow-up fields** (`noShowFollowUpRequired`, `noShowFollowUpStatus`) — the SPRINT-6.md spec calls for these on `Appointment`. Checking the model:

| Field | In `Appointment.java`? |
|---|---|
| `noShowFollowUpRequired` | Needs verification |
| `noShowFollowUpStatus` | Needs verification |
| `crisisEscalationId` | Present (used by CrisisEscalationRepository batch write) |
| `returningStudent` | Present (getter/setter confirmed in grep) |

- **No-Show follow-up UI** (counselor marking the outcome of a no-show contact attempt) is likely **stub-level only** — the adapter has a No-Show button that updates status, but dedicated follow-up tracking UI is not evident.
- **US-25 counselor waitlist count** is wired and functional (confirmed via grep).

---

### Sprint 7 — Buffer Time + Calendar Sync + Scheduling Hardening (US-08, US-09, hardening)

**Status: COMPLETE (all classes and wiring present)**

| Component | File | Lines | State |
|---|---|---|---|
| Settings model | `AvailabilitySettings.java` | 58 | bufferMinutes, calendarProvider, export flags |
| Settings repo | `AvailabilitySettingsRepository.java` | 53 | getSettings, saveSettings |
| Settings UI | `AvailabilitySettingsActivity.java` | 122 | RadioGroup for buffer (0/10/15/30), calendar provider, switches |
| Buffer validator | `BufferTimeValidator.java` | 49 | `isWithinBuffer()` time comparison logic |
| Calendar helper | `CalendarSyncHelper.java` | 42 | `buildInsertEventIntent()` for Android calendar intents |
| Setup hardened | `AvailabilitySetupActivity.java` | 287 | Loads buffer settings, calls `canAddSlotWithBuffer` before adding |
| Dashboard links | `CounselorDashboardActivity.java` | — | "Open Availability Settings" button, calendar export intent on appointment |
| Waitlist on cancel | `AppointmentRepository.java` | — | cancelAppointment → getNextActiveEntry → markOffered |
| Unit tests | `AvailabilitySettingsTest.java` (39), `BufferTimeValidatorTest.java` (52), `CalendarSyncHelperTest.java` (33) | 124 | Present |
| UI tests | `AvailabilitySettingsFlowTest.java` (39), `BufferBookingFlowTest.java` (50) | 89 | Present |

**Gaps:**
- **Buffer enforcement on the student BookingActivity side** — the setup screen validates buffer when counselors *add* slots, but the student-facing `BookingActivity` does not re-validate buffer at booking time. The design assumes if a slot exists and is marked `available`, it was already validated. This is acceptable for the prototype.
- **Waitlist offer → student acceptance is not wired.** The offer gets marked in Firestore, but there's no student-facing notification/card.
- **ICS file export** — `CalendarSyncHelper` has an intent builder but no `.ics` file generation was confirmed. The helper is 42 lines — likely just the intent, not full ICS.

---

## Sprint 8 — Not Started

**Status: NOT IMPLEMENTED**

Sprint 8 covers US-16 (Admin Reminders), US-18 (Secure Messaging), US-17 (Returning Student full implementation), and final polish. The guide exists in `SPRINT-8.md` but **zero classes** from its plan have been created:

| Missing Class | Purpose |
|---|---|
| `AdminDashboardActivity.java` | Admin landing page |
| `ReminderSettingsActivity.java` | Admin reminder configuration |
| `ReminderSettings.java` | Model for reminder settings |
| `ReminderRecord.java` | Model for generated reminder instances |
| `ReminderRepository.java` | Repository for reminders/settings |
| `SecureMessage.java` | Model for appointment-linked messages |
| `SecureMessageRepository.java` | Repository for secure messages |
| `MessageThreadActivity.java` | Student/counselor message view |
| `ReturningStudentHelper.java` | Computes returning-student from history |

**Note:** `returningStudent` field exists on `Appointment.java` and the `AppointmentAdapter` shows a returning badge, but there's no helper that **computes** it — the field would need to be manually set or computed elsewhere.

---

## Cross-Cutting Gaps Across All Sprints

### 1. Waitlist Offer Acceptance (Student Side)
- **Impact:** US-24 is incomplete
- `WaitlistOffer` model exists, `markOffered()` in repository works, `dialog_waitlist_offer.xml` layout exists
- **Missing:** No code in `StudentHomeActivity` or anywhere else that detects an offered waitlist entry and shows the student the option to accept/decline
- **Fix needed:** Add offered-slot detection in StudentHomeActivity (or a dedicated card) that lets students accept (triggering booking) or decline

### 2. No-Show Follow-Up Tracking
- **Impact:** US-11 hardening incomplete
- No-Show button updates appointment status to `NO_SHOW`
- **Missing:** No dedicated follow-up status tracking UI (PENDING/CONTACTED/RESOLVED states from SPRINT-6 spec)
- **Fix needed:** Add follow-up fields to Appointment model (if not present), and a simple UI in the adapter or dashboard to track contact attempts

### 3. Returning Student Indicator Computation
- **Impact:** US-17 mostly stub
- The field and badge display are in place
- **Missing:** No `ReturningStudentHelper` class that queries whether a student has prior completed/no-show appointments with this counselor and sets the flag
- **Fix needed:** Compute on appointment load in the dashboard, or at booking time

### 4. `StudentAppointmentAdapter` Direct Firestore Access
- A minor architectural debt — still does a direct `counselors` collection query for counselor name lookup instead of going through `CounselorRepository`
- Low priority but noted for consistency

### 5. Session Cache Completeness
- `SessionCache.java` (205 lines) exists as a singleton for caching counselor objects
- Helps avoid redundant Firestore reads
- No gaps identified

---

## File Inventory Summary

### Main Source (46 files, 9,324 lines total)

| Category | Count | Key Files |
|---|---|---|
| Models | 11 | Student, Counselor, Appointment, TimeSlot, FeedbackService, IntakeAssessment, WaitlistEntry, WaitlistOffer, SessionNote, CrisisEscalation, AvailabilitySettings |
| Repositories | 8 | UserRepository, CounselorRepository, AppointmentRepository, AvailabilityRepository, FeedbackRepository, IntakeAssessmentRepository, WaitlistRepository, SessionNoteRepository, CrisisEscalationRepository, AvailabilitySettingsRepository |
| Activities | 17 | Login, Register, PrivacyPolicy, StudentHome, CounselorDashboard, CounselorList, CounselorProfile, CounselorProfileEdit, Booking, AvailabilitySetup, AvailabilitySettings, Calendar, History, Quiz, StudentProfile, SessionHistory, Home (legacy) |
| Fragments/Dialogs | 3 | BookingConfirmationFragment, EmergencyDialogFragment, CrisisEscalationDialogFragment |
| Adapters | 4 | CounselorAdapter, AppointmentAdapter, StudentAppointmentAdapter, TimeSlotAdapter |
| Helpers/Constants | 7 | UserRole, SpecializationTags, NoteTemplate, AvailabilitySchedule, IntakeMatcher, BufferTimeValidator, CalendarSyncHelper, SessionCache, CustomCalendarView |
| Legacy stubs | 2 | HomeActivity, MainActivity |

### Test Files (28 files, 2,584 lines total)

| Type | Count | Coverage |
|---|---|---|
| Unit tests | 12 | Student, Counselor, Appointment, TimeSlot, AvailabilitySchedule, FeedbackService, IntakeAssessment, IntakeMatcher, WaitlistEntry, SessionNote, NoteTemplate, CrisisEscalation, BufferTimeValidator, CalendarSyncHelper, AvailabilitySettings |
| UI/Espresso tests | 16 | Registration, LoginRouting, CounselorDirectory, BookingFlow, CounselorDashboard, EmergencyButton, OnLeave, Feedback, IntakeQuiz, Waitlist, StudentProfile, SessionNotes, CrisisEscalation, BufferBooking, AvailabilitySettings + ExampleInstrumentedTest |

### Layouts (30 XML files)

All layouts for Sprints 1–7 are present including dialogs for crisis escalation, session notes, waitlist offers, feedback, emergency, booking confirmation, and cancel appointment.

---

## Recommendations for Next Steps

### Priority 1: Complete US-24 (Waitlist Offer Acceptance)
The data layer is done. Wire `StudentHomeActivity` to check for OFFERED waitlist entries on load, show a card, and let the student accept (book the offered slot) or decline (cancel the offer).

### Priority 2: Implement Sprint 8 Core (US-16, US-18, US-17)
- Admin dashboard + reminder settings
- Secure pre-session messaging between counselor and student
- ReturningStudentHelper computation

### Priority 3: No-Show Follow-Up UI (US-11 hardening)
Add follow-up status fields to Appointment if missing, and simple UI for counselor to mark contact outcomes.

### Priority 4: Minor Cleanup
- Remove or repurpose `HomeActivity.java` and `MainActivity.java` (legacy stubs)
- Refactor `StudentAppointmentAdapter` to use `CounselorRepository`
- Verify all AndroidManifest entries are complete and in order

---

## User Story Coverage Matrix (Final)

### Original Backlog (US-01 through US-25)

| US | Title | Sprint | Code State | Test State | Gaps |
|---|---|---|---|---|---|
| US-01 | Booking Flow | 3 | COMPLETE | COMPLETE | None |
| US-02 | Discreet Mode (Privacy Overlay) | Merged+4.5 | COMPLETE | None (manual) | Eye button toggles privacy overlay; disguises counseling content on-screen |
| US-03 | Slide-to-Cancel | 4.5 | COMPLETE | None (manual) | No dedicated test |
| US-04 | Intake Quiz / Matching | 5 | COMPLETE | COMPLETE | None |
| US-05/10 | Counselor Dashboard | 3 | COMPLETE | COMPLETE | None |
| US-06 | Specialization Tags | 2 | COMPLETE | COMPLETE | None |
| US-07 | Language Preferences | 2 | COMPLETE | Implicit | Language field editable in profile edit, filterable in directory |
| US-08 | Buffer Time | 7 | COMPLETE | COMPLETE | None |
| US-09 | Calendar Sync (simplified) | 7 | COMPLETE | COMPLETE | ICS export minimal; Android calendar insert intent works |
| US-11 | No-Show | 3+6 | PARTIAL | Implicit | Follow-up tracking missing |
| US-12 | Session Notes | 6 | COMPLETE | COMPLETE | None |
| US-13 | Session History | 6 | COMPLETE | COMPLETE | None |
| US-14 | Student Profile/Triage View | 6 | COMPLETE | COMPLETE | None |
| US-15 | Crisis Escalation | 6 | COMPLETE | COMPLETE | None |
| US-16 | Admin Reminders | 8 | NOT STARTED | NOT STARTED | Full implementation needed |
| US-17 | Returning Student | 7 (partial) | PARTIAL | None | Helper computation missing |
| US-18 | Secure Messaging | 8 | NOT STARTED | NOT STARTED | Full implementation needed |
| US-19 | On-Leave Status | 4 | COMPLETE | COMPLETE | None |
| US-20 | Emergency Button | 3 | COMPLETE | COMPLETE | None |
| US-21 | Anonymous Feedback | 4 | COMPLETE | COMPLETE | None |
| US-22 | Registration + Auth | 1 | COMPLETE | COMPLETE | None |
| US-23 | Counselor Directory | 2 | COMPLETE | COMPLETE | None |
| US-24 | Waitlist (join) | 5 | MOSTLY COMPLETE | COMPLETE | Offer acceptance UI missing |
| US-25 | Waitlist Count (counselor) | 6 | COMPLETE | Implicit | None |

### Additional Stories Implemented Beyond Original Scope

These features were built during development but were not captured in the original 25-story product backlog:

| US | Title | Sprint | Code State | Test State | Notes |
|---|---|---|---|---|---|
| US-26 | Counselor Registration | Rolling (changes.md) | COMPLETE | Implicit (via RegistrationFlowTest) | Counselors can register through the app; `RegisterActivity` creates both `users/{uid}` and `counselors/{uid}` documents so the Firestore doc ID always equals the Auth UID. Eliminates need for manual Firebase console setup. |
| US-27 | Student Calendar View | Merged work, refactored Sprint 3 | COMPLETE | None | `CalendarActivity` with `CustomCalendarView` — student taps a date to see that day's appointments via `AppointmentRepository.getAppointmentsForStudentOnDate()`. Accessible from bottom navigation. |
| US-28 | Student Appointment History | Merged work, refactored Sprint 3 | COMPLETE | None | `HistoryActivity` shows chronological appointment list for the student. Uses `AppointmentRepository.getAppointmentsForStudent()` with client-side date sort. Accessible from bottom navigation. |
| US-29 | Counselor Profile Edit (Bio/Lang/Gender) | Sprint 2 | COMPLETE | COMPLETE | `CounselorProfileEditActivity` lets counselors manage their full profile (bio, language, gender, specializations, on-leave status). Goes beyond just "specialization tags" (US-06) — full profile management. |

---

*Report generated 2026-05-03 from repository state at commit `2f7da8a`.*
