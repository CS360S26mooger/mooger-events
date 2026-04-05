# Phase 3 — D3 Midway Deliverable Plan

## Scope Decision

The original D3 user stories (US-01, 05, 06, 10, 15, 19, 20, 21, 22, 23) are too ambitious for a midway build starting from boilerplate. This document defines a **trimmed, working D3 scope** that delivers a coherent end-to-end experience — real login, real data, real booking — without advanced features. Advanced stories are deferred to D4.

---

## D3 Target: What We Are Building

A working Android app where a **student can register, browse counselors, and book a session**, and a **counselor can log in and see their appointments**.

### Screens to Build

| Screen | Role | Covers (simplified) |
|---|---|---|
| Login / Register | Both | US-22 (email+password only, no SSO) |
| Counselor List | Student | US-23 (list view, no filters yet) |
| Counselor Detail | Student | US-06 (view name + specialization) |
| Book Appointment | Student | US-01 (pick from available slots, no calendar widget) |
| My Appointments | Student | Confirmation/history of booked slots |
| Counselor Dashboard | Counselor | US-05 / US-10 (list of upcoming appointments) |
| Emergency Button | Student home | US-20 (static button showing crisis hotline number — no workflow) |

### What is explicitly NOT in D3

| Feature | Original Story | Why Deferred |
|---|---|---|
| Crisis escalation workflow (notifies campus services) | US-15 | Backend integration, out of scope midway |
| Triage questionnaire + counselor matching | US-04 | Complex logic, D4 |
| Discreet mode | US-02 | Cosmetic feature, D4 |
| Slide-to-cancel | US-03 | Gesture UX, D4 |
| Buffer time between sessions | US-08 | Scheduling logic, D4 |
| Google Calendar / Outlook sync | US-09 | External API, D4 |
| No-show marking | US-11 | Dashboard enhancement, D4 |
| Quick-insert note templates | US-12 | In-session tooling, D4 |
| Session history per student | US-13 | Requires longitudinal data, D4 |
| Pre-session student profile view | US-14 | D4 |
| Automated reminders (24h / 1h) | US-16 | Push notifications, D4 |
| Returning student indicator | US-17 | D4 |
| Secure pre-session messaging | US-18 | Messaging system, D4 |
| Counselor on-leave + referral | US-19 | D4 |
| Anonymous post-session feedback | US-21 | D4 |
| Waitlist | US-24, US-25 | D4 |
| SSO / institutional login | US-22 (partial) | D4 |
| Counselor directory filters | US-23 (partial) | D4 |

---

## Tech Stack

- **Language:** Java (existing boilerplate)
- **Backend:** Firebase (Authentication + Firestore)
  - Firebase Auth handles login/register with email+password
  - Firestore stores users, counselors, availability slots, bookings
- **UI:** standard Android Views (no Jetpack Compose needed)

---

## Data Model (Firestore)

```
users/{uid}
  - name: String
  - email: String
  - role: "student" | "counselor"

counselors/{uid}
  - name: String
  - specialization: String   // e.g. "Academic Stress, Anxiety"
  - bio: String (optional)

slots/{slotId}
  - counselorId: String
  - dateTime: Timestamp       // e.g. 2026-04-10 10:00
  - durationMinutes: int      // e.g. 50
  - isBooked: boolean

bookings/{bookingId}
  - slotId: String
  - studentId: String
  - counselorId: String
  - status: "confirmed" | "cancelled"
  - createdAt: Timestamp
```

---

## Implementation Steps

### Step 1 — Firebase Setup
- [ ] Add Firebase to the Android project (google-services.json)
- [ ] Add dependencies: `firebase-auth`, `firebase-firestore`
- [ ] Create a FirebaseHelper / Repository class for all DB calls

### Step 2 — Authentication (US-22, simplified)
- [ ] `LoginActivity` — email + password fields, Login button, link to Register
- [ ] `RegisterActivity` — name, email, password, role selector (Student / Counselor)
  - On register: create Firebase Auth user + write `users/{uid}` doc
- [ ] Route to correct home screen based on role after login

### Step 3 — Student Home & Counselor List (US-23, simplified)
- [ ] `StudentHomeActivity` — shows counselor list + emergency button
- [ ] `RecyclerView` with `CounselorAdapter` — each card shows name + specialization
- [ ] Fetch `counselors` collection from Firestore and bind to adapter

### Step 4 — Counselor Detail & Booking (US-01, US-06)
- [ ] `CounselorDetailActivity` — shows counselor name, specialization, bio
- [ ] Below the profile: list of available (non-booked) slots from `slots` collection filtered by `counselorId`
- [ ] "Book" button on each slot row — writes a `bookings` doc + sets `slot.isBooked = true` in a Firestore transaction

### Step 5 — Student: My Appointments
- [ ] `MyAppointmentsActivity` — list of bookings where `studentId == currentUser.uid`
- [ ] Each row shows: counselor name, date/time, status
- [ ] Accessible from a nav button on StudentHomeActivity

### Step 6 — Counselor Dashboard (US-05, US-10)
- [ ] `CounselorDashboardActivity` — list of bookings where `counselorId == currentUser.uid`
- [ ] Each row shows: student name, date/time
- [ ] Sort by upcoming date

### Step 7 — Emergency Button (US-20, stub)
- [ ] Floating or prominent button on `StudentHomeActivity`
- [ ] On tap: shows a dialog with hardcoded crisis line numbers (e.g. "Campus Crisis Line: 051-111-249-249")
- [ ] No backend call needed — this satisfies the story's intent at this stage

### Step 8 — Seed Data
- [ ] Write a one-time seeder (or manual Firestore console entry) to populate:
  - 3–4 counselor accounts with specializations
  - 5–10 available slots per counselor spread over the next two weeks

---

## Division of Work (Suggested)

| Member | Responsibility |
|---|---|
| 1 | Firebase setup, Auth (Login + Register screens) |
| 2 | Counselor List screen + Firestore fetch |
| 3 | Counselor Detail + Booking flow (transaction logic) |
| 4 | Counselor Dashboard + My Appointments screen |
| 5 | Emergency button, seed data, integration testing |

---

## Definition of Done for D3

- [ ] A student can register and log in
- [ ] A student can see a list of counselors
- [ ] A student can tap a counselor, view their profile, and book an available slot
- [ ] The slot is no longer bookable after it is taken
- [ ] A student can view their booked appointments
- [ ] A counselor can log in and see their upcoming appointments
- [ ] The emergency button shows a crisis contact dialog
- [ ] App does not crash on the happy path for the above flows
