# BetterCAPS — Phase 3 Development Cycle
### Complete Sprint History and Implementation Record

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Team and Process](#2-team-and-process)
3. [Architecture and Design Patterns](#3-architecture-and-design-patterns)
4. [Firestore Data Model](#4-firestore-data-model)
5. [Sprint 1 — Registration and Authentication (US-22)](#5-sprint-1--registration-and-authentication-us-22)
6. [Sprint 2 — Counselor Directory and Specialization Tags (US-23, US-06)](#6-sprint-2--counselor-directory-and-specialization-tags-us-23-us-06)
7. [Sprint 3 — Booking Flow, Dashboard, Emergency Button (US-01, US-05/US-10, US-20)](#7-sprint-3--booking-flow-dashboard-emergency-button-us-01-us-0510-us-20)
8. [Sprint 4 — On-Leave Status and Anonymous Feedback (US-19, US-21)](#8-sprint-4--on-leave-status-and-anonymous-feedback-us-19-us-21)
9. [Sprint 4.5 — Integration Refinements (US-03, US-04, Homepage Wiring)](#9-sprint-45--integration-refinements-us-03-us-04-homepage-wiring)
10. [Final Class Inventory](#10-final-class-inventory)
11. [Test Coverage Summary](#11-test-coverage-summary)
12. [User Story Traceability Matrix](#12-user-story-traceability-matrix)
13. [Retrospective — Lessons Across Sprints](#13-retrospective--lessons-across-sprints)

---

## 1. Project Overview

**BetterCAPS** is a university counseling appointment platform built as an Android application with Firebase (Authentication + Firestore) as the backend. The app serves three user roles — Students, Counselors, and Administrators — and implements the core student-counselor appointment lifecycle: registration, counselor discovery, booking, session management, and post-session feedback.

Phase 3 represents the **half-way checkpoint**, delivering a working prototype that covers approximately half of the 25+ user stories in the product backlog. The TA builds and runs the app directly from the repository — it must compile and function without manual intervention.

### Phase 3 Definition of Done

- A user can register, log in, browse counselors, and successfully book an appointment — all persisted in Firestore.
- A counselor can log in, see their appointments on a tabbed dashboard, and manage their availability.
- An emergency contact button is accessible from the home screen at all times.
- Counselors can toggle on-leave status with referral routing.
- Students can submit anonymous post-session feedback.
- All implemented features have corresponding unit and UI tests.
- Every model class has Javadoc documentation.

### Stories Implemented in Phase 3

| Tier | Stories | Points | Sprint |
|---|---|---|---|
| **Core** | US-22 (Registration + Auth) | 8 | Sprint 1 |
| **Core** | US-23 (Counselor Directory), US-06 (Specialization Tags) | 8 + 5 | Sprint 2 |
| **Core** | US-01 (Booking Flow) | 13 | Sprint 3 |
| **Core** | US-05/US-10 (Counselor Dashboard) | 8 | Sprint 3 |
| **Core** | US-20 (Emergency Button) | 3 | Sprint 3 |
| **Secondary** | US-19 (On-Leave Status) | 5 | Sprint 4 |
| **Secondary** | US-21 (Anonymous Feedback) | 5 | Sprint 4 |
| **Integration** | US-03 (Slide-to-Cancel), US-04 (Quiz), Homepage Wiring | — | Sprint 4.5 |

**Total story points delivered:** 55

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
┌─────────────────────────────────────────┐
│           UI Layer (Activities)          │
│  LoginActivity, RegisterActivity,        │
│  StudentHomeActivity, BookingActivity,   │
│  CounselorDashboardActivity, ...         │
└──────────────┬──────────────────────────┘
               │ calls
┌──────────────▼──────────────────────────┐
│         Repository Layer                 │
│  UserRepository, CounselorRepository,   │
│  AppointmentRepository,                 │
│  AvailabilityRepository,                │
│  FeedbackRepository                     │
└──────────────┬──────────────────────────┘
               │ reads/writes
┌──────────────▼──────────────────────────┐
│         Firestore (Firebase)             │
│  /users  /counselors  /slots             │
│  /appointments  /feedback                │
└─────────────────────────────────────────┘
```

### Design Patterns Used

| Pattern | Where Applied | Why |
|---|---|---|
| **Repository** | UserRepository, CounselorRepository, AppointmentRepository, AvailabilityRepository, FeedbackRepository | Isolates data layer from UI. Activities are hard to unit test because they depend on Android runtime. Repositories can be mocked. |
| **Callback** | All repository methods use inner interface callbacks (e.g., `OnBookingCallback`) | Firestore operations are asynchronous. Callbacks decouple the caller from the async result. |
| **Factory Method** | `AvailabilitySchedule.fromSlots()`, `EmergencyDialogFragment.newInstance()`, `BookingConfirmationFragment.newInstance()` | Encapsulates construction logic, enforces required parameters. |
| **Adapter** | CounselorAdapter, AppointmentAdapter, StudentAppointmentAdapter, TimeSlotAdapter | Standard RecyclerView pattern for binding data to list views. |
| **Value Object** | AvailabilitySchedule | Local, non-persisted data structure that groups TimeSlots by date for calendar rendering. |
| **Constants Class** | UserRole, SpecializationTags | Single source of truth for string constants. Prevents typos across the codebase. |

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
counselors/{counselorId}
  ├── uid: String               // matches users/{uid}
  ├── name: String
  ├── bio: String
  ├── specializations: List<String>
  ├── language: String
  ├── gender: String
  ├── onLeave: Boolean
  ├── onLeaveMessage: String
  └── referralCounselorId: String
```

### Collection: `slots`
```
slots/{slotId}
  ├── counselorId: String
  ├── date: String              // "yyyy-MM-dd"
  ├── time: String              // "HH:mm"
  └── available: Boolean
```

### Collection: `appointments`
```
appointments/{appointmentId}
  ├── studentId: String
  ├── counselorId: String
  ├── slotId: String
  ├── date: String
  ├── time: String
  └── status: String            // "CONFIRMED" | "COMPLETED" | "CANCELLED" | "NO_SHOW"
```

### Collection: `feedback`
```
feedback/{feedbackId}
  ├── appointmentId: String
  ├── rating: int               // 1–5
  ├── comment: String
  └── submittedAt: Timestamp
  // NOTE: no studentId field — anonymity enforced at schema level
```

**Schema decision:** The original PLAN.md specified `timeSlots` as the collection name. During development, teammate Shahbaz implemented the booking flow using `slots`. Since live data was already persisted under `slots`, Sprint 3 standardized on this name across all repository code.

---

## 5. Sprint 1 — Registration and Authentication (US-22)

**Duration:** Week 1
**Points:** 8
**Story:** *As a student, I want to register using my university email, set my preferred name and pronouns, and be guided through the platform's privacy policy, so that I am formally verified and fully informed before using the service.*

### Starting State

The project began with a functional `LoginActivity` (Firebase Auth sign-in, role selector buttons, animated transitions) and three empty stubs (`RegisterActivity`, `HomeActivity`, and their layouts). The `Counselor`, `TimeSlot`, and `Appointment` models existed. `Counselor.java` had a package declaration mismatch (`com.mooger.moogerscouncil` instead of `com.example.moogerscouncil`).

### What Was Built

| Deliverable | Type | Purpose |
|---|---|---|
| `Student.java` | Model | Firestore-mapped data container for the `users` collection. Fields: uid, name, email, preferredName, pronouns, role, createdAt. No-arg constructor for Firestore deserialization. Role defaults to `UserRole.STUDENT`. |
| `UserRole.java` | Constants | String constants: STUDENT, COUNSELOR, ADMIN. Private constructor prevents instantiation. |
| `UserRepository.java` | Repository | Wraps all `users` collection operations. Methods: `createUser()`, `getCurrentUser()`, `getCurrentUserRole()`. Three callback interfaces for async results. |
| `RegisterActivity.java` | Activity | Full registration form: name, email (@lums.edu.pk domain validation), preferred name, pronouns, password (6+ chars), confirm password. Firebase Auth creates account → `UserRepository.createUser()` writes Firestore doc → navigates to `PrivacyPolicyActivity`. |
| `PrivacyPolicyActivity.java` | Activity | Scrollable privacy policy text from `strings.xml`. "I Agree" button gates entry. Routes to `StudentHomeActivity` or `CounselorDashboardActivity` based on role intent extra. |
| `StudentHomeActivity.java` | Activity | Welcome screen with preferred name (fetched via `UserRepository.getCurrentUser()`). Logout button calls `FirebaseAuth.signOut()`. |
| `LoginActivity.java` modifications | Activity | Post-login role-based routing via `UserRepository.getCurrentUserRole()`: students → `StudentHomeActivity`, counselors → `CounselorDashboardActivity`. Auto-redirect on app relaunch if session exists. Pass selected role to `RegisterActivity` via intent extra. |
| `Counselor.java` package fix | Model | Changed `package com.mooger.moogerscouncil` → `package com.example.moogerscouncil`. |
| `strings.xml` additions | Resources | Registration hints, validation errors, privacy policy text, student home strings. |
| `StudentTest.java` | Unit Test | 5 tests: default role, constructor assignment, empty constructor, setter overrides, createdAt timestamp. |
| `RegistrationFlowTest.java` | UI Test | 5 Espresso tests: form fields visible, invalid email error, password mismatch error, short password error, empty name error. |
| `LoginRoleRoutingTest.java` | UI Test | 4 Espresso tests: role buttons displayed, button text updates on selection, sign-up link visible. |

### Key Technical Decisions

- **Email domain validation is client-side only.** The app checks that the email ends with `@lums.edu.pk` before submitting to Firebase Auth. Server-side rejection would require Cloud Functions, which is out of scope.
- **Role is stored in Firestore, not Firebase Auth custom claims.** This avoids the need for a token refresh after role assignment and makes the role readable with a simple document fetch.
- **Sequential auth + Firestore write.** The Firebase Auth call must complete before the Firestore write because the Firestore document ID is the Auth UID.

### Data Flow: Registration

```
User fills form → validateInputs() (local)
    → FirebaseAuth.createUserWithEmailAndPassword()
        → onSuccess: uid = authResult.getUid()
            → Student s = new Student(uid, name, email, preferredName, pronouns)
            → UserRepository.createUser(s, callback)
                → Firestore: users/{uid}.set(s)
                    → onSuccess: navigate to PrivacyPolicyActivity
```

### Concurrent Teammate Work (Merged Between Sprint 1 and Sprint 2)

Between Sprint 1 and Sprint 2, teammate Shahbaz merged work from branches `shahbaz/student-dash`, `shahbaz/counselor-dashboard`, and `shahbaz/booking-and-dashboard`. This introduced:

- **StudentHomeActivity** enhancements: privacy overlay (5s PII masking), crisis support banner, "Find My Match" quiz, upcoming session card with slide-to-cancel, feedback dialog (UI only), bottom navigation
- **CounselorDashboardActivity**: counselor greeting, 3-column stats, add availability slot, appointment RecyclerView
- **CounselorListActivity**: counselor browsing with specialty intent filter
- **BookingActivity**: time slot selection with atomic Firestore booking transaction
- **CalendarActivity**, **HistoryActivity**, **QuizActivity**: student calendar, history, quiz views
- **4 adapters**: CounselorAdapter, AppointmentAdapter, StudentAppointmentAdapter, TimeSlotAdapter
- **11 layout XMLs** for all the above screens

This work was partially complete — many features had UI but lacked backend wiring, repository abstraction, or full functionality. Subsequent sprints formalized and completed these implementations.

---

## 6. Sprint 2 — Counselor Directory and Specialization Tags (US-23, US-06)

**Duration:** Week 2
**Points:** 13 (8 + 5)
**Stories:**
- US-23: *As a student, I want to browse a searchable directory of all available counselors and filter by specialization, language, and gender.*
- US-06: *As a counselor, I want to list specific areas of focus as selectable tags on my profile so that the triage system can match me accurately.*

### Starting State

Sprint 1 was complete. The merged teammate work provided `CounselorListActivity` (basic specialty filtering), `CounselorAdapter` (hardcoded ratings, no on-leave badge), and `BookingActivity` (partial). No `CounselorRepository`, no `CounselorProfileActivity`, no `CounselorProfileEditActivity`, no `SpecializationTags` constants class.

### What Was Built

| Deliverable | Type | Purpose |
|---|---|---|
| `CounselorRepository.java` | Repository | Wraps all `counselors` collection operations. Methods: `getAllCounselors()`, `getCounselor()`, `updateSpecializations()`, `updateCounselorProfile()`. Document IDs attached after Firestore deserialization. |
| `SpecializationTags.java` | Constants | 8 predefined tags: General Anxiety, Academic Stress, Grief, Relationships, Career Guidance, Depression, Trauma, Family Issues. `ALL_TAGS` array for programmatic chip group generation. |
| `CounselorProfileActivity.java` | Activity | Full counselor profile view: name, bio, specialization chips (read-only), language, gender. On-leave amber card with custom message. "Book Appointment" button disabled when on-leave, routes to `BookingActivity` when available. |
| `CounselorProfileEditActivity.java` | Activity | Counselor edits their profile: bio, language, gender text fields. Toggleable specialization chips from `SpecializationTags.ALL_TAGS`. Pre-fills existing values from Firestore. Save persists via `CounselorRepository.updateCounselorProfile()`. |
| `CounselorListActivity.java` enhancements | Activity | Real-time name search (TextWatcher). Multi-select specialization chips (programmatically built from `SpecializationTags.ALL_TAGS`). Language and gender dropdown filters (dynamically populated from loaded counselor data). Empty-state message when no counselors match. Client-side `FilterState` object + `applyFilters()`. |
| `CounselorAdapter.java` enhancements | Adapter | Shows name, specializations (dot-joined), language. On-leave badge. Card tap navigates to `CounselorProfileActivity`. `setData()` method for filter updates. |
| `Counselor.java` field additions | Model | Added: bio, language, gender, onLeave (Boolean), onLeaveMessage, referralCounselorId. All getters/setters for Firestore mapping. |
| `CounselorDashboardActivity.java` update | Activity | Added "Edit Profile" button routing to `CounselorProfileEditActivity`. |
| `CounselorTest.java` | Unit Test | 7 tests: onLeave default, specializations set/get, empty constructor, field setters, onLeave toggle, leave message, SpecializationTags verification. |
| `CounselorDirectoryTest.java` | UI Test | 5 Espresso tests: core elements display, language dropdown, gender dropdown, search empty state, clearing search restores list. |

### Key Technical Decisions

- **Client-side filtering.** All counselors are fetched once from Firestore. Filtering happens in memory against the master list. This avoids repeated Firestore queries and keeps the UI responsive. A `FilterState` plain object tracks the current filter selections; `applyFilters()` produces a filtered copy on every change.
- **Dropdown population from data, not constants.** Language and gender dropdowns are populated dynamically from the unique values in the fetched counselor list. This means the dropdowns always reflect what's actually in the database, with no maintenance burden.
- **Display-friendly tag strings.** `SpecializationTags` uses display-friendly values ("Academic Stress") rather than enum-style values ("ACADEMIC_STRESS"). These are the exact strings stored in Firestore and shown to users — no mapping layer needed.

### Data Flow: Counselor Directory Filtering

```
CounselorListActivity.onCreate()
    → CounselorRepository.getAllCounselors(callback)
        → Firestore: counselors.get()
        → onSuccess(counselorList):
            masterList = counselorList
            populateFilterDropdowns(masterList)  // unique languages, genders
            buildSpecializationChips(intentSpec)  // from SpecializationTags.ALL_TAGS
            applyFilters()  // initial render

User types in search / toggles chip / selects dropdown:
    → filterState updated
    → applyFilters()
        → filtered = masterList.stream()
            .filter(name match)
            .filter(specialization match)
            .filter(language match)
            .filter(gender match)
            .collect()
        → adapter.setData(filtered)
        → empty state visibility toggle
```

---

## 7. Sprint 3 — Booking Flow, Dashboard, Emergency Button (US-01, US-05/US-10, US-20)

**Duration:** Week 3
**Points:** 24 (13 + 8 + 3)
**Stories:**
- US-01: *As a student, I want to view a counselor's available time slots on a calendar so that I can book an appointment that fits my schedule.*
- US-05/US-10: *As a counselor, I want to view a tabbed dashboard (Today/Week/Month) of my upcoming appointments.*
- US-20: *As a student, I want a clearly visible emergency button that connects me to campus crisis services.*

Sprint 3 collapsed the original PLAN.md Sprints 3 and 4 into a single sprint. The merged teammate work had already partially implemented booking and dashboard functionality. Completing them together was efficient because `AppointmentRepository` serves both US-01 (booking writes) and US-05/US-10 (dashboard reads).

### Starting State

Sprint 2 was complete. The existing `BookingActivity` (~40%) had the atomic Firestore transaction but no calendar UI, no confirmation step, and used direct Firestore calls. `CounselorDashboardActivity` (~80%) had stats that showed the same count three times, no tab filtering, and direct Firestore calls. An inline crisis banner existed in `StudentHomeActivity` but not as a proper `DialogFragment`.

### What Was Built

| Deliverable | Type | Purpose |
|---|---|---|
| `AppointmentRepository.java` | Repository | Central access for the `appointments` collection. `bookAppointment()` runs an atomic Firestore transaction: reads slot availability → marks slot unavailable → creates appointment doc — all in one transaction preventing double-booking. Three-way callback: `onSuccess()`, `onSlotTaken()`, `onFailure()`. Additional methods: `getAppointmentsForCounselor()`, `getAppointmentsForStudent()`, `getAppointmentsForStudentOnDate()`, `updateAppointmentStatus()`, `cancelAppointment()` (marks CANCELLED + restores slot), `getCompletedAppointmentsNeedingFeedback()`. |
| `AvailabilityRepository.java` | Repository | Wraps all `slots` collection operations. Methods: `getSlotsForCounselor()`, `getAvailableSlotsForCounselor()`, `addSlot()`, `removeSlot()`. |
| `AvailabilitySchedule.java` | Model | Local (non-Firestore) value object that groups `TimeSlot` objects by date. Factory method `fromSlots()` filters to available-only slots and builds a `Map<String, List<TimeSlot>>`. `getDatesWithAvailability()` returns dates for calendar highlighting. `getSlotsForDate()` returns slots for a tapped date. |
| `BookingActivity.java` rebuild | Activity | CalendarView with date selection. On date tap, shows available slots for that date via `AvailabilitySchedule.getSlotsForDate()`. Slot tap opens `BookingConfirmationFragment`. Uses `AppointmentRepository.bookAppointment()` for atomic transaction. Handles slot-taken race condition by removing the slot from UI and showing an error. |
| `BookingConfirmationFragment.java` | Fragment | `BottomSheetDialogFragment` showing booking summary (counselor name, date, time). "Confirm Booking" button triggers the `OnConfirmListener` callback which fires the transaction in `BookingActivity`. |
| `CounselorDashboardActivity.java` overhaul | Activity | `TabLayout` with Today / This Week / This Month tabs. Master appointment list loaded once via `AppointmentRepository.getAppointmentsForCounselor()`, filtered client-side by date range per tab. Stats cards: Today session count, Total patients, This Week count — properly differentiated. Add-slot banner routes to `AvailabilitySetupActivity`. `onResume()` refreshes data. |
| `AppointmentAdapter.java` enhancements | Adapter | Student name lookup via `UserRepository.getUserName()` (graceful degradation — returns "Unknown" on failure). Status badge styling: CONFIRMED (green), COMPLETED (grey), CANCELLED/NO_SHOW (red with strikethrough). No-Show button wired to `AppointmentRepository.updateAppointmentStatus()`. |
| `AvailabilitySetupActivity.java` | Activity | Counselor slot management. Add slot via DatePickerDialog → TimePickerDialog → `AvailabilityRepository.addSlot()`. Slot list with swipe-to-delete → `AvailabilityRepository.removeSlot()`. Inner `TimeSlotSetupAdapter` class for rendering slot items. |
| `EmergencyDialogFragment.java` | Fragment | `DialogFragment` with three buttons: Call Crisis Line, Campus Emergency, Dismiss. Uses `Intent.ACTION_DIAL` (opens dialer, doesn't auto-call). Phone numbers from `strings.xml`. Zero network dependency — works offline. |
| `StudentHomeActivity.java` update | Activity | Emergency FAB and crisis banner wired to `EmergencyDialogFragment`. |
| `UserRepository.java` extension | Repository | Added `getUserName(uid, callback)` — fetches only the name field. Returns "Unknown" on failure (name is supplementary display data). |
| `CalendarActivity.java` refactor | Activity | Refactored to use `AppointmentRepository.getAppointmentsForStudentOnDate()` instead of direct Firestore calls. |
| `HistoryActivity.java` refactor | Activity | Refactored to use `AppointmentRepository.getAppointmentsForStudent()` instead of direct Firestore calls. |
| `AvailabilityScheduleTest.java` | Unit Test | 5 tests: filters unavailable, correct date subset, empty for no slots, dates with availability set, empty list produces empty schedule. |
| `AppointmentTest.java` | Unit Test | Tests for empty constructor, setters, status values. |
| `BookingFlowTest.java` | UI Test | Espresso tests: calendar displayed, counselor name title, slot list appears on date tap. |
| `CounselorDashboardTest.java` | UI Test | 6 Espresso tests: TabLayout displayed, appointment list loads, Today/Week tabs present, tab switch changes content, add-slot banner displayed. |
| `EmergencyButtonTest.java` | UI Test | 5 Espresso tests: FAB visible, dialog opens on tap, crisis line option, campus security option, dismiss button. |

### Key Technical Decisions

- **Atomic booking transaction.** `AppointmentRepository.bookAppointment()` uses `db.runTransaction()` to atomically check slot availability, mark the slot unavailable, and create the appointment document. A simple write without a transaction would allow two students to book the same slot simultaneously.
- **Three-way booking callback.** The `OnBookingCallback` interface has three methods: `onSuccess()`, `onSlotTaken()`, and `onFailure()`. This distinguishes between a race-condition loss (slot taken by another user — show a specific message and remove the slot from UI) versus a general error (network failure — show a generic error).
- **`ACTION_DIAL` vs `ACTION_CALL`.** The emergency button uses `ACTION_DIAL` which opens the dialer with the number pre-filled but does not make the call. This avoids the `CALL_PHONE` permission and gives the user a confirmation step — appropriate for preventing accidental calls.
- **Repository pattern fully enforced.** After Sprint 3, no Activity anywhere in the codebase makes direct `db.collection()` calls. All Firestore access flows through one of the five repositories.

### Data Flow: Atomic Booking Transaction

```
Student taps "Confirm Booking" in BookingConfirmationFragment
    → BookingActivity.onConfirm(slot)
        → AppointmentRepository.bookAppointment(studentId, counselorId, slot, callback)
            → db.runTransaction():
                1. READ:  slots/{slotId} → check available == true
                2. WRITE: slots/{slotId}.available = false
                3. WRITE: appointments/{newId} = {studentId, counselorId, slotId, date, time, "CONFIRMED"}
            → Transaction success → callback.onSuccess()
                → Toast "Booked!" → navigate to StudentHomeActivity
            → Transaction fail (available == false) → callback.onSlotTaken()
                → Toast "Slot taken" → remove slot from UI → refresh
            → Transaction error → callback.onFailure()
                → Toast error message
```

### Data Flow: Dashboard Tab Filtering

```
CounselorDashboardActivity.loadAppointments()
    → AppointmentRepository.getAppointmentsForCounselor(counselorId, callback)
        → onSuccess(appointments):
            masterAppointments = appointments
            filterByTab(0)  // default to Today

TabLayout.onTabSelected(tab):
    → filterByTab(tab.getPosition())
        → switch:
            case 0 (Today):  filter where date == today
            case 1 (Week):   filter where date in [weekStart, weekEnd]
            case 2 (Month):  filter where date starts with "yyyy-MM"
        → adapter.setData(filtered)
        → updateStats(todayCount, totalCount, filteredCount)
```

---

## 8. Sprint 4 — On-Leave Status and Anonymous Feedback (US-19, US-21)

**Duration:** Week 4
**Points:** 10 (5 + 5)
**Stories:**
- US-19: *As a counselor, I want to set a temporary 'on leave' status with a custom message and a colleague referral so that students are gracefully redirected.*
- US-21: *As a student, I want to submit optional, fully anonymous post-session feedback after each appointment.*

### Starting State

All six core stories were complete and tested. The `Counselor` model already had `onLeave`, `onLeaveMessage`, and `referralCounselorId` fields (added in Sprint 2). The student-facing on-leave display (amber card, disabled booking) was working. But counselors had no way to edit their on-leave status — `CounselorProfileEditActivity` only had bio/language/gender/specialization fields. For feedback, `StudentHomeActivity` had a feedback dialog (RatingBar + comment) but it only showed a toast on submit — no Firestore persistence, no model class, no repository.

### What Was Built

| Deliverable | Type | Purpose |
|---|---|---|
| `FeedbackService.java` | Model | Firestore-mapped data container for the `feedback` collection. Fields: id, appointmentId, rating (1–5), comment (optional), submittedAt. **Intentionally has no `studentId` field** — anonymity is enforced at the schema level, not just the UI. |
| `FeedbackRepository.java` | Repository | Wraps all `feedback` collection operations. `submitFeedback()` persists a `FeedbackService` object. `hasFeedbackForAppointment()` uses `limit(1)` as an existence check — on failure returns `false` (shows prompt again rather than suppressing it). |
| `CounselorRepository.java` extension | Repository | Added `setOnLeaveStatus(counselorId, onLeave, leaveMessage, referralId, callback)`. Uses `update()` (not `set()`) to only touch the three leave fields without overwriting the rest of the counselor document. |
| `AppointmentRepository.java` extension | Repository | Added `getCompletedAppointmentsNeedingFeedback(studentId, callback)`. Queries for appointments with `status == "COMPLETED"`. |
| `CounselorProfileEditActivity.java` additions | Activity | Added on-leave section: `SwitchMaterial` toggle, leave message `TextInputLayout` (visible when toggle on), referral counselor dropdown (`AutoCompleteTextView` populated from `CounselorRepository.getAllCounselors()`, excluding self). Pre-fills existing leave state. On save, writes leave fields to Firestore. |
| `CounselorProfileActivity.java` addition | Activity | Added "See Referred Counselor" button inside the on-leave card. Visible when `referralCounselorId` is set. Navigates to the referred counselor's profile. |
| `CounselorAdapter.java` fix | Adapter | Fixed on-leave behavior: on-leave counselors now show "Currently Away" chip but their profile is still viewable (booking is disabled in the profile, not at the list level). |
| `StudentHomeActivity.java` feedback wiring | Activity | `checkForPendingFeedback()` on activity load: queries completed appointments → checks each against `FeedbackRepository.hasFeedbackForAppointment()` → shows dismissible feedback prompt card for the most recent unreviewed appointment. `showFeedbackDialog(appointmentId)` persists to Firestore via `FeedbackRepository.submitFeedback()`. |
| `activity_student_home.xml` addition | Layout | Feedback prompt card: title, counselor name subtitle, "Give Feedback" and "Dismiss" buttons. |
| `FeedbackServiceTest.java` | Unit Test | Tests: no studentId field exists (verified via reflection), constructor sets fields, empty constructor, rating bounds, null comment allowed. |
| `OnLeaveFlowTest.java` | UI Test | Tests: on-leave toggle displayed, toggle shows message field, toggle shows referral dropdown, on-leave badge in directory, on-leave card in profile. |
| `FeedbackFlowTest.java` | UI Test | Tests: rating bar displayed, comment field displayed, zero-rating shows error, feedback prompt card appears for completed appointment. |

### Key Technical Decisions

- **Anonymity at schema level.** The `FeedbackService` model class has no `studentId` field — not as a getter that returns null, but as a genuinely absent field. This ensures that even if someone inspects the Firestore document directly, they cannot trace feedback back to a student. Verified by a unit test that uses reflection to confirm no "studentId" field exists.
- **Dedicated `setOnLeaveStatus()` vs reusing `updateCounselorProfile()`.** The existing `updateCounselorProfile()` uses `set()` which overwrites the entire document. If a counselor toggles leave from a quick setting, this would blank out other fields not loaded into the form. A targeted `update()` only touches the three leave fields.
- **Failure-safe feedback check.** `hasFeedbackForAppointment()` returns `false` on Firestore failure. The worst case is the student sees the feedback prompt twice — better than silently losing feedback.

### Data Flow: Feedback Submission

```
StudentHomeActivity.onCreate()
    → checkForPendingFeedback()
        → AppointmentRepository.getCompletedAppointmentsNeedingFeedback(studentId)
            → onSuccess(completedList):
                → FeedbackRepository.hasFeedbackForAppointment(latestAppt.getId())
                    → onResult(false): showFeedbackPromptCard(appointment)
                        → CounselorRepository.getCounselor() → display counselor name
                        → "Give Feedback" → showFeedbackDialog(appointmentId)
                            → User rates 1–5 stars, types comment
                            → Submit:
                                → FeedbackService(appointmentId, rating, comment)  // NO studentId
                                → FeedbackRepository.submitFeedback(feedback)
                                    → Firestore: feedback/{newId} = {appointmentId, rating, comment, submittedAt}
                                    → onSuccess: Toast + hide card
```

### Data Flow: On-Leave Toggle

```
Counselor taps "Edit Profile" → CounselorProfileEditActivity
    → CounselorRepository.getCounselor(uid) → pre-fill all fields
    → Counselor toggles SwitchMaterial ON
        → Message field + referral dropdown become visible
        → CounselorRepository.getAllCounselors() → populate dropdown (exclude self)
    → Counselor types message, selects referral, taps "Save"
        → counselor.setOnLeave(true)
        → counselor.setOnLeaveMessage(message)
        → counselor.setReferralCounselorId(selectedId)
        → CounselorRepository.updateCounselorProfile(uid, counselor)
            → Firestore: counselors/{uid}.set(counselor)
            → onSuccess: Toast + finish()

Student browses directory → sees "Currently Away" chip
    → Taps card → CounselorProfileActivity
        → On-leave card shown with message
        → "Book" button disabled
        → "See Referred Counselor" button → navigates to referral's profile
```

---

## 9. Sprint 4.5 — Integration Refinements (US-03, US-04, Homepage Wiring)

**Duration:** Week 5 (partial)
**Stories:**
- US-03: Slide-to-Cancel completion
- US-04: Find My Match quiz expansion
- Homepage filter chips and search bar wiring

This sprint formalized concurrent developer work that existed outside the original PLAN.md scope. Features had UI but lacked full backend wiring or needed expansion.

### What Was Built

#### Slide-to-Cancel — Session Card Refresh

The slide-to-cancel flow was already ~95% functional: SeekBar triggers at ≥95% → confirmation dialog → `AppointmentRepository.cancelAppointment()` marks the appointment CANCELLED and restores the slot's availability. The only gap: after cancellation, `clearSessionCard()` hid the card but didn't check for the next upcoming appointment.

**Fix:** Replace `clearSessionCard()` with `fetchUpcomingSession()` in the cancel success callback. This method already handles both cases — shows the next CONFIRMED appointment if one exists, or hides the card cleanly if none remain.

#### Find My Match Quiz — Multi-Step Expansion

The original quiz was a single 4-button category picker disguised as a quiz. Sprint 4.5 expanded it into a 3-question guided assessment:

| Question | Options |
|---|---|
| Q1: "What's been on your mind lately?" | Emotional/Personal, Academic/Career, Relationships, Not sure/General |
| Q2: "How long have you been dealing with this?" | Just started, A few weeks, More than a month, On and off |
| Q3: "What kind of support are you looking for?" | Listener, Practical strategies, Understanding feelings, Crisis support |

The activity manages quiz state with a step counter and `answers[]` array. A `ProgressBar` shows progress through the 3 questions. Back navigation returns to the previous question. After Q3, a result screen shows "We found your match — Dr. Baz" (the only counselor on the platform) with two options:
- "View Profile" → `CounselorRepository.getAllCounselors()` → first counselor → `CounselorProfileActivity`
- "Browse All Counselors" → `CounselorListActivity`

The `answers[]` structure is designed for future matching logic when more counselors join — question answers can feed into a scoring function against counselor specializations.

#### Homepage Filter Chips — Dynamic Wiring

The 6 hardcoded TextViews ("All", "Academic Stress", "Career Anxiety", "General Therapy", "Relationships", "Mindfulness") were replaced with a `ChipGroup` programmatically populated from `SpecializationTags.ALL_TAGS`. Each chip launches `CounselorListActivity` with the corresponding specialization as an intent extra — reusing the exact same `SPECIALIZATION` intent-extra pathway that `QuizActivity` already uses.

#### Homepage Search Bar — Query Pass-Through

The static search bar placeholder was replaced with an `EditText` + Go button. User types a query → `CounselorListActivity` receives it via a `SEARCH_QUERY` intent extra → pre-fills the search bar → `applyFilters()` runs immediately on load with the query, showing matching counselors.

### How All Paths Connect

```
StudentHomeActivity
    ├── Search Bar (EditText) → CounselorListActivity(SEARCH_QUERY="...")
    ├── Filter Chips (from SpecializationTags) → CounselorListActivity(SPECIALIZATION="...")
    ├── "Find My Match" → QuizActivity → Q1→Q2→Q3 → Result
    │     ├── "View Profile" → CounselorProfileActivity(Dr. Baz)
    │     └── "Browse All" → CounselorListActivity
    ├── Session Card + Slide-to-Cancel → cancel → fetchUpcomingSession()
    ├── Feedback Prompt → FeedbackDialog → FeedbackRepository
    └── Emergency FAB → EmergencyDialogFragment → ACTION_DIAL
```

---

## 10. Final Class Inventory

### Model Classes (Firestore-mapped)

| Class | Firestore Collection | Fields | Sprint |
|---|---|---|---|
| `Student` | `users` | uid, name, email, preferredName, pronouns, role, createdAt | Sprint 1 |
| `Counselor` | `counselors` | id, uid, name, bio, specializations, language, gender, onLeave, onLeaveMessage, referralCounselorId | Sprint 1 (base), Sprint 2 (fields) |
| `Appointment` | `appointments` | id, studentId, counselorId, slotId, date, time, status | Pre-existing |
| `TimeSlot` | `slots` | id, counselorId, date, time, available | Pre-existing |
| `FeedbackService` | `feedback` | id, appointmentId, rating, comment, submittedAt (no studentId) | Sprint 4 |
| `AvailabilitySchedule` | (local only) | counselorId, slotsByDate | Sprint 3 |

### Repository Classes

| Class | Collection | Key Methods | Sprint |
|---|---|---|---|
| `UserRepository` | `users` | createUser, getCurrentUser, getUserName, getCurrentUserRole | Sprint 1, extended Sprint 3 |
| `CounselorRepository` | `counselors` | getAllCounselors, getCounselor, updateSpecializations, updateCounselorProfile, setOnLeaveStatus | Sprint 2, extended Sprint 4 |
| `AppointmentRepository` | `appointments` + `slots` | bookAppointment (transaction), cancelAppointment, updateAppointmentStatus, getAppointmentsForCounselor, getAppointmentsForStudent, getCompletedAppointmentsNeedingFeedback | Sprint 3, extended Sprint 4 |
| `AvailabilityRepository` | `slots` | getSlotsForCounselor, getAvailableSlotsForCounselor, addSlot, removeSlot | Sprint 3 |
| `FeedbackRepository` | `feedback` | submitFeedback, hasFeedbackForAppointment | Sprint 4 |

### Constants Classes

| Class | Purpose | Sprint |
|---|---|---|
| `UserRole` | STUDENT, COUNSELOR, ADMIN role strings | Sprint 1 |
| `SpecializationTags` | 8 predefined counselor specialization tag strings + ALL_TAGS array | Sprint 2 |

### Activity Classes

| Class | Role | User Type | Sprint |
|---|---|---|---|
| `LoginActivity` | Login + role selection + auto-redirect | All | Pre-existing, updated Sprint 1 |
| `RegisterActivity` | Registration form + Firebase Auth + Firestore write | All | Sprint 1 |
| `PrivacyPolicyActivity` | Privacy policy gate after registration | All | Sprint 1 |
| `StudentHomeActivity` | Student dashboard: welcome, crisis banner, quiz, session card, feedback, search, chips | Student | Sprint 1 (base), enhanced throughout |
| `CounselorDashboardActivity` | Counselor dashboard: tabbed appointments, stats, availability management | Counselor | Merged work, overhauled Sprint 3 |
| `CounselorListActivity` | Counselor directory with multi-filter search | Student | Merged work, enhanced Sprint 2 |
| `CounselorProfileActivity` | Full counselor profile view + booking entry + on-leave display | Student | Sprint 2, extended Sprint 4 |
| `CounselorProfileEditActivity` | Counselor profile editor: bio, tags, on-leave toggle | Counselor | Sprint 2, extended Sprint 4 |
| `BookingActivity` | Calendar + slot selection + booking confirmation | Student | Merged work, rebuilt Sprint 3 |
| `AvailabilitySetupActivity` | Counselor slot management (add/remove) | Counselor | Sprint 3 |
| `CalendarActivity` | Student appointment calendar by date | Student | Merged work, refactored Sprint 3 |
| `HistoryActivity` | Student appointment history | Student | Merged work, refactored Sprint 3 |
| `QuizActivity` | Find My Match multi-step questionnaire | Student | Merged work, expanded Sprint 4.5 |

### Fragment Classes

| Class | Type | Purpose | Sprint |
|---|---|---|---|
| `BookingConfirmationFragment` | BottomSheetDialogFragment | Booking summary + confirm button | Sprint 3 |
| `EmergencyDialogFragment` | DialogFragment | Crisis line dial dialog (offline-capable) | Sprint 3 |

### Adapter Classes

| Class | Binds | Used By | Sprint |
|---|---|---|---|
| `CounselorAdapter` | Counselor → card | CounselorListActivity | Merged work, enhanced Sprint 2 |
| `AppointmentAdapter` | Appointment → card (counselor view) | CounselorDashboardActivity | Merged work, enhanced Sprint 3 |
| `StudentAppointmentAdapter` | Appointment → card (student view) | CalendarActivity, HistoryActivity | Merged work |
| `TimeSlotAdapter` | TimeSlot → card | BookingActivity | Merged work |

### Total: 34 Java classes (6 models, 5 repositories, 2 constants, 13 activities, 2 fragments, 4 adapters, 2 legacy stubs)

---

## 11. Test Coverage Summary

### Unit Tests (JUnit — `src/test/`)

| Test Class | Covers | Tests | Sprint |
|---|---|---|---|
| `StudentTest.java` | Student model | 5 (default role, constructor, empty constructor, setters, timestamp) | Sprint 1 |
| `CounselorTest.java` | Counselor model + SpecializationTags | 7 (onLeave default, specializations, empty constructor, setters, toggle, message, tags verification) | Sprint 2 |
| `AppointmentTest.java` | Appointment model | 3+ (empty constructor, setters, status values) | Sprint 3 |
| `AvailabilityScheduleTest.java` | AvailabilitySchedule model | 5 (filters unavailable, date subset, empty date, available dates set, empty input) | Sprint 3 |
| `FeedbackServiceTest.java` | FeedbackService model | 5 (no studentId field, constructor, empty constructor, rating bounds, null comment) | Sprint 4 |

### UI Tests (Espresso — `src/androidTest/`)

| Test Class | Covers | Tests | Sprint |
|---|---|---|---|
| `RegistrationFlowTest.java` | RegisterActivity form validation | 5 (fields visible, invalid email, password mismatch, short password, empty name) | Sprint 1 |
| `LoginRoleRoutingTest.java` | LoginActivity role selection | 4 (role buttons, counselor button text, admin button text, sign-up link) | Sprint 1 |
| `CounselorDirectoryTest.java` | CounselorListActivity filtering | 5 (core elements, language dropdown, gender dropdown, empty state, clear search) | Sprint 2 |
| `BookingFlowTest.java` | BookingActivity UI | 3+ (calendar displayed, counselor name, slot list on date tap) | Sprint 3 |
| `CounselorDashboardTest.java` | CounselorDashboardActivity | 6 (TabLayout, appointment list, Today tab, Week tab, tab switch, add-slot banner) | Sprint 3 |
| `EmergencyButtonTest.java` | EmergencyDialogFragment | 5 (FAB visible, dialog opens, crisis line, campus security, dismiss) | Sprint 3 |
| `OnLeaveFlowTest.java` | On-leave UI flow | 5 (toggle displayed, message field, referral dropdown, directory badge, profile card) | Sprint 4 |
| `FeedbackFlowTest.java` | Feedback UI flow | 4 (rating bar, comment field, zero-rating error, prompt card) | Sprint 4 |

**Total: ~25 unit tests, ~37 UI tests**

---

## 12. User Story Traceability Matrix

| US | Title | Priority | Points | Sprint | Model | Repository | Activity | Test |
|---|---|---|---|---|---|---|---|---|
| **US-22** | Student Registration | P0 | 8 | 1 | Student, UserRole | UserRepository | LoginActivity, RegisterActivity, PrivacyPolicyActivity, StudentHomeActivity | StudentTest, RegistrationFlowTest, LoginRoleRoutingTest |
| **US-23** | Counselor Directory | P0 | 8 | 2 | Counselor | CounselorRepository | CounselorListActivity, CounselorProfileActivity | CounselorTest, CounselorDirectoryTest |
| **US-06** | Specialization Tags | P1 | 5 | 2 | SpecializationTags | CounselorRepository | CounselorProfileEditActivity | CounselorTest |
| **US-01** | Booking Flow | P0 | 13 | 3 | AvailabilitySchedule | AppointmentRepository, AvailabilityRepository | BookingActivity, BookingConfirmationFragment | AvailabilityScheduleTest, AppointmentTest, BookingFlowTest |
| **US-05/10** | Counselor Dashboard | P0 | 8 | 3 | — | AppointmentRepository, AvailabilityRepository | CounselorDashboardActivity, AvailabilitySetupActivity, AppointmentAdapter | CounselorDashboardTest |
| **US-20** | Emergency Button | P0 | 3 | 3 | — | — | EmergencyDialogFragment, StudentHomeActivity | EmergencyButtonTest |
| **US-19** | On-Leave Status | P2 | 5 | 4 | — | CounselorRepository | CounselorProfileEditActivity, CounselorProfileActivity, CounselorAdapter | OnLeaveFlowTest |
| **US-21** | Anonymous Feedback | P2 | 5 | 4 | FeedbackService | FeedbackRepository, AppointmentRepository | StudentHomeActivity | FeedbackServiceTest, FeedbackFlowTest |
| **US-03** | Slide-to-Cancel | — | — | 4.5 | — | AppointmentRepository | StudentHomeActivity | — |
| **US-04** | Find My Match Quiz | — | — | 4.5 | — | CounselorRepository | QuizActivity | — |

---

## 13. Retrospective — Lessons Across Sprints

### What Went Well

1. **Repository pattern from day one.** Establishing `UserRepository` in Sprint 1 set the architectural standard. Every subsequent sprint followed the same pattern, making the codebase consistent and the data layer testable.

2. **Integration-first approach.** Firestore connectivity was live from Sprint 1. No feature was ever "UI only" — every screen that touched data had real Firestore reads/writes, even if the UI was rough initially.

3. **Incremental story delivery.** Each sprint delivered a demonstrable, end-to-end capability. Sprint 1: register and log in. Sprint 2: browse and filter. Sprint 3: book and manage. Sprint 4: leave and feedback. The app was always in a runnable state.

4. **Concurrent development integration.** Teammate Shahbaz's parallel work on dashboards and booking was substantial. Rather than discarding or rewriting it, the team formalized it — identifying what was complete, what was partial, and building the missing layers (repositories, tests) on top.

### What Could Be Improved

1. **Collection naming alignment.** The PLAN.md specified `timeSlots` but implementation used `slots`. This was caught and standardized in Sprint 3, but earlier alignment would have avoided confusion.

2. **Action button placeholders.** The `AppointmentAdapter` still has placeholder toasts for Join (video call), Notes, and Profile buttons. These depend on Phase 4 features (video integration, session records) but the stubbed buttons give an unfinished impression.

3. **N+1 query pattern in adapters.** Both `AppointmentAdapter` (student name lookup) and `StudentAppointmentAdapter` (counselor name lookup) make one Firestore read per visible list item. For typical appointment counts this is acceptable, but a batch-fetch strategy would scale better.

4. **Sprint scope creep.** Sprint 3 collapsed two planned sprints into one (booking + dashboard + emergency). This was successful because the work shared a repository dependency, but it made the sprint significantly larger (24 points) than others.

### Architecture Evolution

```
Sprint 1:  [LoginActivity] → [UserRepository] → Firestore /users
                                                   ↑
Sprint 2:  [CounselorListActivity] → [CounselorRepository] → Firestore /counselors
                                                                 ↑
Sprint 3:  [BookingActivity] → [AppointmentRepository] → Firestore /appointments
           [BookingActivity] → [AvailabilityRepository] → Firestore /slots
           [CounselorDashboard] → [AppointmentRepository] (shared)
           [EmergencyDialog] → (no Firestore — offline)
                                                                 ↑
Sprint 4:  [StudentHomeActivity] → [FeedbackRepository] → Firestore /feedback
           [ProfileEditActivity] → [CounselorRepository] (extended)
                                                                 ↑
Sprint 4.5: [QuizActivity] → [CounselorRepository] (reused)
            [StudentHomeActivity] → [CounselorListActivity] (wiring only)
```

Each sprint added a new repository or extended an existing one. By Sprint 4, five repositories covered five Firestore collections, and every Activity accessed data exclusively through this layer.

---

*This document consolidates the full development cycle from SPRINT-1.md through SPRINT-4.5.md plus the PLAN.md architectural foundation. For detailed implementation code (method signatures, layout XML, string resources), refer to the individual sprint documents.*
