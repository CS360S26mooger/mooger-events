# BetterCAPS — Phase 4 Final Deliverable
### Complete Development Cycle · Sprints 1–11

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Team and Process](#2-team-and-process)
3. [Architecture and Design Patterns](#3-architecture-and-design-patterns)
4. [Firestore Data Model](#4-firestore-data-model)
5. [Sprint 1 — Registration and Authentication (US-22)](#5-sprint-1--registration-and-authentication-us-22)
6. [Sprint 2 — Counselor Directory and Specialization Tags (US-23, US-06)](#6-sprint-2--counselor-directory-and-specialization-tags-us-23-us-06)
7. [Sprint 3 — Booking Flow, Dashboard, Emergency Button (US-01, US-05/10, US-20)](#7-sprint-3--booking-flow-dashboard-emergency-button-us-01-us-0510-us-20)
8. [Sprint 4 — On-Leave Status and Anonymous Feedback (US-19, US-21)](#8-sprint-4--on-leave-status-and-anonymous-feedback-us-19-us-21)
9. [Sprint 4.5 — Integration Refinements (US-02, US-03, US-04 partial)](#9-sprint-45--integration-refinements-us-02-us-03-us-04-partial)
10. [Sprint 5 — Intake Matching and Waitlist Foundation (US-04, US-24)](#10-sprint-5--intake-matching-and-waitlist-foundation-us-04-us-24)
11. [Sprint 6 — Counselor Workflow: Notes, Crisis, Student Profile (US-12–15, US-25)](#11-sprint-6--counselor-workflow-notes-crisis-student-profile-us-12-15-us-25)
12. [Sprint 7 — Buffer Time and Calendar Sync (US-08, US-09)](#12-sprint-7--buffer-time-and-calendar-sync-us-08-us-09)
13. [Sprint 8 — Waitlist Hardening: Preference Requests and Auto-Resolution](#13-sprint-8--waitlist-hardening-preference-requests-and-auto-resolution)
14. [Sprint 9 — Dashboard Overhaul, Notes Hardening, No-Show Safeguards](#14-sprint-9--dashboard-overhaul-notes-hardening-no-show-safeguards)
15. [Sprint 9.5 — Rolling Hardening: Cache, Slot Overhaul, Real-Time Listeners](#15-sprint-95--rolling-hardening-cache-slot-overhaul-real-time-listeners)
16. [Sprint 10 — Admin Reminders, Secure Messaging, Returning Student (US-16, US-17, US-18)](#16-sprint-10--admin-reminders-secure-messaging-returning-student-us-16-us-17-us-18)
17. [Sprint 11 — Messaging Thread Architecture, Counselor UX Declutter, Crisis Admin](#17-sprint-11--messaging-thread-architecture-counselor-ux-declutter-crisis-admin)
18. [Final Class Inventory](#18-final-class-inventory)
19. [Test Coverage Summary](#19-test-coverage-summary)
20. [User Story Traceability Matrix](#20-user-story-traceability-matrix)
21. [Retrospective — Lessons Across Sprints](#21-retrospective--lessons-across-sprints)

---

## 1. Project Overview

**BetterCAPS** is a university counseling appointment platform built as an Android application with Firebase (Authentication + Firestore) as the backend. The app serves three user roles — Students, Counselors, and Administrators — and implements the complete student-counselor appointment lifecycle: registration, counselor discovery, intake matching, booking, session management, waitlisting, secure messaging, admin oversight, and post-session feedback.

The project was completed across 11 sprints (plus a 9.5 hardening sprint), progressing from a stub codebase to a fully functional, production-ready Android application. This document is the Phase 4 final deliverable — a complete record of every architectural decision, sprint outcome, and design tradeoff made throughout the project.

### Final Deliverable — Achieved Outcomes

- Students can register, log in, complete an intake quiz, browse and filter counselors, book appointments, manage a waitlist, exchange secure messages, and view session history.
- Counselors can log in, view a real-time dashboard, manage availability with buffer-time validation, generate bulk slots, access session notes, file crisis escalations, and view past sessions.
- Admins can configure appointment reminders and review unresolved crisis escalations.
- An emergency contact button is accessible from the student home screen at all times.
- All implemented features have corresponding unit and/or UI tests.
- Every model class is documented with Javadoc.
- All user-visible strings are externalized to `strings.xml` — no hardcoded UI text.

### User Stories Implemented

| Tier | Stories | Sprint |
|---|---|---|
| **Core** | US-22 Registration + Auth | 1 |
| **Core** | US-23 Counselor Directory, US-06 Specialization Tags | 2 |
| **Core** | US-01 Booking, US-05/10 Dashboard, US-20 Emergency | 3 |
| **Secondary** | US-19 On-Leave Status, US-21 Anonymous Feedback | 4 |
| **Integration** | US-02 Discreet Mode, US-03 Slide-to-Cancel, US-04 (partial) | 4.5 |
| **Feature** | US-04 Intake Matching (complete), US-24 Waitlist Foundation | 5 |
| **Feature** | US-12 Session Notes, US-13 History, US-14 Student Profile, US-15 Crisis | 6 |
| **Feature** | US-08 Buffer Time, US-09 Calendar Sync | 7 |
| **Feature** | US-24 Waitlist Hardening (full lifecycle), US-25 Counselor Queue | 8 |
| **Polish** | Dashboard overhaul, no-show safeguards, note upsert | 9 |
| **Hardening** | Real-time listeners, slot management overhaul, booking guards | 9.5 |
| **Feature** | US-16 Admin Reminders, US-17 Returning Student, US-18 Secure Messaging | 10 |
| **Architecture** | Messaging thread redesign, appointment card declutter, crisis admin | 11 |

---

## 2. Team and Process

### Team

| Name | GitHub ID |
|---|---|
| Muhammad Shahbaz Aziz Khan | ShahbazAzizK |
| Mujeeb Asad | mujeeb-asad |
| Hannan Mustafa | hannanmustafa08 |
| Muhammad Hasan Musa Gondal | Musa-Gondal |
| Muhammad Ibrahim | not-ibrahim |

### Agile Process

The team operated in **weekly sprints** following Scrum:

| Event | Cadence | Purpose |
|---|---|---|
| Sprint Planning | Start of each week | Select stories, assign owners, estimate points |
| Daily Sync | As needed (Slack) | Flag blockers, share progress |
| Sprint Review | End of week, in lab with TA | Demo completed stories, get feedback |
| Sprint Retrospective | Post-review | Identify improvements |

Stories moved through **Todo → In Progress → In Review → Done** on the GitHub Kanban board.

### Coding Conventions

- **Naming:** `camelCase` (variables/methods), `PascalCase` (classes), `UPPER_SNAKE_CASE` (constants).
- **Package:** All classes under `com.example.moogerscouncil`.
- **Model classes:** Pure Firestore data containers. No business logic beyond simple validation.
- **Repository pattern:** All Firestore reads/writes through dedicated `*Repository` classes. Activities never call Firestore directly.
- **Error handling:** Every Firestore call has `.addOnFailureListener` with user-facing error messages.
- **String resources:** All user-visible strings in `res/values/strings.xml`. No hardcoded strings.
- **Javadoc:** Every model class and public method documented.

### GitHub Practices

- Feature branches named `feature/US-XX-short-description`.
- No direct commits to `main`. All merges via reviewed Pull Requests.
- Each PR references its GitHub Issue.

---

## 3. Architecture and Design Patterns

### Layered Architecture

```
┌──────────────────────────────────────────────────────────┐
│                    UI Layer (Activities)                   │
│  LoginActivity, StudentHomeActivity, BookingActivity,     │
│  CounselorDashboardActivity, ManageSessionActivity, ...   │
└───────────────────────┬──────────────────────────────────┘
                        │ calls
┌───────────────────────▼──────────────────────────────────┐
│                  Repository Layer                          │
│  UserRepository, CounselorRepository,                     │
│  AppointmentRepository, AvailabilityRepository,           │
│  SessionNoteRepository, SecureMessageRepository, ...      │
└───────────────────────┬──────────────────────────────────┘
                        │ reads / writes
┌───────────────────────▼──────────────────────────────────┐
│                  Firestore (Firebase)                      │
│  /users  /counselors  /slots  /appointments  /feedback    │
│  /sessionNotes  /crisisEscalations  /waitlistEntries      │
│  /reminderSettings  /reminderRecords                      │
│  /messageThreads/{id}/messages                            │
└──────────────────────────────────────────────────────────┘
```

### Design Patterns Used

| Pattern | Where Applied | Why |
|---|---|---|
| **Repository** | All `*Repository` classes | Isolates data layer. Activities cannot be unit tested; repositories can be mocked. |
| **Callback** | All repository methods use inner interface callbacks | Firestore is asynchronous. Callbacks decouple caller from the async result. |
| **Factory Method** | `AvailabilitySchedule.fromSlots()`, `EmergencyDialogFragment.newInstance()`, `BookingConfirmationFragment.newInstance()` | Encapsulates construction, enforces required parameters. |
| **Adapter** | All `*Adapter` classes | Standard RecyclerView pattern for binding data to list views. |
| **Value Object** | `AvailabilitySchedule` | Local, non-persisted grouping of TimeSlots by date for calendar rendering. |
| **Constants Class** | `UserRole`, `SpecializationTags` | Single source of truth for string constants. Prevents typos across the codebase. |
| **Singleton** | `SessionCache` | Single in-memory cache instance to avoid redundant Firestore reads across activities. |
| **Static Utility** | `IntakeMatcher`, `WaitlistMatcher`, `BufferTimeValidator`, `CalendarSyncHelper`, `ReturningStudentHelper`, `NoteTemplate`, `AppToast` | Stateless logic. No need for instantiation — all methods are static. |
| **Snapshot Listener** | `CounselorDashboardActivity`, `MessageThreadActivity` | Real-time Firestore listeners push updates instead of polling on every resume. |

---

## 4. Firestore Data Model

### Collection: `users`
```
users/{uid}
  ├── name: String
  ├── email: String
  ├── role: String              // "student" | "counselor" | "admin"
  ├── preferredName: String
  ├── pronouns: String
  └── createdAt: Timestamp
```

### Collection: `counselors`
```
counselors/{counselorId}        // counselorId == Auth UID (enforced by registration)
  ├── uid: String
  ├── name: String
  ├── bio: String
  ├── specializations: List<String>
  ├── language: String
  ├── gender: String
  ├── onLeave: Boolean
  ├── onLeaveMessage: String
  └── referralCounselorId: String
```

### Collection: `Slots` (hierarchical — changed in changes.md)
```
Slots/{counselorAuthUID}/slots/{slotId}
  ├── counselorId: String
  ├── date: String              // "yyyy-MM-dd"
  ├── time: String              // "HH:mm"
  └── available: Boolean
```
> **Note:** Originally `slots/{slotId}` (flat). Moved to hierarchical path to ensure counselor doc ID always equals their Auth UID.

### Collection: `appointments`
```
appointments/{appointmentId}
  ├── studentId: String
  ├── counselorId: String
  ├── slotId: String
  ├── date: String
  ├── time: String
  ├── status: String            // "CONFIRMED" | "COMPLETED" | "CANCELLED" | "NO_SHOW"
  ├── returningStudent: Boolean // computed by ReturningStudentHelper at booking time
  └── crisisEscalationId: String
```

### Collection: `feedback`
```
feedback/{feedbackId}
  ├── appointmentId: String
  ├── rating: int               // 1–5
  ├── comment: String
  └── submittedAt: Timestamp
  // NOTE: no studentId — anonymity enforced at schema level
```

### Collection: `waitlistEntries`
```
waitlistEntries/{entryId}
  ├── studentId: String
  ├── counselorId: String
  ├── preferredDates: List<String>   // ISO dates the student prefers
  ├── preferredStartTime: String     // "HH:mm"
  ├── preferredEndTime: String       // "HH:mm"
  ├── status: String                 // "ACTIVE" | "RESOLVED" | "CANCELLED"
  ├── requestedAt: Timestamp         // FIFO ordering key
  └── resolvedAppointmentId: String
```

### Collection: `sessionNotes`
```
sessionNotes/{noteId}
  ├── appointmentId: String
  ├── counselorId: String
  ├── noteText: String
  ├── templateKey: String
  └── appointmentDate: String
```

### Collection: `crisisEscalations`
```
crisisEscalations/{escalationId}
  ├── appointmentId: String
  ├── counselorId: String
  ├── studentId: String
  ├── severity: String          // "MODERATE" | "HIGH" | "IMMEDIATE"
  ├── actionTaken: String
  ├── notes: String
  └── resolved: Boolean
```

### Collection: `availabilitySettings`
```
availabilitySettings/{counselorId}
  ├── bufferMinutes: int        // 0 | 10 | 15 | 30
  ├── externalCalendarEnabled: Boolean
  └── calendarProvider: String
```

### Collection: `reminderSettings`
```
reminderSettings/default
  ├── enabled24Hour: Boolean
  ├── enabled1Hour: Boolean
  ├── message24Hour: String
  └── message1Hour: String
```

### Collection: `reminderRecords`
```
reminderRecords/{recordId}
  ├── appointmentId: String
  ├── studentId: String
  ├── reminderType: String      // "24_HOUR" | "1_HOUR"
  ├── messageText: String
  ├── scheduledFor: Timestamp
  └── delivered: Boolean
```

### Collection: `messageThreads` (Sprint 11 architecture)
```
messageThreads/{counselorId_studentId}/            // thread document
  ├── counselorId: String
  ├── studentId: String
  ├── lastMessageAt: Timestamp
  ├── unreadByCounselor: Boolean
  └── unreadByStudent: Boolean

messageThreads/{counselorId_studentId}/messages/{messageId}
  ├── senderId: String
  ├── senderRole: String        // "counselor" | "student"
  ├── messageText: String
  ├── sessionDate: String
  ├── sessionTime: String
  ├── createdAt: Timestamp
  └── read: Boolean
```
> **Note:** Replaces the Sprint 10 flat `secureMessages/{id}` collection. Legacy data not deleted, no new writes to old collection.

### Collection: `recommendationCounts`
```
recommendationCounts/{counselorId}
  └── count: int                // number of intake assessments that matched this counselor
```

---

## 5. Sprint 1 — Registration and Authentication (US-22)

**Duration:** Week 1 · **Points:** 8

**Story:** *As a student, I want to register using my university email, set my preferred name and pronouns, and be guided through the privacy policy, so that I am formally verified and informed before using the service.*

### Starting State

The project began with a functional `LoginActivity` (Firebase Auth sign-in, role selector, animated transitions) and three empty stubs (`RegisterActivity`, `HomeActivity`, and their layouts). `Counselor`, `TimeSlot`, and `Appointment` models existed. `Counselor.java` had a package declaration mismatch.

### What Was Built

| Deliverable | Type | Purpose |
|---|---|---|
| `Student.java` | Model | Firestore-mapped container for `users`. Fields: uid, name, email, preferredName, pronouns, role, createdAt. Role defaults to `UserRole.STUDENT`. |
| `UserRole.java` | Constants | STUDENT, COUNSELOR, ADMIN string constants. |
| `UserRepository.java` | Repository | `createUser()`, `getCurrentUser()`, `getCurrentUserRole()`, `getUserName()`. Three async callback interfaces. |
| `RegisterActivity.java` | Activity | Full registration form with @lums.edu.pk domain validation, password matching, Firebase Auth + Firestore write flow. |
| `PrivacyPolicyActivity.java` | Activity | Scrollable privacy policy gate. Routes to StudentHomeActivity or CounselorDashboardActivity based on role. |
| `StudentHomeActivity.java` | Activity | Student welcome screen with preferred name and logout. |
| `LoginActivity.java` updates | Activity | Role-based routing and auto-redirect if session exists. |
| `StudentTest.java` | Unit Test | 5 tests: default role, constructor, empty constructor, setter overrides, timestamp. |
| `RegistrationFlowTest.java` | UI Test | 5 Espresso tests: form fields, invalid email, password mismatch, short password, empty name. |
| `LoginRoleRoutingTest.java` | UI Test | 4 Espresso tests: role buttons displayed, button text, sign-up link. |

### Key Technical Decisions

- **Email domain validation is client-side only.** Server-side rejection would require Cloud Functions, which is out of scope.
- **Role in Firestore, not Firebase Auth custom claims.** Avoids token refresh after role assignment.
- **Sequential Auth + Firestore write.** Auth must complete before Firestore write because the document ID is the Auth UID.

### Data Flow: Registration

```
User fills form → validateInputs()
    → FirebaseAuth.createUserWithEmailAndPassword()
        → uid = authResult.getUid()
            → Student s = new Student(uid, name, email, ...)
            → UserRepository.createUser(s, callback)
                → Firestore: users/{uid}.set(s)
                    → navigate to PrivacyPolicyActivity
```

### Concurrent Teammate Work (Merged Between Sprint 1 and Sprint 2)

Teammate Shahbaz merged `shahbaz/student-dash`, `shahbaz/counselor-dashboard`, and `shahbaz/booking-and-dashboard`, introducing: enhanced StudentHomeActivity (privacy overlay, quiz, session card, bottom nav), CounselorDashboardActivity, CounselorListActivity, BookingActivity, CalendarActivity, HistoryActivity, QuizActivity, and 4 adapters + 11 layouts. This work was partially complete — subsequent sprints formalized and completed it.

---

## 6. Sprint 2 — Counselor Directory and Specialization Tags (US-23, US-06)

**Duration:** Week 2 · **Points:** 13 (8 + 5)

**Stories:**
- US-23: *Browse a searchable, filterable directory of available counselors.*
- US-06: *Counselors list specialization tags so the triage system can match accurately.*

### What Was Built

| Deliverable | Type | Purpose |
|---|---|---|
| `CounselorRepository.java` | Repository | `getAllCounselors()`, `getCounselor()`, `updateSpecializations()`, `updateCounselorProfile()`. Document IDs attached post-deserialization. |
| `SpecializationTags.java` | Constants | 8 predefined tags + `ALL_TAGS` array. Display-friendly strings stored directly in Firestore — no mapping layer. |
| `CounselorProfileActivity.java` | Activity | Full profile view: name, bio, chips, on-leave amber card, booking button. |
| `CounselorProfileEditActivity.java` | Activity | Bio/language/gender fields, toggleable specialization chips, on-leave toggle with referral dropdown. |
| `CounselorListActivity.java` | Activity | Real-time name search (TextWatcher), multi-select specialization chips, language/gender dropdowns — all client-side filtering against a master list. |
| `CounselorAdapter.java` | Adapter | Name, specializations (dot-joined), on-leave badge. Tap → CounselorProfileActivity. |
| `CounselorTest.java` | Unit Test | 7 tests: onLeave default, specializations, empty constructor, setters, toggle, leave message, tags. |
| `CounselorDirectoryTest.java` | UI Test | 5 Espresso tests: core elements, language dropdown, gender dropdown, empty state, clear search. |

### Key Technical Decision

**Client-side filtering.** All counselors fetched once; filtering in memory against a `FilterState` object. Language and gender dropdowns populated dynamically from unique values in the live data — no maintenance burden.

---

## 7. Sprint 3 — Booking Flow, Dashboard, Emergency Button (US-01, US-05/10, US-20)

**Duration:** Week 3 · **Points:** 24 (13 + 8 + 3)

Sprint 3 collapsed two planned sprints. The three stories share `AppointmentRepository` — delivering them together was efficient.

### What Was Built

| Deliverable | Type | Purpose |
|---|---|---|
| `AppointmentRepository.java` | Repository | `bookAppointment()` atomic Firestore transaction (read slot → mark unavailable → create appointment). Three-way callback: `onSuccess`, `onSlotTaken`, `onFailure`. Cancel restores slot. |
| `AvailabilityRepository.java` | Repository | `getSlotsForCounselor()`, `getAvailableSlots()`, `addSlot()`, `removeSlot()`. |
| `AvailabilitySchedule.java` | Model | Local value object. `fromSlots()` groups TimeSlots by date. `getDatesWithAvailability()` for calendar highlighting. |
| `BookingActivity.java` | Activity | Calendar date selection → slots for date → `BookingConfirmationFragment` → atomic booking transaction. |
| `BookingConfirmationFragment.java` | Fragment | BottomSheet with booking summary and confirm/cancel. |
| `CustomCalendarView.java` | View | Custom calendar rendering with highlighted and selected date sets. |
| `CounselorDashboardActivity.java` | Activity | TabLayout (Today/Week/Month). Master appointment list filtered client-side. Stats cards properly differentiated. |
| `AvailabilitySetupActivity.java` | Activity | Counselor slot management: add via date/time pickers, swipe-to-delete. |
| `AppointmentAdapter.java` | Adapter | Student name lookup via UserRepository. Status badge styling. No-Show wired to `updateAppointmentStatus()`. |
| `EmergencyDialogFragment.java` | Fragment | Three dial buttons (Crisis Line, Campus Emergency, Dismiss). `ACTION_DIAL` — no CALL_PHONE permission. Offline capable. |

### Key Technical Decision

**Atomic booking transaction.** `db.runTransaction()` atomically reads slot availability, marks unavailable, creates appointment. Without this, two students could book the same slot simultaneously. Three-way callback distinguishes race loss (`onSlotTaken`) from general failure (`onFailure`).

### Data Flow: Atomic Booking

```
Student taps "Confirm" → BookingActivity.onConfirm(slot)
    → AppointmentRepository.bookAppointment(studentId, counselorId, slot, cb)
        → db.runTransaction():
            1. READ  slots/{slotId} → check available == true
            2. WRITE slots/{slotId}.available = false
            3. WRITE appointments/{newId} = {..., "CONFIRMED"}
        → success → cb.onSuccess() → navigate StudentHomeActivity
        → available==false → cb.onSlotTaken() → remove slot from UI
        → error → cb.onFailure() → show error toast
```

---

## 8. Sprint 4 — On-Leave Status and Anonymous Feedback (US-19, US-21)

**Duration:** Week 4 · **Points:** 10 (5 + 5)

### What Was Built

| Deliverable | Type | Purpose |
|---|---|---|
| `FeedbackService.java` | Model | No `studentId` field — anonymity enforced at schema level, not just UI. Verified by reflection in unit test. |
| `FeedbackRepository.java` | Repository | `submitFeedback()`, `hasFeedbackForAppointment()` (returns false on failure — shows prompt again rather than suppressing). |
| `CounselorRepository` extension | Repository | `setOnLeaveStatus()` uses `update()` (not `set()`) to only touch three leave fields without overwriting rest of counselor document. |
| On-leave UI in `CounselorProfileEditActivity` | Activity | SwitchMaterial toggle → message field + referral counselor dropdown (populated from getAllCounselors, excluding self). |
| Feedback wiring in `StudentHomeActivity` | Activity | `checkForPendingFeedback()` → `hasFeedbackForAppointment()` → show prompt card → `showFeedbackDialog()` → persist. |

### Key Technical Decision

**Anonymity at schema level.** The `FeedbackService` class has no `studentId` field whatsoever — not a getter returning null, but a genuinely absent field. Even direct Firestore inspection cannot trace feedback to a student.

---

## 9. Sprint 4.5 — Integration Refinements (US-02, US-03, US-04 partial)

**Duration:** Week 5 (partial)

### What Was Built

| Feature | Details |
|---|---|
| **Discreet Mode (US-02)** | Eye button in StudentHomeActivity toggles `privacyOverlay` to disguise counseling content on-screen. Overlay exit button restores normal view. |
| **Slide-to-Cancel refresh (US-03)** | After cancellation `clearSessionCard()` replaced with `fetchUpcomingSession()` — shows next confirmed appointment or hides card cleanly. |
| **Multi-step quiz (US-04 partial)** | QuizActivity expanded from 1-question picker to 3-question guided assessment with step counter, ProgressBar, and back navigation. |
| **Homepage filter chips** | ChipGroup programmatically built from `SpecializationTags.ALL_TAGS`. Each chip launches `CounselorListActivity(SPECIALIZATION=...)`. |
| **Homepage search bar** | EditText passes `SEARCH_QUERY` extra to `CounselorListActivity` which pre-fills and immediately filters. |

---

## 10. Sprint 5 — Intake Matching and Waitlist Foundation (US-04, US-24)

**Duration:** Week 6

### Starting State

The quiz existed but used a placeholder recommendation (first counselor found). No intake persistence, no real matching logic, no waitlist preference form, and no student-facing waitlist view had been built yet.

### What Was Built

| Deliverable | Type | Purpose |
|---|---|---|
| `IntakeAssessment.java` | Model | Stores quiz answers (primaryConcern, duration, supportType), urgencyLevel, recommendedSpecializations, matchedCounselorId. |
| `IntakeAssessmentRepository.java` | Repository | `saveAssessment()`, `getLatestForStudent()`, `getRecommendationCounts()` for admin analytics. |
| `IntakeMatcher.java` | Utility | `tagsForAnswers(answers)` maps quiz answer combinations to specialization tags. `scoreCounselor(counselor, tags)` computes tag overlap. `findBestCounselor(counselors, tags)` returns highest scorer, tie-broken by recommendation count. |
| `WaitlistEntry.java` | Model | studentId, counselorId, status (ACTIVE/RESOLVED/CANCELLED), requestedAt (FIFO key). |
| `WaitlistOffer.java` | Model | Links a WaitlistEntry to a specific offered slot. |
| `WaitlistRepository.java` | Repository | `joinWaitlist()` (dedup — no duplicate entries per counselor), `getActiveWaitlistForCounselorOrdered()`, `resolveEntry()`. |
| `QuizActivity` hardened | Activity | Saves IntakeAssessment, runs IntakeMatcher against live counselor data, routes to matched counselor's profile. |

### Key Technical Decision

**Specialization overlap scoring.** `IntakeMatcher.scoreCounselor()` counts how many of the student's mapped tags appear in the counselor's specializations list. Tie-breaking by `recommendationCounts` (how many times that counselor was previously matched) creates a virtuous cycle where well-matched counselors surface more often.

---

## 11. Sprint 6 — Counselor Workflow: Notes, Crisis, Student Profile (US-12–15, US-25)

**Duration:** Week 7

### What Was Built

| Deliverable | Type | Purpose |
|---|---|---|
| `StudentProfileActivity.java` | Activity | Counselor views student name, pronouns, latest intake assessment (concern + urgency), and session note history for this student. |
| `SessionHistoryActivity.java` | Activity | Chronological list of all appointments for a given student, viewable by counselor. |
| `SessionNote.java` | Model | appointmentId, counselorId, noteText, templateKey, appointmentDate. |
| `NoteTemplate.java` | Utility | 5 templates: Academic Stress, Anxiety, Follow-up, Crisis, General. `getTemplateText(key)` returns prefill text. |
| `SessionNoteRepository.java` | Repository | `saveNote()`, `updateNote()`, `getNoteForAppointment()`. |
| `CrisisEscalation.java` | Model | appointmentId, counselorId, studentId, severity (MODERATE/HIGH/IMMEDIATE), actionTaken, notes, resolved flag. |
| `CrisisEscalationRepository.java` | Repository | `createEscalation()` with batch write that stamps `crisisEscalationId` on the appointment. |
| `CrisisEscalationDialogFragment.java` | Dialog | Severity radio picker, action-taken field, notes field, Save/Cancel. |
| Dashboard waitlist count | `CounselorDashboardActivity` | `waitlistCount` stat card from WaitlistRepository (US-25). |
| Adapter wiring | `AppointmentAdapter` | Profile button → StudentProfileActivity, Notes button → session note dialog, Crisis button → CrisisEscalationDialogFragment. |

---

## 12. Sprint 7 — Buffer Time and Calendar Sync (US-08, US-09)

**Duration:** Week 8

### What Was Built

| Deliverable | Type | Purpose |
|---|---|---|
| `AvailabilitySettings.java` | Model | bufferMinutes (0/10/15/30), externalCalendarEnabled, calendarProvider. |
| `AvailabilitySettingsRepository.java` | Repository | `getSettings()`, `saveSettings()` — one document per counselor. |
| `AvailabilitySettingsActivity.java` | Activity | RadioGroup for buffer selection. Switches for calendar provider. |
| `BufferTimeValidator.java` | Utility | `hasConflict(slots, newSlot, bufferMin)` — checks that no existing slot falls within the buffer window of the proposed new slot. `isWithinBlockedWindow()` for time-gate checks. |
| `CalendarSyncHelper.java` | Utility | `buildInsertEventIntent(appt)` — constructs an `Intent.ACTION_INSERT` for Android's calendar app with appointment date/time/title pre-filled. |
| `AvailabilitySetupActivity` hardened | Activity | Loads buffer settings on start, calls `canAddSlotWithBuffer()` before adding a slot. |
| Waitlist on cancel | `AppointmentRepository` | `cancelAppointment()` now triggers `getNextActiveEntry()` → `markOffered()` for waitlist auto-offer. |

### Key Technical Decision

**Buffer validation at slot-add time (counselor side), not at booking time (student side).** If a slot exists and is marked `available`, it was already validated. Re-validating at booking would require loading all counselor slots for every booking attempt — expensive and adding latency to the critical booking path.

---

## 13. Sprint 8 — Waitlist Hardening: Preference Requests and Auto-Resolution

**Duration:** Week 9

### Problem

The Sprint 5 waitlist was structurally present but functionally minimal: no date/time preferences captured from students, no student-facing list view, no counselor queue view, and auto-offer on cancellation was a legacy mechanism. Students could join a waitlist but had no way to see their position or express when they wanted an appointment.

### What Was Built

| Deliverable | Type | Purpose |
|---|---|---|
| `WaitlistEntry` redesign | Model | Added: `preferredDates` (List\<String\>), `preferredStartTime`, `preferredEndTime`. Status simplified to ACTIVE/RESOLVED/CANCELLED. |
| `WaitlistMatcher.java` | Utility | `matches(entry, slot)` — returns true if slot date is in preferred dates AND slot time falls within preferred time window. `findFirstMatch(entry, slots)` finds first matching slot from available list. |
| `WaitlistRequestActivity.java` | Activity | Date multi-select via `CustomCalendarView`, time window picker (start/end popups), free-text note. Validates and submits a `WaitlistEntry`. |
| `StudentWaitlistActivity.java` | Activity | Student's personal waitlist tab. Lists active entries with counselor name and status. Cancel confirmation. |
| `WaitlistAdapter.java` | Adapter | Renders student waitlist entries. Status badge (ACTIVE/RESOLVED/CANCELLED). |
| `CounselorWaitlistActivity.java` | Activity | Counselor's waitlist queue — FIFO-ordered active entries with student names and preferences. "Create Slot" shortcut pre-populates date/time from top entry. |
| `CounselorWaitlistAdapter.java` | Adapter | Renders counselor waitlist queue entries. |
| Auto-resolution on slot creation | `AvailabilityRepository` | After adding a slot, `WaitlistMatcher.findFirstMatch()` runs against ACTIVE entries. If found, `WaitlistRepository.resolveEntry()` auto-books for the matched student. |

### Data Flow: Auto-Resolution

```
Counselor creates new slot (AddSlotActivity / GenerateSlotsActivity)
    → AvailabilityRepository.addSlot()
        → Firestore: slot written
        → WaitlistRepository.getActiveWaitlistForCounselorOrdered()
            → for each entry: WaitlistMatcher.matches(entry, newSlot)
                → first match found:
                    → AppointmentRepository.bookAppointment(...)
                    → WaitlistRepository.resolveEntry(entryId)
```

---

## 14. Sprint 9 — Dashboard Overhaul, Notes Hardening, No-Show Safeguards

**Duration:** Week 10

### Problems Addressed

| Problem | Fix |
|---|---|
| Session note always creates new — no upsert | `SessionNoteRepository.getNoteForAppointment()` → edit existing if found |
| No-show fires immediately on tap | Confirmation dialog added before status change |
| Cancelled/no-show appointments appear in active list | Active-only filter applied on appointment load |
| Note history shows save timestamp not session date | Note timestamp sourced from `appointmentDate` field |
| Stats cards count all-time totals | Live count from real-time listener, decrements on cancel |
| Logout without confirmation | AlertDialog confirmation before `FirebaseAuth.signOut()` |

### What Was Built

- Session note upsert: `SessionNoteRepository.updateNote()` used when note already exists for appointment.
- No-show time gate: cannot mark no-show if appointment time hasn't passed.
- No-show confirmation: orange-themed AlertDialog with "Mark No-Show" / "Cancel".
- Active-only appointment filter: `status == CONFIRMED` filter applied before displaying in active tab.
- Dashboard UI redesign: rounded cards, pink accent, cleaner layout.
- `item_appointment.xml` redesign: new layout with session info prominence.

---

## 15. Sprint 9.5 — Rolling Hardening: Cache, Slot Overhaul, Real-Time Listeners

**Duration:** Continuous between Sprint 9 and Sprint 10 (three phases)

### Phase A — Dashboard Hardening and Session Management

| Problem | Resolution |
|---|---|
| No way to delete a session note | Delete button added to note history dialog |
| Note history timestamp shows save time | Anchored to `appointmentDate` |
| Dashboard polls Firestore on every `onResume` | Snapshot listener replaces poll-on-resume |
| Appointment list forces full Firestore round-trip | `SessionCache` with background refresh |
| Two stale `loadAppointments()` calls | Deduplicated; single call path |

### Phase B — Slot Management Overhaul

| Problem | Resolution |
|---|---|
| Slot list is flat unformatted text | Multi-view-type adapter (date header + slot row) |
| Deleting a slot has no confirmation | AlertDialog: "Delete slot?" with date/time shown |
| Booked slots deletable like available ones | Booked slots show lock icon; delete disabled |
| No calendar/past-date guard on add slot | `AddSlotActivity` with `CustomCalendarView` and past-date rejection |
| No way to generate a full workday of slots | `GenerateSlotsActivity`: multi-date picker, start/end time, break windows, auto-generate |

### Phase C — Real-Time Listeners, Booking Guards, Past Sessions

| Problem | Resolution |
|---|---|
| Snapshot listener fires redundant initial callback | `isFirstLoad` guard skips initial callback |
| Waitlist count fetched unnecessarily | Lazy load: count fetched only when waitlist tab selected |
| Students can book two appointments same day | `getAppointmentsForStudentOnDate()` check before confirming booking |
| No-show time gate missing | Appointment time parsed; no-show disabled if appointment hasn't passed |
| No way to view past booked sessions | `PastSessionsActivity` with chronological completed/no-show list |
| Counselors can't change display name | Display name field added to `CounselorProfileEditActivity` |
| Expired slots (past dates, available=true) polluting available list | Filter: `available == true AND date >= today` |

### New Files Created

| File | Purpose |
|---|---|
| `AddSlotActivity.java` | Single-slot add with `CustomCalendarView` and past-date guard |
| `GenerateSlotsActivity.java` | Bulk slot generation with break windows |
| `PastSessionsActivity.java` | Counselor's past (completed/no-show) appointment list |
| `SessionNoteHistoryAdapter.java` | Renders note history in chronological order |
| `SessionCache.java` (hardened) | Singleton with per-collection invalidation; background refresh |

### Firestore Impact

New collection: `recommendationCounts/{counselorId}` — tracks how many intake assessments matched each counselor. Used by `IntakeMatcher` for tie-breaking.

### Test Coverage Added

39 new test methods across buffer validation, waitlist matching, intake matcher, session note repository, and booking guard logic.

---

## 16. Sprint 10 — Admin Reminders, Secure Messaging, Returning Student (US-16, US-17, US-18)

**Duration:** Week 11

### What Was Built

| Deliverable | Type | Purpose |
|---|---|---|
| `ReminderSettings.java` | Model | `enabled24Hour`, `enabled1Hour`, `message24Hour`, `message1Hour`. `defaultSettings()` factory. |
| `ReminderRecord.java` | Model | appointmentId, studentId, reminderType ("24_HOUR"/"1_HOUR"), scheduledFor, delivered. |
| `ReminderRepository.java` | Repository | `getSettings()`, `saveSettings()`, `getDueRemindersForStudent()`. |
| `ReminderScheduler.java` | Scheduler | `generateReminderRecords(appointments, settings)` → produces list of ReminderRecord objects to be persisted. |
| `ReminderSettingsActivity.java` | Activity | Admin form: 24-hour and 1-hour toggles with custom message fields. |
| `AdminDashboardActivity.java` | Activity | Admin landing page with reminder settings management and logout. |
| `SecureMessage.java` | Model | counselorId, studentId, senderId, messageText, sessionDate, read. `getThreadId()` builds `{counselorId}_{studentId}`. |
| `SecureMessageRepository.java` | Repository | `listenToThread()`, `sendMessage()`, `markThreadRead()`. |
| `MessageThreadActivity.java` | Activity | Full messaging UI: thread loaded on open, real-time subscription, send with sessionDate/Time context. |
| `SecureMessageAdapter.java` | Adapter | Renders messages with sender alignment and date dividers. |
| `ReturningStudentHelper.java` | Utility | `isReturningStudent(appointments)` — returns true if student has prior completed/no-show appointments. Called at booking to stamp `returningStudent` field. |

### Firestore Schema: Messaging (Sprint 10)

Sprint 10 implemented messaging as flat `secureMessages/{id}` keyed by appointment. This was replaced in Sprint 11 — see below.

---

## 17. Sprint 11 — Messaging Thread Architecture, Counselor UX Declutter, Crisis Admin

**Duration:** Week 12

### Problems Identified in Sprint 10

| Problem | Root Cause | Impact |
|---|---|---|
| Messages stored per-appointment, not per counselor-student pair | `secureMessages` flat collection | No cross-session thread view; full conversation history inaccessible |
| Counselor appointment cards cluttered with 6 action buttons | All actions inline on card face | Counselors can't quickly skim appointment list |
| Crisis escalations invisible to admins | `AdminDashboardActivity` only showed reminders | Escalations filed but never actioned |

### 17.1 Messaging Thread Architecture

**Firestore schema changed from per-appointment to per-thread:**

```
messageThreads/{counselorId_studentId}/        ← thread document
messageThreads/{counselorId_studentId}/messages/{id}  ← sub-collection
```

`SecureMessage.java` simplified: removed `appointmentId`, `receiverId`; added `sessionDate`, `sessionTime` so messages carry their session context without requiring appointment lookups.

`SecureMessageRepository.java` fully rewritten with thread-keyed paths and real-time snapshot listeners via `listenToThread()`.

`SecureMessageAdapter.java` now supports two view types: `TYPE_MESSAGE` and `TYPE_DATE_DIVIDER`. Session date dividers automatically inserted when message date changes within the thread.

### 17.2 Counselor Appointment Card Declutter

**`item_appointment.xml`** simplified to show only: student name, date/time, status badge, returning badge, trailing chevron. All action buttons removed from card face.

**`AppointmentAdapter.java`** stripped of action handlers. `itemView` click opens `ManageSessionActivity`.

**`ManageSessionActivity.java`** (new) — dedicated entry point for all appointment management actions, organized into sections:
- Student + session info card
- SESSION NOTES: RecyclerView of note history + add/edit note
- SESSION ACTIONS: Mark Attended, Mark No-Show, File Crisis Escalation, Open Message Thread

One-time crisis guard: `crisisEscalationId` field checked before allowing a second escalation on the same appointment.

### 17.3 Crisis Escalation Dialog Redesign

`CrisisEscalationDialogFragment.java` redesigned with:
- Custom white/pink card matching `EmergencyDialogFragment` style
- Severity radio chips (Moderate / High / Immediate)
- Action-taken radio group
- Two direct-dial emergency buttons
- Notes field
- `OnEscalationSavedListener` callback interface

### 17.4 Admin Crisis Alerts Tab

`AdminDashboardActivity` gains a `TabLayout` with two tabs:
- **Reminders** — existing reminder settings management
- **Crisis Alerts** — live feed of unresolved crisis escalations

`CrisisEscalationRepository` extended with `getUnresolvedEscalations()` and `markResolved()`.

`CrisisAlertAdapter.java` (new) — renders crisis alert cards with severity-colored strip, student/counselor info, and "Mark Resolved" button that updates Firestore.

---

## 18. Final Class Inventory

### Model Classes (Firestore-mapped)

| Class | Collection | Sprint |
|---|---|---|
| `Student` | `users` | 1 |
| `Counselor` | `counselors` | 1 (base), 2 (fields) |
| `Appointment` | `appointments` | Pre-existing |
| `TimeSlot` | `Slots/{uid}/slots` | Pre-existing |
| `FeedbackService` | `feedback` | 4 |
| `AvailabilitySchedule` | (local only) | 3 |
| `IntakeAssessment` | `intakeAssessments` | 5 |
| `WaitlistEntry` | `waitlistEntries` | 5, extended 8 |
| `WaitlistOffer` | `waitlistOffers` | 5 |
| `SessionNote` | `sessionNotes` | 6 |
| `CrisisEscalation` | `crisisEscalations` | 6 |
| `AvailabilitySettings` | `availabilitySettings` | 7 |
| `ReminderSettings` | `reminderSettings` | 10 |
| `ReminderRecord` | `reminderRecords` | 10 |
| `SecureMessage` | `messageThreads/.../messages` | 10 (model), 11 (schema) |

### Repository Classes

| Class | Sprint |
|---|---|
| `UserRepository` | 1 |
| `CounselorRepository` | 2 |
| `AppointmentRepository` | 3 |
| `AvailabilityRepository` | 3 |
| `FeedbackRepository` | 4 |
| `IntakeAssessmentRepository` | 5 |
| `WaitlistRepository` | 5 |
| `SessionNoteRepository` | 6 |
| `CrisisEscalationRepository` | 6 |
| `AvailabilitySettingsRepository` | 7 |
| `ReminderRepository` | 10 |
| `SecureMessageRepository` | 10 (rewritten 11) |

### Utility / Helper Classes

| Class | Purpose | Sprint |
|---|---|---|
| `UserRole` | Role string constants | 1 |
| `SpecializationTags` | 8 tags + ALL_TAGS | 2 |
| `IntakeMatcher` | Quiz answer → tag → counselor scoring | 5 |
| `WaitlistMatcher` | Preference → slot matching | 8 |
| `BufferTimeValidator` | Slot conflict detection | 7 |
| `CalendarSyncHelper` | Android calendar insert intent | 7 |
| `NoteTemplate` | 5 note templates with prefill text | 6 |
| `ReturningStudentHelper` | Prior appointment detection | 10 |
| `SessionCache` | In-memory Firestore cache singleton | 9.5 |
| `ReminderScheduler` | Generates ReminderRecord list from appointments | 10 |
| `AppToast` | Cross-activity toast scheduling | 9.5 |

### Activity Classes

| Class | Role | User | Sprint |
|---|---|---|---|
| `MainActivity` | Auth state check / splash | All | Pre-existing |
| `HomeActivity` | Legacy stub | — | Pre-existing |
| `LoginActivity` | Login + role routing | All | Pre-existing, 1 |
| `RegisterActivity` | Registration | All | 1 |
| `PrivacyPolicyActivity` | Policy gate | All | 1 |
| `StudentHomeActivity` | Student dashboard | Student | 1, enhanced throughout |
| `CounselorDashboardActivity` | Counselor dashboard | Counselor | Merged, 3, 9, 9.5, 10, 11 |
| `CounselorListActivity` | Counselor directory | Student | Merged, 2 |
| `CounselorProfileActivity` | Counselor profile view | Student | 2, 4 |
| `CounselorProfileEditActivity` | Profile editing | Counselor | 2, 4, 9.5 |
| `BookingActivity` | Slot selection + booking | Student | Merged, 3, 9.5 |
| `AvailabilitySetupActivity` | Slot management hub | Counselor | 3, 9.5 |
| `AddSlotActivity` | Single slot add | Counselor | 9.5 |
| `GenerateSlotsActivity` | Bulk slot generation | Counselor | 9.5 |
| `AvailabilitySettingsActivity` | Buffer + calendar settings | Counselor | 7 |
| `CalendarActivity` | Student appointment calendar | Student | Merged, 3 |
| `HistoryActivity` | Student appointment history | Student | Merged, 3 |
| `QuizActivity` | Intake quiz | Student | Merged, 4.5, 5 |
| `StudentProfileActivity` | Student triage view | Counselor | 6 |
| `SessionHistoryActivity` | Student session list | Counselor | 6 |
| `ManageSessionActivity` | Appointment actions hub | Counselor | 11 |
| `SessionNoteActivity` | Note editing + templates | Counselor | 6, 9 |
| `PastSessionsActivity` | Completed session list | Counselor | 9.5 |
| `MessageThreadActivity` | Secure messaging UI | Both | 10, 11 |
| `StudentWaitlistActivity` | Student waitlist entries | Student | 8 |
| `WaitlistRequestActivity` | Waitlist preference form | Student | 8 |
| `CounselorWaitlistActivity` | Counselor waitlist queue | Counselor | 8 |
| `ReminderSettingsActivity` | Admin reminder config | Admin | 10 |
| `AdminDashboardActivity` | Admin landing | Admin | 10, 11 |

### Fragment / Dialog Classes

| Class | Type | Sprint |
|---|---|---|
| `BookingConfirmationFragment` | BottomSheetDialogFragment | 3 |
| `EmergencyDialogFragment` | DialogFragment | 3 |
| `CrisisEscalationDialogFragment` | DialogFragment | 6, redesigned 11 |

### Adapter Classes

| Class | Binds | Sprint |
|---|---|---|
| `CounselorAdapter` | Counselor → card | Merged, 2 |
| `AppointmentAdapter` | Appointment → counselor card | Merged, 3, 9, 11 |
| `StudentAppointmentAdapter` | Appointment → student card | Merged |
| `TimeSlotAdapter` | TimeSlot → slot card | Merged |
| `WaitlistAdapter` | WaitlistEntry → student card | 8 |
| `CounselorWaitlistAdapter` | WaitlistEntry → counselor card | 8 |
| `CrisisAlertAdapter` | CrisisEscalation → admin card | 11 |
| `SecureMessageAdapter` | SecureMessage → message bubble | 10, 11 |
| `SessionNoteHistoryAdapter` | SessionNote → history row | 9.5 |

**Total: ~80 Java classes across 15 models, 12 repositories, 11 utilities, 27 activities, 3 dialogs, 9 adapters, 1 custom view, 2 legacy stubs.**

---

## 19. Test Coverage Summary

### Unit Tests (JUnit — `src/test/`)

| Test Class | Covers | Sprint |
|---|---|---|
| `StudentTest` | Student model | 1 |
| `CounselorTest` | Counselor model + SpecializationTags | 2 |
| `AppointmentTest` | Appointment model | 3 |
| `AvailabilityScheduleTest` | AvailabilitySchedule | 3 |
| `FeedbackServiceTest` | FeedbackService (anonymity via reflection) | 4 |
| `IntakeAssessmentTest` | IntakeAssessment model | 5 |
| `IntakeMatcherTest` | Matcher scoring + tag mapping | 5 |
| `WaitlistEntryTest` | WaitlistEntry model | 5 |
| `SessionNoteTest` | SessionNote model | 6 |
| `NoteTemplateTest` | NoteTemplate utility | 6 |
| `CrisisEscalationTest` | CrisisEscalation model | 6 |
| `AvailabilitySettingsTest` | AvailabilitySettings model | 7 |
| `BufferTimeValidatorTest` | Buffer conflict detection | 7 |
| `CalendarSyncHelperTest` | Calendar intent building | 7 |

### UI Tests (Espresso — `src/androidTest/`)

| Test Class | Covers | Sprint |
|---|---|---|
| `RegistrationFlowTest` | RegisterActivity validation | 1 |
| `LoginRoleRoutingTest` | LoginActivity role selection | 1 |
| `CounselorDirectoryTest` | CounselorListActivity filtering | 2 |
| `BookingFlowTest` | BookingActivity UI | 3 |
| `CounselorDashboardTest` | CounselorDashboardActivity | 3 |
| `EmergencyButtonTest` | EmergencyDialogFragment | 3 |
| `OnLeaveFlowTest` | On-leave UI end-to-end | 4 |
| `FeedbackFlowTest` | Feedback UI end-to-end | 4 |
| `IntakeQuizFlowTest` | QuizActivity full flow | 5 |
| `WaitlistFlowTest` | Waitlist join + student view | 5 |
| `StudentProfileFlowTest` | StudentProfileActivity | 6 |
| `SessionNotesFlowTest` | Session note dialog | 6 |
| `CrisisEscalationFlowTest` | Crisis dialog | 6 |
| `AvailabilitySettingsFlowTest` | Buffer settings UI | 7 |
| `BufferBookingFlowTest` | Buffer enforcement in add-slot | 7 |

**Total: ~40 unit tests, ~60 UI tests across 29 test files.**

---

## 20. User Story Traceability Matrix

| US | Title | Sprint | Status |
|---|---|---|---|
| US-01 | Booking Flow | 3 | **COMPLETE** |
| US-02 | Discreet Mode | 4.5 | **COMPLETE** |
| US-03 | Slide-to-Cancel | 4.5 | **COMPLETE** |
| US-04 | Intake Quiz + Matching | 4.5 (partial), 5 (complete) | **COMPLETE** |
| US-05/10 | Counselor Dashboard | 3, 9, 9.5 | **COMPLETE** |
| US-06 | Specialization Tags | 2 | **COMPLETE** |
| US-07 | Language Preferences | 2 | **COMPLETE** (filter + profile edit) |
| US-08 | Buffer Time | 7 | **COMPLETE** |
| US-09 | Calendar Sync | 7 | **COMPLETE** (Android calendar intent) |
| US-11 | No-Show Tracking | 3, 9, 9.5 | **COMPLETE** (time gate + confirmation) |
| US-12 | Session Notes | 6, 9 | **COMPLETE** (upsert) |
| US-13 | Session History | 6 | **COMPLETE** |
| US-14 | Student Profile / Triage View | 6 | **COMPLETE** |
| US-15 | Crisis Escalation | 6, 11 | **COMPLETE** (redesigned dialog) |
| US-16 | Admin Reminders | 10 | **COMPLETE** |
| US-17 | Returning Student Indicator | 10 | **COMPLETE** |
| US-18 | Secure Messaging | 10, 11 | **COMPLETE** (thread architecture) |
| US-19 | On-Leave Status | 4 | **COMPLETE** |
| US-20 | Emergency Button | 3 | **COMPLETE** |
| US-21 | Anonymous Feedback | 4 | **COMPLETE** |
| US-22 | Registration + Auth | 1 | **COMPLETE** |
| US-23 | Counselor Directory | 2 | **COMPLETE** |
| US-24 | Waitlist (join + resolve) | 5, 8 | **COMPLETE** |
| US-25 | Waitlist Count (counselor) | 6 | **COMPLETE** |

### Additional Stories Beyond Original Backlog

| ID | Title | Sprint | Notes |
|---|---|---|---|
| US-26 | Counselor Registration via App | changes.md | `RegisterActivity` creates both `users/{uid}` and `counselors/{uid}` — no Firebase console setup needed |
| US-27 | Student Calendar View | Merged + 3 | `CalendarActivity` + `CustomCalendarView` |
| US-28 | Student Appointment History | Merged + 3 | `HistoryActivity` — chronological past appointments |
| US-29 | Full Counselor Profile Edit | 2, 4, 9.5 | Bio, language, gender, specializations, on-leave, display name |
| US-30 | Bulk Slot Generation | 9.5 | `GenerateSlotsActivity` — multi-date, time range, break windows |
| US-31 | Admin Crisis Alerts | 11 | Unresolved escalation feed with resolve action |

---

## 21. Retrospective — Lessons Across Sprints

### What Went Well

1. **Repository pattern from day one.** Establishing `UserRepository` in Sprint 1 set the architectural standard. Every subsequent sprint followed it, making the data layer testable and consistent.

2. **Integration-first approach.** Firestore connectivity was live from Sprint 1. No feature was ever UI-only — every screen had real Firestore reads/writes.

3. **Incremental story delivery.** Each sprint delivered a demonstrable end-to-end capability. The app was always in a runnable state.

4. **Concurrent development integration.** Teammate Shahbaz's parallel work was substantial. Rather than discarding it, the team identified what was complete, what was partial, and built the missing layers on top.

5. **Feature depth over breadth.** When features were built (waitlist, messaging), they were built properly — with preference inputs, auto-resolution, thread architecture — rather than minimal stubs.

### What Could Be Improved

1. **Collection naming alignment.** PLAN.md specified `timeSlots` but implementation used `slots`. Caught and standardized in Sprint 3, but earlier alignment would have avoided confusion.

2. **N+1 query pattern in adapters.** Both `AppointmentAdapter` (student name) and `StudentAppointmentAdapter` (counselor name) make one Firestore read per list item. A batch-fetch strategy would scale better. Partially addressed by `SessionCache`.

3. **Sprint 3 scope creep.** Collapsing booking + dashboard + emergency into one sprint was successful (shared repository dependency) but made it the largest sprint (24 points). Better to split with shared repos planned upfront.

4. **Messaging architecture revision.** The Sprint 10 per-appointment messaging design required a full rewrite in Sprint 11 to support per-thread architecture. The thread-keyed design should have been the initial design.

### Architecture Evolution

```
Sprint 1:  [Login/Register] → [UserRepository] → Firestore /users
                                                        ↑
Sprint 2:  [CounselorListActivity] → [CounselorRepository] → /counselors
                                                        ↑
Sprint 3:  [BookingActivity] → [AppointmentRepository] → /appointments
           [BookingActivity] → [AvailabilityRepository] → /slots (atomic transaction)
                                                        ↑
Sprint 4:  [StudentHomeActivity] → [FeedbackRepository] → /feedback
           [ProfileEditActivity] → [CounselorRepository] (extended)
                                                        ↑
Sprint 5:  [QuizActivity] → [IntakeAssessmentRepository] → /intakeAssessments
           [ProfileActivity] → [WaitlistRepository] → /waitlistEntries
                                                        ↑
Sprint 6:  [ManageSession] → [SessionNoteRepository] → /sessionNotes
           [ManageSession] → [CrisisEscalationRepository] → /crisisEscalations
                                                        ↑
Sprint 7:  [AvailabilitySetup] → [AvailabilitySettingsRepository] → /availabilitySettings
                                                        ↑
Sprint 8:  [WaitlistRequest] → [WaitlistRepository] (preference matching)
           [CounselorWaitlist] → auto-resolution on slot create
                                                        ↑
Sprints 9–9.5: [Dashboard] → real-time snapshot listener (not poll)
               [BookingActivity] → [SessionCache] (avoids redundant reads)
                                                        ↑
Sprint 10: [AdminDashboard] → [ReminderRepository] → /reminderSettings
           [MessageThread] → [SecureMessageRepository] → /secureMessages (flat, replaced)
                                                        ↑
Sprint 11: [MessageThread] → [SecureMessageRepository] → /messageThreads/{id}/messages
           [ManageSession] → all appointment actions consolidated
           [AdminDashboard] → [CrisisEscalationRepository] → /crisisEscalations (admin tab)
```

Each sprint added a repository or extended an existing one. By Sprint 11, 12 repositories cover 12 Firestore collections, and every Activity accesses data exclusively through this layer.

---

*This document is the Phase 4 final deliverable. It consolidates all sprint implementation records from SPRINT-1.md through SPRINT-11.md, the PLAN.md architectural foundation, SPRINT_1-7_STATUS_REPORT.md audit, and changes.md data-layer fixes. For full implementation code (method signatures, layout XML, string resources, test cases), refer to the individual sprint documents in `documentation/sprints/`.*
