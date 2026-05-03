# BetterCaps — Phase 3 Development Plan
### CS360 Project Part 3 · Half-Way Checkpoint

---

## Table of Contents

1. [Phase 3 Goal](#1-phase-3-goal)
2. [Agile Practices and Team Standards](#2-agile-practices-and-team-standards)
3. [Architecture Overview](#3-architecture-overview)
4. [Firestore Data Model](#4-firestore-data-model)
5. [CRC Card Traceability](#5-crc-card-traceability)
6. [Story Dependency Graph](#6-story-dependency-graph)
7. [Sprint Plan](#7-sprint-plan)
8. [User Stories — Core Foundation](#8-user-stories--core-foundation)
   - [US-22 · Student Registration and Authentication](#us-22--student-registration-and-authentication)
   - [US-23 · Browse and Search Counselor Directory](#us-23--browse-and-search-counselor-directory)
   - [US-06 · Counselor Specialization Tags on Profile](#us-06--counselor-specialization-tags-on-profile)
   - [US-01 · View Available Slots and Book Appointment](#us-01--view-available-slots-and-book-appointment)
   - [US-05 / US-10 · Counselor Appointment Dashboard](#us-05--us-10--counselor-appointment-dashboard)
   - [US-20 · Emergency Button](#us-20--emergency-button)
9. [User Stories — Secondary (Post-Foundation)](#9-user-stories--secondary-post-foundation)
   - [US-19 · Counselor On-Leave Status](#us-19--counselor-on-leave-status)
   - [US-21 · Anonymous Post-Session Feedback](#us-21--anonymous-post-session-feedback)
10. [Class Inventory and Responsibilities](#10-class-inventory-and-responsibilities)
11. [Testing Strategy](#11-testing-strategy)
12. [Definition of Done](#12-definition-of-done)
13. [Deliverables Checklist](#13-deliverables-checklist)

---

## 1. Phase 3 Goal

Phase 3 is a **half-way checkpoint**. The project must demonstrate a tangible, working prototype that supports roughly half the total requirements with Firestore connectivity live. The TA will build and run the app directly from the repository — it must compile and function without manual intervention.

### Working definition of "done" for this phase

- A user can register, log in, browse counselors, and successfully book an appointment — all persisted in Firestore.
- A counselor can log in and see their upcoming appointments on a dashboard.
- An emergency contact button is accessible from the home screen at all times.
- All implemented features have corresponding unit and intent tests.
- Every model class has Javadoc documentation.

### Rationale for story selection

The 25+ user stories in the product backlog were triaged into two tiers:

| Tier | Stories | Why |
|---|---|---|
| **Core (this phase)** | US-22, US-23, US-06, US-01, US-05/US-10, US-20 | These form the **minimum end-to-end loop**: a student registers, browses counselors, books an appointment, and the counselor sees it. The emergency button is a safety-critical feature that must be present from the first usable build. |
| **Secondary (stretch)** | US-19, US-21 | These enrich the core loop (on-leave graceful redirect, anonymous feedback) but the app functions without them. Included as stretch goals. |
| **Deferred (Phase 4)** | All remaining stories | Features like waitlisting, secure messaging, intake assessments, session records, admin dashboards, privacy filters, and notification services are experience enhancers built atop the foundation. They depend on the core loop being stable. |

---

## 2. Agile Practices and Team Standards

### 2.1 Scrum Workflow

The team operates in **weekly sprints**. Each sprint follows this lifecycle:

| Event | When | Purpose |
|---|---|---|
| Sprint Planning | Start of each week | Select user stories, assign owners, estimate points |
| Daily Sync | As needed (Slack) | Flag blockers, share progress |
| Sprint Review | End of week, in lab with TA | Demo completed stories, get feedback |
| Sprint Retrospective | Post-review | Identify what to improve next sprint |

Stories move through: **Todo → In Progress → In Review → Done** on the GitHub Kanban board. No story is "Done" unless it satisfies the Definition of Done (Section 12).

### 2.2 GitHub Practices

- Every feature lives on its own branch named `feature/US-XX-short-description`.
- No direct commits to `main`. All merges go through a Pull Request reviewed by at least one other team member.
- Each PR must reference its GitHub Issue (e.g. `Closes #12`).
- Commit messages follow the format: `[US-XX] short description of change`.
- The TA and instructor must remain invited as collaborators throughout.

### 2.3 Coding Convention (Java / Android)

- **Naming:** `camelCase` for variables and methods, `PascalCase` for classes, `UPPER_SNAKE_CASE` for constants.
- **Package:** All classes under `com.example.moogerscouncil`. No sub-packages diverge from this (fix the existing `Counselor.java` package mismatch).
- **Model classes:** Pure data containers for Firestore deserialization. No business logic beyond simple validation (e.g. `TimeSlot.book()` state guard).
- **Repository pattern:** All Firestore reads and writes go through a dedicated `*Repository` class. Activities and Fragments never call Firestore directly. This keeps the data layer testable and swappable.
- **No anonymous inner classes for Firestore callbacks** — use named listener classes or lambdas with clearly named variables to keep code readable.
- **Error handling:** Every Firestore call must have an `.addOnFailureListener` that logs the error and surfaces a user-facing message via `Toast` or `Snackbar`. Silent failures are not acceptable.
- **String resources:** All user-visible strings go in `res/values/strings.xml`. No hardcoded strings in Java or XML layouts.
- **File header comment:** Every `.java` source file begins with a block comment describing its role in the application and any design pattern it participates in (e.g. Repository, Observer, Adapter).

### 2.4 Javadoc Requirements

Every **model class** and every **public method** must have Javadoc. Minimum content per method:
- One-line summary sentence.
- `@param` for each parameter.
- `@return` if non-void.
- `@throws` if an exception can be raised.

Example:
```java
/**
 * Books this time slot, marking it unavailable in local state.
 * The caller is responsible for persisting this change to Firestore.
 *
 * @throws IllegalStateException if the slot is already booked.
 */
public void book() { ... }
```

### 2.5 Integration First

Per the course hint: **integrate early and often**. Firestore connections are not added at the end. Every user story that touches data must wire up real Firestore reads/writes from day one, even if the UI is rough. Stubbing is only acceptable where a dependent story is not yet started (e.g. a counselor profile screen can show a placeholder if US-06 is not yet done, but the fetch call must already be in place).

---

## 3. Architecture Overview

The app follows a layered architecture:

```
┌─────────────────────────────────────────┐
│           UI Layer (Activities)          │
│  LoginActivity, RegisterActivity,        │
│  HomeActivity, CounselorDirectoryActivity│
│  BookingActivity, CounselorDashboard...  │
└──────────────┬──────────────────────────┘
               │ calls
┌──────────────▼──────────────────────────┐
│         Repository Layer                 │
│  UserRepository, CounselorRepository,   │
│  AppointmentRepository,                 │
│  AvailabilityRepository                 │
└──────────────┬──────────────────────────┘
               │ reads/writes
┌──────────────▼──────────────────────────┐
│         Firestore (Firebase)             │
│  /users  /counselors  /appointments      │
│  /timeSlots  /feedback                   │
└─────────────────────────────────────────┘
```

**Why the Repository pattern?**
Activities are hard to unit test because they depend on the Android runtime. By isolating all Firestore calls in repository classes, we can inject mock repositories in tests without needing an emulator for every test run. This also aligns with the CRC card separation — model classes (Student, Counselor, Appointment, etc.) hold state and validation; repositories handle persistence.

---

## 4. Firestore Data Model

### Collection: `users`
```
users/{uid}
  ├── name: String
  ├── email: String
  ├── role: String          // "student" | "counselor" | "admin"
  ├── preferredName: String
  ├── pronouns: String
  └── createdAt: Timestamp
```

### Collection: `counselors`
```
counselors/{counselorId}
  ├── uid: String           // matches users/{uid}
  ├── name: String
  ├── bio: String
  ├── specializations: List<String>   // e.g. ["anxiety", "academic stress"]
  ├── language: String
  ├── gender: String
  ├── onLeave: Boolean
  ├── onLeaveMessage: String
  └── referralCounselorId: String
```

### Collection: `timeSlots`
```
timeSlots/{slotId}
  ├── counselorId: String
  ├── date: String          // ISO format "YYYY-MM-DD"
  ├── time: String          // "HH:MM"
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
  └── status: String        // "CONFIRMED" | "COMPLETED" | "CANCELLED"
```

### Collection: `feedback`
```
feedback/{feedbackId}
  ├── appointmentId: String
  ├── rating: int           // 1–5
  ├── comment: String
  └── submittedAt: Timestamp
  // NOTE: no studentId field — anonymity is enforced at schema level
```

---

## 5. CRC Card Traceability

This section maps each CRC card (from `imgs/CRC/`) to its Phase 3 implementation status. This ensures no design artifact is orphaned or overlooked.

### Phase 3 — Implemented

| CRC Card | Phase 3 Class(es) | Coverage Notes |
|---|---|---|
| **Student** | `Student` model, `UserRepository` | Registration, profile, role-based routing. CRC responsibilities for IntakeAssessment and PrivacyFilter are deferred to Phase 4. |
| **Counselor** | `Counselor` model, `CounselorRepository` | Profile, specialization tags, on-leave status. CRC responsibilities for Waitlist counts, SessionRecord access, and SecureMessaging are deferred to Phase 4. |
| **CounselorProfile** | Merged into `Counselor` model | The CRC card's bio, specialization tags, and language preferences are fields on the `Counselor` model. The profile-edit screen uses `CounselorProfileEditActivity`. Full CounselorProfile as a separate class is deferred to Phase 4 if needed. |
| **Appointment** | `Appointment` model, `AppointmentRepository` | Booking, lifecycle (CONFIRMED/COMPLETED/CANCELLED), student-counselor linkage. CRC responsibilities for auto-confirm gestures and pre-session emails are deferred. |
| **AvailabilitySchedule** | `AvailabilitySchedule` model, `AvailabilityRepository` | Groups TimeSlots by date for calendar rendering. CRC responsibilities for recurring weekly patterns and external calendar sync are deferred. |
| **FeedbackService** | `FeedbackService` model, `FeedbackRepository` | Anonymous post-session rating and comment. CRC responsibilities for aggregated analytics are deferred. |
| **CrisisIntervention** | `EmergencyDialogFragment` | Emergency dial button on home screen. CRC responsibilities for auditable crisis logs and safety plans are deferred. |

### Phase 4 — Deferred

| CRC Card | Reason for Deferral |
|---|---|
| **Administrator** | Admin dashboard, platform statistics, and account management are not part of the core student-counselor loop. |
| **IntakeAssessment** | Triage questionnaire depends on a stable counselor directory and matching logic. |
| **SessionRecord** | Session notes and history depend on completed appointments flowing through the system. |
| **Waitlist** | Waitlisting requires the booking flow to be stable and appointment cancellation to be implemented. |
| **SecureMessaging** | End-to-end encrypted messaging is a standalone feature that enriches but does not enable the core flow. |
| **PrivacyFilter** | Discreet mode and notification filtering depend on NotificationService, which is also deferred. |
| **NotificationService** | Push notifications (FCM) require backend configuration and are an enhancement to the core flow. |

---

## 6. Story Dependency Graph

Stories must be implemented in dependency order. A story cannot begin until its prerequisites are complete (or at minimum, the prerequisite's data layer is stubbed).

```
US-22 (Registration + Auth)
  │
  ├──► US-23 (Counselor Directory)  ◄── US-06 (Specialization Tags)
  │         │
  │         └──► US-01 (Booking Flow)
  │                   │
  │                   └──► US-05/US-10 (Counselor Dashboard)
  │                             │
  │                             └──► US-21 (Feedback) [secondary]
  │
  ├──► US-20 (Emergency Button)  [no data dependency, can parallel]
  │
  └──► US-19 (On-Leave Status)   [secondary, needs US-23]
```

**Key takeaways:**
- **US-22 is the critical path root.** Nothing works without auth and the `users` collection.
- **US-06 and US-23 can be developed in parallel** once US-22 is done — US-06 writes specialization data, US-23 reads it. If US-06 finishes later, US-23 can display counselors without tags.
- **US-20 has zero data dependencies** and can be built at any time. Assign it to whoever finishes their sprint story early.
- **US-19 and US-21 are secondary** and only begin after all core stories are functional.

---

## 7. Sprint Plan

| Sprint | Stories | Points | Prerequisites | Focus |
|---|---|---|---|---|
| **Sprint 1** | US-22 (Registration + Auth) | 8 | None | Firebase Auth integration, `Student` model, `UserRepository`, role-based routing, privacy policy screen. |
| **Sprint 2** | US-06 (Specialization Tags), US-23 (Counselor Directory) | 5 + 8 | US-22 | `Counselor` model (fix package), `CounselorRepository`, directory UI with filtering, profile edit with tag chips. |
| **Sprint 3** | US-01 (Booking Flow) | 13 | US-23 | `TimeSlot` model (exists), `AvailabilitySchedule`, `AvailabilityRepository`, `AppointmentRepository` with Firestore transaction, calendar UI. |
| **Sprint 4** | US-05/US-10 (Counselor Dashboard), US-20 (Emergency Button) | 8 + 3 | US-01 (dashboard), None (emergency) | Tabbed dashboard with appointment cards, `AvailabilitySetupActivity`, `EmergencyDialogFragment`. |
| **Sprint 5** | US-19 (On-Leave), US-21 (Feedback) | 5 + 5 | US-23, US-05 | On-leave toggle + referral, `FeedbackService` model, `FeedbackRepository`, feedback form. |
| **Sprint 6** | Polish + deliverables | — | All | UML diagrams, Javadoc pass, test coverage review, backlog update, demo prep. |

### Story Point Scale (Fibonacci)

| Points | Meaning |
|---|---|
| 1–2 | Trivial: single-file change, no Firestore |
| 3 | Small: one screen, one repository method |
| 5 | Medium: one screen with Firestore integration and basic UI |
| 8 | Large: multiple screens, repository with multiple methods, filtering/search logic |
| 13 | X-Large: complex transaction logic, calendar UI, multiple interacting components |

Stories in a later sprint may begin early if a team member finishes their current sprint story ahead of schedule. The GitHub Kanban board is the source of truth for actual progress.

---

## 8. User Stories — Core Foundation

---

### US-22 · Student Registration and Authentication

> **As a student**, I want to register using my university email address, set my preferred name and pronouns, and be guided through the platform's privacy policy in clear, plain language, so that I am formally verified and fully informed before I begin using the service.

**Points:** 8 · **Priority:** P0 (blocking) · **Sprint:** 1
**CRC Cards:** Student

#### Acceptance Criteria

1. A new user can create an account with a `@lums.edu.pk` email, password, preferred name, and pronouns.
2. Non-university emails are rejected with a clear validation message before submission.
3. On successful registration, a `users/{uid}` document is created in Firestore with `role: "student"`.
4. After registration, the user sees a scrollable privacy policy and must tap "I Agree" before proceeding.
5. On subsequent launches, if a Firebase Auth session exists, the app skips login and routes directly to the correct home screen.
6. After login, the app reads `users/{uid}.role` from Firestore and routes: students → `StudentHomeActivity`, counselors → `CounselorDashboardActivity`.
7. The login screen displays a meaningful error on failed auth attempts (wrong password, no account, network error).

#### What needs to happen

1. User opens the app and lands on `LoginActivity`.
2. New users tap "Sign Up" to reach `RegisterActivity`.
3. Registration collects: email (must end in a university domain, validated client-side), password, preferred name, pronouns.
4. On submit, Firebase Authentication creates the account. On success, a corresponding document is written to `users/{uid}` in Firestore with the role set to `"student"` by default.
5. After registration, the user is shown a brief privacy policy screen (`PrivacyPolicyActivity`) before being redirected to `HomeActivity`.
6. On subsequent launches, if a Firebase Auth session exists, skip login and go directly to `HomeActivity`.
7. Role-based routing: after login, the app reads `users/{uid}.role` from Firestore. Students go to `StudentHomeActivity`; counselors go to `CounselorDashboardActivity`.

#### Screens involved

| Screen | Description |
|---|---|
| `LoginActivity` | Already exists. Needs Firestore role-check wired up post-login. |
| `RegisterActivity` | Already exists (placeholder). Needs registration form and Firestore write on success. |
| `PrivacyPolicyActivity` | New. Simple scrollable text screen with "I Agree" button. |
| `StudentHomeActivity` | New. Student's main landing page post-login. |

#### Classes involved

| Class | Role | Status |
|---|---|---|
| `Student` | Model — holds uid, name, email, pronouns, role | **New** |
| `UserRepository` | Repository — wraps all `users` collection reads/writes | **New** |
| `LoginActivity` | UI — add post-login Firestore role check | Exists, needs update |
| `RegisterActivity` | UI — add registration form and Firestore write on auth success | Exists (placeholder), needs implementation |
| `PrivacyPolicyActivity` | UI — display privacy policy, gate entry | **New** |

#### Student model fields

```java
private String uid;
private String name;
private String email;
private String preferredName;
private String pronouns;
private String role;          // "student"
private Timestamp createdAt;
```

#### Key implementation notes

- Email domain validation: check that email ends with `@lums.edu.pk` (or whatever the configured domain is) before allowing registration. This is a client-side check only — do not reject server-side, just warn the user.
- Store the role in Firestore, not just in Firebase Auth custom claims, so it can be read without a token refresh.
- `UserRepository.getCurrentUser(callback)` fetches the current user's Firestore document by `FirebaseAuth.getInstance().getCurrentUser().getUid()`.
- **Fix:** The existing `Counselor.java` has package `com.mooger.moogerscouncil` — must be corrected to `com.example.moogerscouncil` during this sprint to prevent build failures.

---

### US-23 · Browse and Search Counselor Directory

> **As a student**, I want to browse a searchable directory of all available counselors and filter results by specialization, session format, spoken language, and counselor gender, so that I can identify a counselor who genuinely fits my needs.

**Points:** 8 · **Priority:** P0 (core) · **Sprint:** 2
**CRC Cards:** Counselor, CounselorProfile (merged into Counselor)
**Depends on:** US-22 (auth must work so student can access the directory)

#### Acceptance Criteria

1. The counselor directory loads all counselors from Firestore on screen open.
2. Each counselor card displays: name, specialization tags (as chips), language, and gender.
3. The student can filter by specialization (multi-select), language (dropdown), and gender (dropdown). Filters are applied client-side instantly.
4. A search bar filters counselors by name in real time as the student types.
5. Tapping a card navigates to `CounselorProfileActivity` showing the full bio and a "Book Appointment" button.
6. Counselors with `onLeave: true` display a "Currently Unavailable" badge; their booking button is disabled and shows their leave message.
7. An empty-state message is shown if no counselors match the current filters.

#### What needs to happen

1. From `StudentHomeActivity`, the student taps "Find a Counselor".
2. `CounselorDirectoryActivity` opens and immediately fetches all documents from the `counselors` Firestore collection.
3. Counselors are displayed in a `RecyclerView` using `CounselorCardAdapter`. Each card shows: name, specialization tags (chips), language, gender.
4. A filter bar at the top allows filtering by: specialization (multi-select chips), language (dropdown), gender (dropdown). Filters are applied client-side against the already-fetched list for responsiveness.
5. A search bar filters by name in real time.
6. Tapping a counselor card opens `CounselorProfileActivity`, showing their full bio and a "Book Appointment" button.
7. Counselors whose `onLeave` flag is `true` are shown with a "Currently Unavailable" badge and their booking button is disabled, showing their leave message instead.

#### Screens involved

| Screen | Description |
|---|---|
| `CounselorDirectoryActivity` | New. RecyclerView with filter bar and search. |
| `CounselorProfileActivity` | New. Full counselor profile + "Book" entry point. |

#### Classes involved

| Class | Role | Status |
|---|---|---|
| `Counselor` | Model — holds all counselor profile fields | Exists (package fix needed) |
| `CounselorRepository` | Repository — wraps `counselors` collection reads | **New** |
| `CounselorCardAdapter` | RecyclerView.Adapter — binds Counselor to card view | **New** |
| `CounselorDirectoryActivity` | UI — directory list with filters | **New** |
| `CounselorProfileActivity` | UI — full profile view | **New** |

#### Filter logic

Filtering happens in memory after the initial Firestore fetch. A `FilterState` plain object holds the current filter selections. When any filter changes, the adapter's dataset is replaced with a filtered copy of the master list. This avoids repeated Firestore queries and keeps the UI snappy.

```java
// Example filter application in CounselorDirectoryActivity
List<Counselor> filtered = masterList.stream()
    .filter(c -> filterState.specialization == null
              || c.getSpecializations().contains(filterState.specialization))
    .filter(c -> filterState.language == null
              || c.getLanguage().equals(filterState.language))
    .filter(c -> filterState.gender == null
              || c.getGender().equals(filterState.gender))
    .collect(Collectors.toList());
adapter.setData(filtered);
```

---

### US-06 · Counselor Specialization Tags on Profile

> **As a counselor**, I want to list specific areas of focus as selectable tags on my profile so that the triage system can match me accurately with the right students.

**Points:** 5 · **Priority:** P1 (supports US-23 filtering) · **Sprint:** 2
**CRC Cards:** CounselorProfile (merged into Counselor)
**Depends on:** US-22 (counselor must be authenticated)

#### Acceptance Criteria

1. A logged-in counselor can navigate to "Edit Profile" from their dashboard.
2. The edit screen displays all predefined specialization tags as toggleable Material chips.
3. The counselor can select/deselect any combination of tags.
4. On save, the selected tags are persisted to `counselors/{id}.specializations` in Firestore.
5. The saved tags appear correctly on the counselor's card in the directory (US-23).
6. The predefined tag list is maintained as string constants in `SpecializationTags`, not hardcoded across files.

#### What needs to happen

1. When a counselor logs in and reaches their dashboard, an "Edit Profile" option is accessible.
2. `CounselorProfileEditActivity` lets a counselor select their specialization tags from a predefined list displayed as toggleable chips.
3. Selected tags are stored in `counselors/{id}.specializations` as a `List<String>`.
4. This story is a prerequisite for US-23 filtering to be meaningful.

#### Predefined tag list (constants)

Defined in a `SpecializationTags` class as string constants to avoid typos across the codebase:
```
ANXIETY, ACADEMIC_STRESS, GRIEF, RELATIONSHIPS,
CAREER_GUIDANCE, DEPRESSION, TRAUMA, FAMILY_ISSUES
```

#### Classes involved

| Class | Role | Status |
|---|---|---|
| `Counselor` | Model — `specializations` field already defined | Exists |
| `SpecializationTags` | Constants class — predefined tag strings | **New** |
| `CounselorRepository` | Repository — add `updateSpecializations(id, tags, cb)` | **New** |
| `CounselorProfileEditActivity` | UI — chip-based tag selector | **New** |

---

### US-01 · View Available Slots and Book Appointment

> **As a student**, I want to view a counselor's available time slots on a calendar so that I can book an appointment that fits my schedule.

**Points:** 13 · **Priority:** P0 (core) · **Sprint:** 3
**CRC Cards:** Appointment, AvailabilitySchedule
**Depends on:** US-23 (student must be able to navigate to a counselor's profile to initiate booking)

#### Acceptance Criteria

1. From a counselor's profile, the student can tap "Book Appointment" to open the booking screen.
2. A calendar view highlights dates that have at least one available slot for this counselor.
3. Tapping a date reveals a horizontal list of available time slots for that day.
4. Tapping a time slot shows a confirmation view with: counselor name, date, time, and a "Confirm Booking" button.
5. On confirm, a new `appointments` document is created with status `"CONFIRMED"` **and** the corresponding `timeSlots/{slotId}.available` is set to `false` — **atomically via a Firestore transaction**.
6. On successful booking, the student sees a success message and is returned to `StudentHomeActivity`.
7. If the transaction fails (slot was taken by another user), the student sees an error and the slot is removed from the displayed list.
8. Already-booked slots are never shown to the student.

#### What needs to happen

1. From `CounselorProfileActivity`, the student taps "Book Appointment".
2. `BookingActivity` opens, showing a calendar (using `CalendarView` or `MaterialCalendarView`). Dates that have at least one available `TimeSlot` for this counselor are highlighted.
3. Tapping a date reveals a horizontal list of available time slots for that day.
4. Tapping a time slot brings up a confirmation bottom sheet: shows counselor name, date, time, and a "Confirm Booking" button.
5. On confirm:
   - A new document is written to `appointments` with status `"CONFIRMED"`.
   - The corresponding `timeSlots/{slotId}.available` is set to `false` in a **Firestore transaction** to prevent double-booking.
   - The student is shown a success screen and returned to `StudentHomeActivity`.
6. If the transaction fails (race condition — slot taken by another user), the student is shown an error message and the slot is removed from the UI.

#### Firestore transaction requirement

The booking **must** use `db.runTransaction()` to atomically check availability and write the appointment. A simple write without a transaction would allow two students to book the same slot simultaneously.

```java
db.runTransaction(transaction -> {
    DocumentSnapshot slotSnap = transaction.get(slotRef);
    if (!slotSnap.getBoolean("available")) {
        throw new FirebaseFirestoreException("Slot taken",
            FirebaseFirestoreException.Code.ABORTED);
    }
    transaction.update(slotRef, "available", false);
    transaction.set(appointmentRef, appointment);
    return null;
});
```

#### Screens involved

| Screen | Description |
|---|---|
| `BookingActivity` | New. Calendar + slot list + confirmation sheet. |
| `BookingConfirmationActivity` | New (or bottom sheet). Summary + confirm button. |

#### Classes involved

| Class | Role | Status |
|---|---|---|
| `TimeSlot` | Model — date, time, counselorId, available | Exists |
| `Appointment` | Model — studentId, counselorId, slotId, date, time, status | Exists |
| `AvailabilitySchedule` | Model — groups TimeSlots by date for a counselor | **New** |
| `AppointmentRepository` | Repository — write appointment + transaction booking | **New** |
| `AvailabilityRepository` | Repository — fetch TimeSlots by counselorId and date range | **New** |
| `BookingActivity` | UI — calendar and slot selection | **New** |

#### AvailabilitySchedule model

This class is a local (non-Firestore) wrapper that groups `TimeSlot` objects by date, making calendar rendering efficient:

```java
public class AvailabilitySchedule {
    private String counselorId;
    private Map<String, List<TimeSlot>> slotsByDate; // key = "YYYY-MM-DD"

    public List<TimeSlot> getSlotsForDate(String date) { ... }
    public Set<String> getDatesWithAvailability() { ... }
}
```

---

### US-05 / US-10 · Counselor Appointment Dashboard

> **As a counselor**, I want to view a dashboard of my upcoming appointments for the day so that I know exactly who I am seeing and can prepare. I also want to see a daily/weekly/monthly dashboard view so that I can plan my workday at a glance.

These two stories share the same screen and are implemented together.

**Points:** 8 · **Priority:** P0 (core) · **Sprint:** 4
**CRC Cards:** Counselor, Appointment, AvailabilitySchedule
**Depends on:** US-01 (appointments must exist in Firestore to display on dashboard)

#### Acceptance Criteria

1. After login with role `"counselor"`, the user lands on `CounselorDashboardActivity`.
2. Three tabs are available: **Today**, **This Week**, **This Month**.
3. Each tab shows a `RecyclerView` of appointment cards for the relevant date range.
4. Each card displays: student name (looked up from `users/{studentId}`), date, time, and a status badge.
5. `CONFIRMED` appointments are in full color; `COMPLETED` are muted; `CANCELLED` are struck through.
6. A floating "My Availability" button opens `AvailabilitySetupActivity` where the counselor can add/remove `TimeSlot` documents.
7. The dashboard refreshes when the counselor returns from the availability setup screen.

#### What needs to happen

1. After logging in with role `"counselor"`, the user lands on `CounselorDashboardActivity`.
2. The dashboard has a tab bar: **Today | This Week | This Month**.
3. Each tab shows a `RecyclerView` of appointment cards, fetched from `appointments` where `counselorId == currentUser.uid` and filtered by the relevant date range.
4. Each appointment card shows: student name (fetched from `users/{studentId}.name`), date, time, and status badge.
5. A floating "My Availability" button navigates to `AvailabilitySetupActivity` where the counselor can create `TimeSlot` documents (add their available slots).
6. Appointments with status `"CONFIRMED"` are shown in full color; `"COMPLETED"` are muted; `"CANCELLED"` are struck through.

#### Screens involved

| Screen | Description |
|---|---|
| `CounselorDashboardActivity` | New. Tabbed appointment list (Today/Week/Month). |
| `AvailabilitySetupActivity` | New. Counselor adds/removes their time slots. |

#### Classes involved

| Class | Role | Status |
|---|---|---|
| `Appointment` | Model | Exists |
| `AppointmentRepository` | Repository — add `getAppointmentsForCounselor(id, cb)` | Extends from US-01 |
| `UserRepository` | Repository — `getUserName(uid, cb)` for student name lookup | Extends from US-22 |
| `AppointmentCardAdapter` | RecyclerView.Adapter — binds Appointment to card | **New** |
| `CounselorDashboardActivity` | UI — tabbed dashboard | **New** |
| `AvailabilitySetupActivity` | UI — slot creation UI for counselors | **New** |

#### Date range filtering

Firestore does not support OR queries across fields efficiently. Filter the date range client-side after fetching the counselor's appointments, or use a Firestore compound query with `whereGreaterThanOrEqualTo("date", startDate).whereLessThanOrEqualTo("date", endDate)`.

---

### US-20 · Emergency Button

> **As a student**, I want a clearly visible emergency button on the home screen that immediately connects me to campus crisis services and mental health emergency lines — always accessible from the top level of the app, never buried.

**Points:** 3 · **Priority:** P0 (safety-critical) · **Sprint:** 4
**CRC Cards:** CrisisIntervention (partial — dial-only, no audit logging in Phase 3)
**Depends on:** None (can be built at any time; assigned to Sprint 4 for workload balancing)

#### Acceptance Criteria

1. A red emergency button (FAB or top-right icon) is **always visible** on `StudentHomeActivity`. It is never hidden behind navigation or menus.
2. Tapping it immediately shows `EmergencyDialogFragment` — no loading, no network call.
3. The dialog offers two options: **Call Crisis Line** and **Campus Emergency Services**, each triggering an `ACTION_DIAL` intent.
4. A **Dismiss** button closes the dialog without action.
5. Phone numbers are stored in `res/values/strings.xml` (`@string/crisis_line_number`, `@string/campus_security_number`), not hardcoded in Java.
6. The dialog works offline — zero dependency on Firestore or any network call.

#### What needs to happen

1. A persistent **red emergency button** (FAB or a top-right icon) is visible on `StudentHomeActivity` at all times. It is not hidden behind any navigation or menu.
2. Tapping it shows an `EmergencyDialogFragment` — a modal with two options:
   - **Call Crisis Line** — dials the configured campus crisis number using an `Intent(Intent.ACTION_DIAL, Uri.parse("tel:XXXX"))`.
   - **Campus Emergency Services** — dials campus security.
   - A **Dismiss** button.
3. The dialog appears immediately — no loading, no network call. This is intentional: emergency access must never depend on Firestore being reachable.
4. The phone numbers are stored as string resources (`@string/crisis_line_number`, `@string/campus_security_number`) so they can be updated without a code change.

#### Why no network dependency

The emergency feature must be available even if the device has poor connectivity. This is why there is no Firestore write on tap. If a logging requirement is added in future (per the CrisisIntervention CRC card's audit-log responsibility), it must be fire-and-forget (best-effort), never blocking the dial intent.

#### Classes involved

| Class | Role | Status |
|---|---|---|
| `EmergencyDialogFragment` | UI — modal with dial options | **New** |
| `StudentHomeActivity` | UI — add FAB and dialog trigger | **New** (created in US-22) |

---

## 9. User Stories — Secondary (Post-Foundation)

These are implemented **after** all six core stories above are working and tested. They are less critical to the demonstrable loop but meaningful to the user experience and demonstrate depth for the "relative quality" evaluation component.

---

### US-19 · Counselor On-Leave Status

> **As a counselor**, I want to set a temporary 'on leave' status with a custom message and a colleague referral so that students who try to book during my absence are gracefully redirected.

**Points:** 5 · **Priority:** P2 (secondary) · **Sprint:** 5
**CRC Cards:** Counselor, CounselorProfile
**Depends on:** US-23 (directory must exist so on-leave badge and referral link can be displayed)

#### Acceptance Criteria

1. In `CounselorProfileEditActivity`, an "On Leave" toggle is available.
2. When toggled on, the counselor can type a leave message and optionally select a referral counselor from a dropdown populated from the `counselors` collection.
3. Saving writes `onLeave: true`, `onLeaveMessage`, and `referralCounselorId` to Firestore.
4. In the directory (US-23), on-leave counselors show a "Currently Away" chip and their "Book" button is replaced with "See Referred Counselor" linking to the referral counselor's profile.
5. Toggling off clears the on-leave state and restores normal booking.

#### What needs to happen

1. In `CounselorProfileEditActivity`, add an "On Leave" toggle.
2. When toggled on, the counselor can type a leave message and optionally select a referral counselor from a dropdown populated from the `counselors` collection.
3. This writes `onLeave: true`, `onLeaveMessage: "..."`, `referralCounselorId: "..."` to `counselors/{id}`.
4. In `CounselorDirectoryActivity` (US-23), counselors with `onLeave: true` show a "Currently Away" chip. Their "Book" button is replaced with a "See Referred Counselor" button that navigates to the referral counselor's profile.

#### Classes involved

| Class | Role | Status |
|---|---|---|
| `Counselor` | Model — add `onLeave`, `onLeaveMessage`, `referralCounselorId` fields | Exists, needs update |
| `CounselorRepository` | Repository — add `setOnLeaveStatus(id, status, cb)` | Extends existing |
| `CounselorProfileEditActivity` | UI — add on-leave toggle + message field | Extends from US-06 |

---

### US-21 · Anonymous Post-Session Feedback

> **As a student**, I want the option to submit optional, fully anonymous post-session feedback after each appointment, so that my experience can contribute to improving the service.

**Points:** 5 · **Priority:** P2 (secondary) · **Sprint:** 5
**CRC Cards:** FeedbackService
**Depends on:** US-05/US-10 (appointments must reach `COMPLETED` status to trigger feedback prompt)

#### Acceptance Criteria

1. When an appointment transitions to `"COMPLETED"`, a dismissible feedback card appears on `StudentHomeActivity`.
2. Tapping the card opens `FeedbackActivity` with a 1–5 star rating and an optional text comment field.
3. On submit, a `feedback/{id}` document is created with `appointmentId`, `rating`, `comment`, and `submittedAt`. **No `studentId` is stored** — anonymity is enforced at the schema level.
4. After submission, the feedback card disappears and does not reappear for that appointment.
5. If dismissed without submitting, a local flag prevents the card from reappearing.

#### What needs to happen

1. When an appointment's status transitions to `"COMPLETED"`, the student's `StudentHomeActivity` shows a dismissible feedback card: "How was your session with [Counselor Name]?".
2. Tapping it opens `FeedbackActivity` — a simple form with a 1–5 star rating and an optional text comment.
3. On submit, a document is written to `feedback/{id}` containing `appointmentId`, `rating`, and `comment`. **No `studentId` field is stored** — anonymity is enforced at the schema level, not just the UI.
4. If the student dismisses, a flag `feedbackDismissed: true` is written to the local appointment document so the card doesn't reappear.

#### Classes involved

| Class | Role | Status |
|---|---|---|
| `FeedbackService` | Model — holds appointmentId, rating, comment (no studentId) | **New** |
| `FeedbackRepository` | Repository — writes to `feedback` collection | **New** |
| `FeedbackActivity` | UI — star rating + comment form | **New** |
| `AppointmentRepository` | Repository — query for completed appointments without feedback | Extends existing |

---

## 10. Class Inventory and Responsibilities

Complete list of all classes in scope for Phase 3, mapped against CRC cards where applicable.

### Model Classes (Firestore-mapped)

| Class | Firestore Collection | CRC Card | Phase | Status |
|---|---|---|---|---|
| `Student` | `users` (role = student) | Student | Phase 3 | **New** |
| `Counselor` | `counselors` | Counselor + CounselorProfile (merged) | Phase 3 | Exists (package fix needed) |
| `TimeSlot` | `timeSlots` | AvailabilitySchedule (data component) | Phase 3 | Exists |
| `Appointment` | `appointments` | Appointment | Phase 3 | Exists |
| `FeedbackService` | `feedback` | FeedbackService | Phase 3 (secondary) | **New** |
| `AvailabilitySchedule` | local wrapper, no collection | AvailabilitySchedule (logic component) | Phase 3 | **New** |

### Model Classes (Deferred to Phase 4)

| Class | CRC Card | Reason |
|---|---|---|
| `SessionRecord` | SessionRecord | Depends on stable appointment flow |
| `Waitlist` | Waitlist | Depends on booking + cancellation |
| `IntakeAssessment` | IntakeAssessment | Depends on counselor matching logic |
| `Administrator` | Administrator | Admin dashboard not in core loop |
| `SecureMessaging` | SecureMessaging | Standalone feature, not core loop |
| `PrivacyFilter` | PrivacyFilter | Depends on NotificationService |
| `NotificationService` | NotificationService | FCM backend configuration needed |
| `CrisisIntervention` | CrisisIntervention | Phase 3 covers dial-only via EmergencyDialogFragment; full audit logging deferred |

### Repository Classes (New in Phase 3)

| Class | Responsibilities | Introduced In |
|---|---|---|
| `UserRepository` | `createUser`, `getUser`, `getCurrentUser`, `getUserName` | US-22 |
| `CounselorRepository` | `getAllCounselors`, `getCounselor`, `updateSpecializations`, `setOnLeaveStatus` | US-23, US-06, US-19 |
| `AppointmentRepository` | `bookAppointment` (transaction), `getAppointmentsForCounselor`, `getAppointmentsForStudent` | US-01, US-05 |
| `AvailabilityRepository` | `getSlotsForCounselor`, `addSlot`, `removeSlot` | US-01, US-05 |
| `FeedbackRepository` | `submitFeedback`, `hasFeedbackForAppointment` | US-21 |

### UI Classes (New in Phase 3)

| Class | Type | Introduced In |
|---|---|---|
| `LoginActivity` | Activity | Exists, updated in US-22 |
| `RegisterActivity` | Activity | Exists (placeholder), implemented in US-22 |
| `PrivacyPolicyActivity` | Activity | US-22 |
| `StudentHomeActivity` | Activity | US-22 |
| `CounselorDirectoryActivity` | Activity | US-23 |
| `CounselorProfileActivity` | Activity | US-23 |
| `CounselorProfileEditActivity` | Activity | US-06 |
| `BookingActivity` | Activity | US-01 |
| `BookingConfirmationActivity` | Activity / BottomSheet | US-01 |
| `CounselorDashboardActivity` | Activity | US-05/US-10 |
| `AvailabilitySetupActivity` | Activity | US-05/US-10 |
| `EmergencyDialogFragment` | DialogFragment | US-20 |
| `FeedbackActivity` | Activity | US-21 |
| `CounselorCardAdapter` | RecyclerView.Adapter | US-23 |
| `AppointmentCardAdapter` | RecyclerView.Adapter | US-05/US-10 |

### Utility / Constants Classes (New in Phase 3)

| Class | Purpose |
|---|---|
| `SpecializationTags` | String constants for all valid counselor specialization tags |
| `AppointmentStatus` | Constants: `CONFIRMED`, `COMPLETED`, `CANCELLED` |
| `UserRole` | Constants: `STUDENT`, `COUNSELOR`, `ADMIN` |

---

## 11. Testing Strategy

### Unit Tests (JUnit — `src/test/`)

Every model class method with logic gets a unit test. Repository classes are tested with a Firestore emulator or a mock using Mockito.

| Test Class | What it covers | Story |
|---|---|---|
| `StudentTest` | Field getters/setters; role value defaults to "student" | US-22 |
| `CounselorTest` | `getSpecializations()` returns correct list; on-leave state toggles | US-06, US-19 |
| `TimeSlotTest` | `book()` throws on already-booked slot; `book()` sets available=false | US-01 |
| `AppointmentTest` | Status transitions; field validation; constructor integrity | US-01 |
| `AvailabilityScheduleTest` | `getSlotsForDate()` returns correct subset; `getDatesWithAvailability()` is accurate | US-01 |
| `FeedbackServiceTest` | No studentId field present after construction; rating bounds | US-21 |

### Intent / UI Tests (Espresso — `src/androidTest/`)

One intent test per completed user story, testing the happy path end-to-end.

| Test | Story | What it asserts |
|---|---|---|
| `RegistrationFlowTest` | US-22 | User can register, privacy screen appears, home screen loads |
| `LoginRoleRoutingTest` | US-22 | Student role → StudentHome; Counselor role → Dashboard |
| `CounselorDirectoryTest` | US-23 | Directory loads, filter by specialization shows correct subset |
| `BookingFlowTest` | US-01 | Student selects slot, confirms, appointment appears in Firestore |
| `EmergencyButtonTest` | US-20 | Emergency button is visible on home screen; dialog appears on tap |
| `CounselorDashboardTest` | US-05/10 | Today tab shows correct appointments for logged-in counselor |

### Test Data

Realistic seed data scripts (Firestore import JSON) are stored in `documentation/test-data/`. They include:
- 3 counselors with varying specializations, languages, gender.
- 1 student account.
- 10 time slots spread across 5 days.
- 2 pre-existing appointments (1 CONFIRMED, 1 COMPLETED).

---

## 12. Definition of Done

A user story is **Done** only when ALL of the following are satisfied:

| # | Criterion | Verified By |
|---|---|---|
| 1 | All acceptance criteria pass when manually tested on a device or emulator | Developer + reviewer |
| 2 | Firestore reads/writes are live (no stubbed data) | Code review |
| 3 | Unit tests for any new/modified model logic pass | `./gradlew test` |
| 4 | At least one intent test for the story's happy path passes | `./gradlew connectedAndroidTest` |
| 5 | All new `.java` files have a file header comment | Code review |
| 6 | All new public methods on model classes have Javadoc | Code review |
| 7 | No hardcoded user-facing strings (all in `strings.xml`) | Code review |
| 8 | All Firestore calls have `.addOnFailureListener` with user-facing error | Code review |
| 9 | PR is linked to the GitHub Issue and reviewed by at least one teammate | GitHub |
| 10 | Story is moved to "Done" on the Kanban board | Assignee |

---

## 13. Deliverables Checklist

Track this against the Phase 3 submission requirements.

- [ ] **Addressing Feedback** — all TA feedback from Phase 2 addressed and noted
- [ ] **Code Base**
  - [ ] US-22 implemented and functional
  - [ ] US-23 implemented and functional
  - [ ] US-06 implemented and functional
  - [ ] US-01 implemented and functional
  - [ ] US-05/US-10 implemented and functional
  - [ ] US-20 implemented and functional
  - [ ] US-19 implemented (secondary)
  - [ ] US-21 implemented (secondary)
  - [ ] Firestore connectivity live (no stubbed data in final submission)
  - [ ] Consistent coding convention throughout
  - [ ] `Counselor.java` package mismatch fixed
- [ ] **Code Documentation**
  - [ ] File header comment in every `.java` source file
  - [ ] Javadoc on all model class public methods
- [ ] **Test Cases**
  - [ ] Unit tests for all model classes with logic
  - [ ] Intent tests for each completed user story
  - [ ] Test data files in `documentation/test-data/`
- [ ] **Object-Oriented Design**
  - [ ] UML class diagram covering all Phase 3 classes
  - [ ] Key attributes and methods shown
  - [ ] CRC card traceability noted (Section 5)
- [ ] **Product Backlog**
  - [ ] README updated with status of each user story
  - [ ] Completed stories marked Done on Kanban board
- [ ] **UI Mockups / Storyboards**
  - [ ] Updated to reflect any UI changes from Phase 2 designs
- [ ] **Sprint Planning Records**
  - [ ] Sprint plan table updated with actual owners and dates
  - [ ] Sprint review notes recorded after each lab session
- [ ] **Demo Ready**
  - [ ] App builds from clean checkout with no manual steps
  - [ ] All team members available for demo session
- [ ] **Tool Use**
  - [ ] All team members have commits across multiple weeks
  - [ ] GitHub Issues used for every user story
  - [ ] PRs used for all merges to main
