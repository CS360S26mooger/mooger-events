# Sprint 3 — US-01 (Booking Flow), US-05/US-10 (Counselor Dashboard), US-20 (Emergency Button)
### Detailed Implementation Guide

---

## 0. Pre-Sprint Status Report

### Sprint 2 Review — US-23 (Counselor Directory) + US-06 (Specialization Tags): COMPLETE

All Sprint 2 deliverables have been verified as complete:

| Deliverable | Status | Notes |
|---|---|---|
| `CounselorRepository.java` | Done | `getAllCounselors()`, `getCounselor()`, `updateSpecializations()`, `updateCounselorProfile()`. Callback-based async. Document IDs attached post-deserialization. |
| `SpecializationTags.java` | Done | 8 predefined tags (Anxiety, Academic Stress, Grief, Relationships, Career Guidance, Depression, Trauma, Family Issues). `ALL_TAGS` array for chip group iteration. |
| `CounselorProfileActivity.java` | Done | Full bio, specialization chips (read-only), language, gender, on-leave card with custom message. "Book Appointment" disabled when on-leave. Routes to `BookingActivity`. |
| `CounselorProfileEditActivity.java` | Done | Loads current profile via `CounselorRepository`. Editable bio, language, gender. Toggleable specialization chips from `SpecializationTags.ALL_TAGS`. Save persists via `updateCounselorProfile()`. |
| `CounselorListActivity.java` enhancements | Done | Real-time name search, multi-select specialization chips, language + gender dropdowns (dynamically populated from data), empty-state message, on-leave badge. All filtering is client-side against master list. |
| `CounselorAdapter.java` enhancements | Done | Shows name, specializations (dot-joined), language. On-leave badge. Card tap → `CounselorProfileActivity`. `setData()` for filter updates. |
| `Counselor.java` field additions | Done | Full model: id, uid, name, bio, specializations (`List<String>`), language, gender, onLeave (`Boolean`), onLeaveMessage, referralCounselorId. All getters/setters. |
| `CounselorDashboardActivity.java` — Edit Profile button | Done | Routes to `CounselorProfileEditActivity`. |
| `strings.xml` additions | Done | All directory, profile, profile-edit, and filter strings. |
| `AndroidManifest.xml` updates | Done | `CounselorProfileActivity` and `CounselorProfileEditActivity` registered. |
| `CounselorTest.java` (unit) | Done | 7 tests: onLeave default, specializations set/get, empty constructor, field setters, onLeave toggle, leave message, SpecializationTags verification. |
| `CounselorDirectoryTest.java` (UI) | Done | 5 tests: core elements display, language dropdown, gender dropdown, search empty state, clearing search restores list. |

### What Carries Over Into Sprint 3

The merged teammate work and Sprint 2 together created a partially-built booking and dashboard flow. Sprint 3 inherits these incomplete implementations and must complete them to spec.

#### Booking Flow — Existing Partial Implementation

| Component | Current State | What Carries Over | What Sprint 3 Must Fix/Build |
|---|---|---|---|
| `BookingActivity.java` | ~40% complete | Fetches available slots from `slots` collection, has atomic Firestore transaction for booking (correct pattern), creates `appointments` doc with status CONFIRMED | No calendar UI for date selection (flat slot list), no confirmation bottom sheet, collection name is `slots` (PLAN.md says `timeSlots` — standardize on `slots` since it's already live), no `AppointmentRepository` abstraction |
| `TimeSlot.java` | Complete | Model with id, counselorId, date, time, available. `book()` method throws on double-book. | No changes needed |
| `Appointment.java` | Complete | Model with id, studentId, counselorId, slotId, date, time, status. | No changes needed |
| `TimeSlotAdapter.java` | Complete | Renders date/time/Book button. `OnBookClickListener` callback. | No changes needed |
| `CounselorProfileActivity.java` | Complete | "Book Appointment" button routes to `BookingActivity` with counselorId + counselorName. | Entry point is ready — no changes needed |

**Key technical note:** The existing codebase uses `slots` as the Firestore collection name. The PLAN.md schema says `timeSlots`. Since `slots` is already in use across `BookingActivity`, `CounselorDashboardActivity`, and has live data in Firestore, **Sprint 3 standardizes on `slots`**. All new repository code will reference `slots`.

#### Counselor Dashboard — Existing Partial Implementation

| Component | Current State | What Carries Over | What Sprint 3 Must Fix/Build |
|---|---|---|---|
| `CounselorDashboardActivity.java` | ~80% complete | Counselor greeting from Firestore, 3-stat cards (Today/Total/Week), Add Availability slot via DatePicker+TimePicker → `slots` collection, appointment RecyclerView, Edit Profile button, logout | Stats show same count (not filtered by date range), no Today/Week/Month tabs, direct Firestore calls (no repository), no status badges on appointment cards |
| `AppointmentAdapter.java` | ~40% complete | Shows student initial, name, time, date, status. 5 action buttons (Join, No-Show, Crisis, Profile, Notes) | All action buttons are placeholder toasts — no real functionality. Student name is not looked up from `users` collection |
| `activity_counselor_dashboard.xml` | ~80% complete | Greeting bar, 3-column stats, add-slot banner, appointment RecyclerView | No TabLayout for Today/Week/Month |

#### Emergency Button — Existing Partial Implementation

| Component | Current State | What Sprint 3 Must Build |
|---|---|---|
| Crisis support in `StudentHomeActivity` | Red banner card with hardcoded helpline numbers (Umang, Rozan, LUMS CAPS). Inline `AlertDialog` on tap. | Refactor into proper `EmergencyDialogFragment` with `ACTION_DIAL` intents. Add persistent red FAB. Move phone numbers to `strings.xml`. |

#### Classes That Do NOT Exist Yet

| Class | Required By | Purpose |
|---|---|---|
| `AppointmentRepository.java` | US-01, US-05/US-10 | Wraps all `appointments` collection operations. Centralizes the booking transaction. |
| `AvailabilityRepository.java` | US-01, US-05/US-10 | Wraps all `slots` collection operations. Fetch, add, remove slots. |
| `AvailabilitySchedule.java` | US-01 | Local model grouping `TimeSlot` objects by date for calendar rendering. |
| `EmergencyDialogFragment.java` | US-20 | `DialogFragment` with crisis line dial buttons. |
| `AvailabilitySetupActivity.java` | US-05/US-10 | Dedicated counselor screen for managing availability slots. |

#### Firestore Collections After Sprint 2

| Collection | Fields | Current Read Access | Current Write Access |
|---|---|---|---|
| `users` | uid, name, email, preferredName, pronouns, role, createdAt | `UserRepository` (correct) | `UserRepository` (correct) |
| `counselors` | uid, id, name, bio, specializations, language, gender, onLeave, onLeaveMessage, referralCounselorId | `CounselorRepository` (correct) | `CounselorRepository` (correct) |
| `slots` | counselorId, date, time, available | `BookingActivity` (direct — **needs repository**) | `CounselorDashboardActivity` (direct — **needs repository**) |
| `appointments` | studentId, counselorId, slotId, date, time, status | `BookingActivity`, `CounselorDashboardActivity`, `CalendarActivity`, `HistoryActivity` (all direct — **needs repository**) | `BookingActivity` transaction (direct — **needs repository**) |

---

## 1. Sprint 3 Objective

By the end of this sprint, the app delivers:

1. **A student can view a counselor's available slots on a calendar, select a date/time, see a confirmation summary, and atomically book an appointment** — with race-condition protection via Firestore transaction (US-01).
2. **A counselor can view their appointments in a tabbed dashboard (Today / This Week / This Month)**, with student name lookups and status badges, plus a dedicated screen for managing their availability slots (US-05/US-10).
3. **A persistent emergency button on the student home screen** that opens a dialog for immediately dialing campus crisis services — zero network dependency (US-20).
4. **Repository pattern enforced** across all remaining Firestore access — `AppointmentRepository` and `AvailabilityRepository` created, and all direct Firestore calls in Activities refactored.

### Why These Three Stories Together

The original PLAN.md spread these across Sprints 3 and 4. However, the merged teammate work already partially implemented both the booking flow and the counselor dashboard. Completing them together in a single sprint is efficient because:

- `AppointmentRepository` is needed by both US-01 (booking writes) and US-05/US-10 (dashboard reads). Building it once serves both.
- `AvailabilityRepository` is needed by both US-01 (slot fetching) and US-05/US-10 (`AvailabilitySetupActivity`). Same repository, two consumers.
- US-20 (Emergency Button) has zero data dependencies and fits as a parallel task while the repository/UI work proceeds.

---

## 2. Files to Create or Modify

### 2.1 New Files

```
src/main/java/com/example/moogerscouncil/
├── AppointmentRepository.java           // Repository — all appointments collection ops + booking transaction
├── AvailabilityRepository.java          // Repository — all slots collection ops
├── AvailabilitySchedule.java            // Model — groups TimeSlots by date (local, not Firestore)
├── EmergencyDialogFragment.java         // UI — crisis dial dialog
├── AvailabilitySetupActivity.java       // UI — counselor manages their time slots
├── BookingConfirmationFragment.java     // UI — bottom sheet confirming booking details

src/main/res/layout/
├── activity_availability_setup.xml      // Counselor slot management screen
├── fragment_booking_confirmation.xml    // Bottom sheet with booking summary
├── fragment_emergency_dialog.xml        // Emergency dial options

src/test/java/com/example/moogerscouncil/
├── AvailabilityScheduleTest.java        // Unit test — slot grouping, date queries
├── AppointmentTest.java                 // Unit test — status transitions, field integrity

src/androidTest/java/com/example/moogerscouncil/
├── BookingFlowTest.java                 // UI test — booking happy path
├── EmergencyButtonTest.java             // UI test — FAB visible, dialog appears
├── CounselorDashboardTest.java          // UI test — tabs, appointment display
```

### 2.2 Files to Modify

```
BookingActivity.java               // Add calendar UI, date-based slot filtering, confirmation sheet, use AppointmentRepository
CounselorDashboardActivity.java    // Add TabLayout (Today/Week/Month), use AppointmentRepository + AvailabilityRepository, fix stats
AppointmentAdapter.java            // Student name lookup via UserRepository, status badge styling
StudentHomeActivity.java           // Add emergency FAB, wire EmergencyDialogFragment
UserRepository.java                // Add getUserName(uid, callback) method for student name lookups
activity_booking.xml               // Add CalendarView, date-based slot list area
activity_counselor_dashboard.xml   // Add TabLayout, refine stats section
activity_student_home.xml          // Add red emergency FAB
item_appointment.xml               // Add status badge styling (CONFIRMED/COMPLETED/CANCELLED)
strings.xml                        // All new user-facing strings
AndroidManifest.xml                // Register AvailabilitySetupActivity
```

---

## 3. Implementation Details — Model Layer

### 3.1 `AvailabilitySchedule.java` — Local Grouping Model

**Purpose:** Groups `TimeSlot` objects by date for a specific counselor. Not a Firestore document — purely a local data structure that makes calendar rendering efficient.

**Why this exists:** `BookingActivity` needs to (a) highlight dates on a `CalendarView` that have at least one available slot, and (b) show only the slots for the tapped date. Without this grouping, the Activity would need to iterate the full slot list on every date tap.

```java
package com.example.moogerscouncil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Local (non-Firestore) model that groups TimeSlot objects by date
 * for a single counselor. Used by BookingActivity to efficiently
 * render calendar highlights and date-specific slot lists.
 *
 * This class is populated from a list of TimeSlots fetched by
 * AvailabilityRepository and is never persisted to Firestore.
 */
public class AvailabilitySchedule {

    private String counselorId;
    private final Map<String, List<TimeSlot>> slotsByDate; // key = "yyyy-MM-dd"

    /** Constructs an empty schedule for the given counselor. */
    public AvailabilitySchedule(String counselorId) {
        this.counselorId = counselorId;
        this.slotsByDate = new HashMap<>();
    }

    /**
     * Builds a schedule from a flat list of TimeSlots.
     * Only includes slots where available == true.
     *
     * @param counselorId The counselor this schedule belongs to.
     * @param slots       All fetched TimeSlots (available and unavailable).
     * @return A schedule containing only available slots, grouped by date.
     */
    public static AvailabilitySchedule fromSlots(String counselorId, List<TimeSlot> slots) {
        AvailabilitySchedule schedule = new AvailabilitySchedule(counselorId);
        for (TimeSlot slot : slots) {
            if (slot.isAvailable()) {
                schedule.slotsByDate
                    .computeIfAbsent(slot.getDate(), k -> new ArrayList<>())
                    .add(slot);
            }
        }
        return schedule;
    }

    /**
     * Returns all available slots for a specific date.
     *
     * @param date The date in "yyyy-MM-dd" format.
     * @return List of available TimeSlots, or empty list if none.
     */
    public List<TimeSlot> getSlotsForDate(String date) {
        return slotsByDate.getOrDefault(date, new ArrayList<>());
    }

    /**
     * Returns the set of dates that have at least one available slot.
     * Used to highlight dates on the CalendarView.
     *
     * @return Set of date strings in "yyyy-MM-dd" format.
     */
    public Set<String> getDatesWithAvailability() {
        return slotsByDate.keySet();
    }

    /** @return The counselor ID this schedule belongs to. */
    public String getCounselorId() { return counselorId; }
}
```

**How it connects:**
1. `AvailabilityRepository.getSlotsForCounselor()` fetches all `TimeSlot` documents for a counselor.
2. `BookingActivity` passes the result to `AvailabilitySchedule.fromSlots()`.
3. `getDatesWithAvailability()` feeds the calendar highlighting logic.
4. `getSlotsForDate()` feeds the slot RecyclerView when a date is tapped.

---

## 4. Implementation Details — Repository Layer

### 4.1 `AvailabilityRepository.java`

**Purpose:** Single point of access for all Firestore operations on the `slots` collection. Replaces direct calls in `BookingActivity` and `CounselorDashboardActivity`.

```java
package com.example.moogerscouncil;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository for the Firestore 'slots' collection.
 * All reads and writes to time slot documents flow through this class.
 *
 * Used by BookingActivity (fetch available slots), CounselorDashboardActivity
 * (add slots), and AvailabilitySetupActivity (manage slots).
 *
 * Follows the Repository design pattern consistent with UserRepository
 * and CounselorRepository.
 */
public class AvailabilityRepository {

    private final CollectionReference slotsCollection;

    public AvailabilityRepository() {
        this.slotsCollection = FirebaseFirestore.getInstance().collection("slots");
    }

    // --- Callback interfaces ---

    public interface OnSlotsLoadedCallback {
        void onSuccess(List<TimeSlot> slots);
        void onFailure(Exception e);
    }

    public interface OnSlotActionCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    // --- Read operations ---

    /**
     * Fetches all time slots for a given counselor.
     * Returns both available and unavailable slots — the caller
     * (typically AvailabilitySchedule.fromSlots) filters as needed.
     *
     * @param counselorId The counselor whose slots to fetch.
     * @param callback    Receives the slot list on success.
     */
    public void getSlotsForCounselor(String counselorId, OnSlotsLoadedCallback callback) {
        slotsCollection
            .whereEqualTo("counselorId", counselorId)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<TimeSlot> slots = querySnapshot.toObjects(TimeSlot.class);
                // Attach document IDs (Firestore doesn't auto-populate 'id')
                for (int i = 0; i < slots.size(); i++) {
                    slots.get(i).setId(querySnapshot.getDocuments().get(i).getId());
                }
                callback.onSuccess(slots);
            })
            .addOnFailureListener(callback::onFailure);
    }

    /**
     * Fetches only available slots for a given counselor.
     * Used by BookingActivity where unavailable slots are never shown.
     *
     * @param counselorId The counselor whose available slots to fetch.
     * @param callback    Receives the available slot list on success.
     */
    public void getAvailableSlotsForCounselor(String counselorId, OnSlotsLoadedCallback callback) {
        slotsCollection
            .whereEqualTo("counselorId", counselorId)
            .whereEqualTo("available", true)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<TimeSlot> slots = querySnapshot.toObjects(TimeSlot.class);
                for (int i = 0; i < slots.size(); i++) {
                    slots.get(i).setId(querySnapshot.getDocuments().get(i).getId());
                }
                callback.onSuccess(slots);
            })
            .addOnFailureListener(callback::onFailure);
    }

    // --- Write operations ---

    /**
     * Adds a new time slot for a counselor.
     * Creates a new document in the slots collection with available=true.
     *
     * @param counselorId The counselor this slot belongs to.
     * @param date        Date in "yyyy-MM-dd" format.
     * @param time        Time in "HH:mm" format.
     * @param callback    Success/failure callback.
     */
    public void addSlot(String counselorId, String date, String time,
                        OnSlotActionCallback callback) {
        Map<String, Object> slotData = new HashMap<>();
        slotData.put("counselorId", counselorId);
        slotData.put("date", date);
        slotData.put("time", time);
        slotData.put("available", true);

        slotsCollection.add(slotData)
            .addOnSuccessListener(docRef -> callback.onSuccess())
            .addOnFailureListener(callback::onFailure);
    }

    /**
     * Removes a time slot from Firestore.
     * Used by AvailabilitySetupActivity when a counselor deletes a slot.
     *
     * @param slotId   The Firestore document ID of the slot to remove.
     * @param callback Success/failure callback.
     */
    public void removeSlot(String slotId, OnSlotActionCallback callback) {
        slotsCollection.document(slotId)
            .delete()
            .addOnSuccessListener(unused -> callback.onSuccess())
            .addOnFailureListener(callback::onFailure);
    }
}
```

**Migration path:** After this class exists:
- `BookingActivity.fetchSlots()` → replace direct `db.collection("slots")` query with `availabilityRepository.getAvailableSlotsForCounselor()`
- `CounselorDashboardActivity.addSlotToFirestore()` → replace direct write with `availabilityRepository.addSlot()`

---

### 4.2 `AppointmentRepository.java`

**Purpose:** Centralizes all `appointments` collection operations. Most critically, wraps the atomic booking transaction so it can be reused and tested.

```java
package com.example.moogerscouncil;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;

/**
 * Repository for the Firestore 'appointments' collection.
 * All reads and writes to appointment documents flow through this class.
 *
 * The critical method is bookAppointment(), which uses a Firestore
 * transaction to atomically mark a slot as unavailable and create
 * the appointment document — preventing double-booking.
 *
 * Follows the Repository design pattern consistent with UserRepository,
 * CounselorRepository, and AvailabilityRepository.
 */
public class AppointmentRepository {

    private final FirebaseFirestore db;
    private final CollectionReference appointmentsCollection;

    public AppointmentRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.appointmentsCollection = db.collection("appointments");
    }

    // --- Callback interfaces ---

    public interface OnBookingCallback {
        void onSuccess();
        void onSlotTaken();
        void onFailure(Exception e);
    }

    public interface OnAppointmentsLoadedCallback {
        void onSuccess(List<Appointment> appointments);
        void onFailure(Exception e);
    }

    // --- Write operations ---

    /**
     * Atomically books a time slot for a student.
     *
     * This method runs a Firestore transaction that:
     * 1. Reads the slot document to verify it is still available.
     * 2. Sets slot.available = false.
     * 3. Creates a new appointment document with status "CONFIRMED".
     *
     * If the slot was already taken (available == false), the transaction
     * aborts and onSlotTaken() is called instead of onFailure().
     *
     * @param studentId   The booking student's UID.
     * @param counselorId The counselor being booked.
     * @param slot        The TimeSlot to book (must have a valid ID).
     * @param callback    Three-way callback: success, slot-taken, or failure.
     */
    public void bookAppointment(String studentId, String counselorId,
                                 TimeSlot slot, OnBookingCallback callback) {

        DocumentReference slotRef = db.collection("slots").document(slot.getId());
        DocumentReference appointmentRef = appointmentsCollection.document();

        db.runTransaction(transaction -> {
            DocumentSnapshot slotSnap = transaction.get(slotRef);

            Boolean available = slotSnap.getBoolean("available");
            if (available == null || !available) {
                throw new RuntimeException("SLOT_TAKEN");
            }

            // 1. Mark slot as unavailable
            transaction.update(slotRef, "available", false);

            // 2. Create appointment document
            Appointment appointment = new Appointment();
            appointment.setId(appointmentRef.getId());
            appointment.setStudentId(studentId);
            appointment.setCounselorId(counselorId);
            appointment.setSlotId(slot.getId());
            appointment.setDate(slot.getDate());
            appointment.setTime(slot.getTime());
            appointment.setStatus("CONFIRMED");

            transaction.set(appointmentRef, appointment);
            return null;

        }).addOnSuccessListener(unused -> callback.onSuccess())
          .addOnFailureListener(e -> {
              if ("SLOT_TAKEN".equals(e.getMessage())) {
                  callback.onSlotTaken();
              } else {
                  callback.onFailure(e);
              }
          });
    }

    // --- Read operations ---

    /**
     * Fetches all appointments for a given counselor, ordered by date.
     * Used by CounselorDashboardActivity for the tabbed view.
     *
     * @param counselorId The counselor's UID.
     * @param callback    Receives the appointment list on success.
     */
    public void getAppointmentsForCounselor(String counselorId,
                                             OnAppointmentsLoadedCallback callback) {
        appointmentsCollection
            .whereEqualTo("counselorId", counselorId)
            .orderBy("date")
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<Appointment> appointments = querySnapshot.toObjects(Appointment.class);
                for (int i = 0; i < appointments.size(); i++) {
                    appointments.get(i).setId(
                        querySnapshot.getDocuments().get(i).getId());
                }
                callback.onSuccess(appointments);
            })
            .addOnFailureListener(callback::onFailure);
    }

    /**
     * Fetches all appointments for a given student, ordered by date descending.
     * Used by CalendarActivity and HistoryActivity.
     *
     * @param studentId The student's UID.
     * @param callback  Receives the appointment list on success.
     */
    public void getAppointmentsForStudent(String studentId,
                                           OnAppointmentsLoadedCallback callback) {
        appointmentsCollection
            .whereEqualTo("studentId", studentId)
            .orderBy("date")
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<Appointment> appointments = querySnapshot.toObjects(Appointment.class);
                for (int i = 0; i < appointments.size(); i++) {
                    appointments.get(i).setId(
                        querySnapshot.getDocuments().get(i).getId());
                }
                callback.onSuccess(appointments);
            })
            .addOnFailureListener(callback::onFailure);
    }

    /**
     * Fetches appointments for a student on a specific date.
     * Used by CalendarActivity when a date is tapped.
     *
     * @param studentId The student's UID.
     * @param date      Date in "yyyy-MM-dd" format.
     * @param callback  Receives the appointment list on success.
     */
    public void getAppointmentsForStudentOnDate(String studentId, String date,
                                                 OnAppointmentsLoadedCallback callback) {
        appointmentsCollection
            .whereEqualTo("studentId", studentId)
            .whereEqualTo("date", date)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<Appointment> appointments = querySnapshot.toObjects(Appointment.class);
                for (int i = 0; i < appointments.size(); i++) {
                    appointments.get(i).setId(
                        querySnapshot.getDocuments().get(i).getId());
                }
                callback.onSuccess(appointments);
            })
            .addOnFailureListener(callback::onFailure);
    }

    /**
     * Updates the status of an appointment.
     * Used for marking appointments as COMPLETED, CANCELLED, or NO_SHOW.
     *
     * @param appointmentId The Firestore document ID.
     * @param newStatus     The new status string.
     * @param callback      Success/failure callback.
     */
    public void updateAppointmentStatus(String appointmentId, String newStatus,
                                         OnStatusUpdateCallback callback) {
        appointmentsCollection.document(appointmentId)
            .update("status", newStatus)
            .addOnSuccessListener(unused -> callback.onSuccess())
            .addOnFailureListener(callback::onFailure);
    }

    public interface OnStatusUpdateCallback {
        void onSuccess();
        void onFailure(Exception e);
    }
}
```

**How the transaction connects to the rest of the system:**

```
Student taps "Confirm Booking" in BookingConfirmationFragment
    │
    ▼
BookingActivity calls:
    appointmentRepository.bookAppointment(studentId, counselorId, slot, callback)
    │
    ▼
AppointmentRepository.bookAppointment() runs Firestore transaction:
    │
    ├── READ: slots/{slotId}.available
    │     ├── If false → throw "SLOT_TAKEN" → callback.onSlotTaken()
    │     │                                     → BookingActivity removes slot from UI
    │     │                                     → Toast "Slot no longer available"
    │     │
    │     └── If true →
    │           ├── WRITE: slots/{slotId}.available = false
    │           └── WRITE: appointments/{newId} = {studentId, counselorId, slotId, date, time, "CONFIRMED"}
    │
    └── Transaction success → callback.onSuccess()
                               → Toast "Appointment booked!"
                               → Navigate to StudentHomeActivity
```

---

### 4.3 `UserRepository.java` — Extension

Add a `getUserName()` method for looking up student names in appointment cards.

```java
/**
 * Fetches only the name field for a given user ID.
 * Used by AppointmentAdapter to display student names
 * on counselor dashboard appointment cards.
 *
 * @param uid      The user's Firebase Auth UID.
 * @param callback Receives the name string on success.
 */
public void getUserName(String uid, OnUserNameCallback callback) {
    usersCollection.document(uid)
        .get()
        .addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String name = doc.getString("name");
                callback.onSuccess(name != null ? name : "Unknown");
            } else {
                callback.onSuccess("Unknown");
            }
        })
        .addOnFailureListener(e -> callback.onSuccess("Unknown"));
}

public interface OnUserNameCallback {
    void onSuccess(String name);
}
```

**Why `onFailure` returns "Unknown" instead of propagating the error:** The student name is supplementary display data. If it fails, the appointment card should still render — just with "Unknown" instead of a name. Blocking the entire card render for a name lookup failure is a bad UX trade-off.

---

## 5. Implementation Details — UI Layer

### 5.1 `BookingActivity.java` — Full Rebuild

**Current state:** Flat list of all available slots. No calendar. No confirmation step. Direct Firestore calls.

**Sprint 3 target state:**

```
BookingActivity opens (from CounselorProfileActivity)
  │
  ├── Receive counselorId + counselorName from Intent
  │
  ├── AvailabilityRepository.getAvailableSlotsForCounselor(counselorId, callback)
  │     └── onSuccess(slots):
  │           schedule = AvailabilitySchedule.fromSlots(counselorId, slots)
  │           highlightCalendarDates(schedule.getDatesWithAvailability())
  │
  ├── CalendarView shows current month
  │     └── Dates with available slots are highlighted (decorated)
  │
  ├── User taps a date:
  │     └── slotsForDate = schedule.getSlotsForDate(selectedDate)
  │         └── TimeSlotAdapter.setData(slotsForDate)
  │         └── Slot RecyclerView updates below calendar
  │
  ├── User taps a slot's "Book" button:
  │     └── BookingConfirmationFragment.show()
  │           ├── Displays: counselor name, date, time
  │           └── "Confirm Booking" button
  │
  └── User taps "Confirm Booking":
        └── AppointmentRepository.bookAppointment(studentId, counselorId, slot, callback)
              ├── onSuccess() → Toast "Booked!", finish() → StudentHomeActivity
              ├── onSlotTaken() → Toast "Slot taken", remove from UI, refresh
              └── onFailure() → Toast error message
```

**Layout update — `activity_booking.xml`:**

```xml
LinearLayout (vertical)
├── Toolbar: "Book with {CounselorName}" + back arrow
│
├── CalendarView (id: calendarView)
│     └── android:layout_width="match_parent"
│     └── Minimum date = today
│
├── TextView: "Available Times" (section header, id: labelSlots)
│     └── Initially GONE, shown when a date is tapped
│
├── RecyclerView (id: recyclerSlots, horizontal)
│     └── LinearLayoutManager(HORIZONTAL)
│     └── TimeSlotAdapter
│
├── TextView: "No available slots for this date" (id: textNoSlots)
│     └── Initially GONE, shown when tapped date has zero slots
│
└── ProgressBar (id: progressBar)
      └── Shown during initial load
```

**Calendar date highlighting:**

Android's `CalendarView` does not natively support date decoration. Two approaches:

**Option A (simpler):** Use `CalendarView` as-is. When a date is tapped, check `schedule.getDatesWithAvailability().contains(dateString)`. If the date has no slots, show the "No available slots" message. No visual highlighting.

**Option B (richer):** Replace `CalendarView` with `MaterialCalendarView` from the Prolific Interactive library. This supports `DayViewDecorator` for highlighting specific dates with dots or colored backgrounds.

**Recommended for this sprint:** Option A. The visual highlighting is nice but adds a library dependency and complexity. The core requirement is functional: tapping a date shows or hides slots. Option B can be a Sprint 6 polish task.

**Date formatting:**

```java
// CalendarView returns epoch millis in onDateChange listener
calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
    // CalendarView months are 0-indexed
    String selectedDate = String.format(Locale.US, "%04d-%02d-%02d",
        year, month + 1, dayOfMonth);
    showSlotsForDate(selectedDate);
});

private void showSlotsForDate(String date) {
    List<TimeSlot> slots = schedule.getSlotsForDate(date);
    if (slots.isEmpty()) {
        textNoSlots.setVisibility(View.VISIBLE);
        recyclerSlots.setVisibility(View.GONE);
    } else {
        textNoSlots.setVisibility(View.GONE);
        recyclerSlots.setVisibility(View.VISIBLE);
        slotAdapter.setData(slots);
    }
    labelSlots.setVisibility(View.VISIBLE);
}
```

---

### 5.2 `BookingConfirmationFragment.java` — Bottom Sheet

**Purpose:** A `BottomSheetDialogFragment` shown when a student taps "Book" on a time slot. Displays the booking summary and a "Confirm" button.

```java
package com.example.moogerscouncil;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

/**
 * Bottom sheet dialog that shows a booking summary before the student
 * confirms. Displays counselor name, date, and time. The "Confirm Booking"
 * button triggers the atomic Firestore transaction via a callback to
 * the hosting BookingActivity.
 */
public class BookingConfirmationFragment extends BottomSheetDialogFragment {

    public interface OnConfirmListener {
        void onConfirm(TimeSlot slot);
    }

    private static final String ARG_COUNSELOR_NAME = "counselorName";
    private static final String ARG_DATE = "date";
    private static final String ARG_TIME = "time";

    private TimeSlot slot;
    private OnConfirmListener listener;

    public static BookingConfirmationFragment newInstance(String counselorName,
                                                          TimeSlot slot) {
        BookingConfirmationFragment fragment = new BookingConfirmationFragment();
        Bundle args = new Bundle();
        args.putString(ARG_COUNSELOR_NAME, counselorName);
        args.putString(ARG_DATE, slot.getDate());
        args.putString(ARG_TIME, slot.getTime());
        fragment.setArguments(args);
        fragment.slot = slot;
        return fragment;
    }

    public void setOnConfirmListener(OnConfirmListener listener) {
        this.listener = listener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_booking_confirmation, container, false);

        TextView textCounselor = view.findViewById(R.id.textCounselorName);
        TextView textDate = view.findViewById(R.id.textDate);
        TextView textTime = view.findViewById(R.id.textTime);
        MaterialButton buttonConfirm = view.findViewById(R.id.buttonConfirmBooking);

        Bundle args = getArguments();
        if (args != null) {
            textCounselor.setText(args.getString(ARG_COUNSELOR_NAME));
            textDate.setText(args.getString(ARG_DATE));
            textTime.setText(args.getString(ARG_TIME));
        }

        buttonConfirm.setOnClickListener(v -> {
            if (listener != null && slot != null) {
                listener.onConfirm(slot);
            }
            dismiss();
        });

        return view;
    }
}
```

**Layout — `fragment_booking_confirmation.xml`:**

```xml
LinearLayout (vertical, padding 24dp)
├── TextView: "Confirm Your Appointment" (20sp, bold, center)
├── Divider (1dp, marginTop 16dp)
│
├── LinearLayout (horizontal, marginTop 16dp)
│   ├── TextView: "Counselor:" (14sp, bold)
│   └── TextView id=textCounselorName (14sp)
│
├── LinearLayout (horizontal, marginTop 8dp)
│   ├── TextView: "Date:" (14sp, bold)
│   └── TextView id=textDate (14sp)
│
├── LinearLayout (horizontal, marginTop 8dp)
│   ├── TextView: "Time:" (14sp, bold)
│   └── TextView id=textTime (14sp)
│
├── MaterialButton id=buttonConfirmBooking (full width, primary_blue, 56dp, marginTop 24dp)
│   └── text: "Confirm Booking"
│
└── MaterialButton (text button, "Cancel", center, marginTop 8dp)
      └── dismiss()
```

**How it wires into BookingActivity:**

```java
// In BookingActivity, when TimeSlotAdapter.OnBookClickListener fires:
@Override
public void onBookClick(TimeSlot slot) {
    BookingConfirmationFragment fragment =
        BookingConfirmationFragment.newInstance(counselorName, slot);

    fragment.setOnConfirmListener(confirmedSlot -> {
        String studentId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        appointmentRepository.bookAppointment(studentId, counselorId, confirmedSlot,
            new AppointmentRepository.OnBookingCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(BookingActivity.this,
                        getString(R.string.booking_success), Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(BookingActivity.this,
                        StudentHomeActivity.class));
                    finish();
                }

                @Override
                public void onSlotTaken() {
                    Toast.makeText(BookingActivity.this,
                        getString(R.string.error_slot_taken), Toast.LENGTH_LONG).show();
                    // Remove taken slot from schedule and refresh UI
                    schedule.getSlotsForDate(confirmedSlot.getDate())
                        .remove(confirmedSlot);
                    showSlotsForDate(confirmedSlot.getDate());
                }

                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(BookingActivity.this,
                        getString(R.string.error_booking_failed), Toast.LENGTH_LONG).show();
                }
            });
    });

    fragment.show(getSupportFragmentManager(), "booking_confirm");
}
```

---

### 5.3 `CounselorDashboardActivity.java` — Tab + Stats Overhaul

**Current problems:**
1. Stats (Today/Total/Week) all show the same count — `appointments.size()` repeated three times.
2. No tab UI — shows all appointments in one flat list.
3. Direct Firestore calls — no `AppointmentRepository`.
4. Student names not looked up — shows hardcoded text.

**Sprint 3 changes:**

#### 5.3.1 Add TabLayout

In `activity_counselor_dashboard.xml`, add a `TabLayout` above the RecyclerView:

```xml
<com.google.android.material.tabs.TabLayout
    android:id="@+id/tabLayout"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:tabMode="fixed"
    app:tabGravity="fill">

    <com.google.android.material.tabs.TabItem
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/tab_today" />
    <com.google.android.material.tabs.TabItem
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/tab_this_week" />
    <com.google.android.material.tabs.TabItem
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/tab_this_month" />
</com.google.android.material.tabs.TabLayout>
```

#### 5.3.2 Date Range Filtering

All appointments are fetched once via `AppointmentRepository.getAppointmentsForCounselor()`. Tab selection filters the master list client-side:

```java
private List<Appointment> masterAppointments = new ArrayList<>();

private void filterByTab(int tabIndex) {
    String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    Calendar cal = Calendar.getInstance();

    List<Appointment> filtered;
    switch (tabIndex) {
        case 0: // Today
            filtered = masterAppointments.stream()
                .filter(a -> today.equals(a.getDate()))
                .collect(Collectors.toList());
            break;

        case 1: // This Week
            cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
            String weekStart = new SimpleDateFormat("yyyy-MM-dd", Locale.US)
                .format(cal.getTime());
            cal.add(Calendar.DAY_OF_WEEK, 6);
            String weekEnd = new SimpleDateFormat("yyyy-MM-dd", Locale.US)
                .format(cal.getTime());
            filtered = masterAppointments.stream()
                .filter(a -> a.getDate().compareTo(weekStart) >= 0
                          && a.getDate().compareTo(weekEnd) <= 0)
                .collect(Collectors.toList());
            break;

        case 2: // This Month
        default:
            String monthPrefix = today.substring(0, 7); // "yyyy-MM"
            filtered = masterAppointments.stream()
                .filter(a -> a.getDate().startsWith(monthPrefix))
                .collect(Collectors.toList());
            break;
    }

    appointmentAdapter.setData(filtered);
    updateStats(filtered.size());
}
```

#### 5.3.3 Fix Stats

Replace the three identical counts with meaningful calculations:

```java
private void updateStats(int filteredCount) {
    String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());

    long todayCount = masterAppointments.stream()
        .filter(a -> today.equals(a.getDate()))
        .count();

    long totalCount = masterAppointments.size();

    textTodayCount.setText(String.valueOf(todayCount));
    textTotalCount.setText(String.valueOf(totalCount));
    textWeekCount.setText(String.valueOf(filteredCount));
}
```

#### 5.3.4 Wire Tab Listener

```java
tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        filterByTab(tab.getPosition());
    }
    // onTabUnselected, onTabReselected: no-op
});
```

#### 5.3.5 Refactor to Use Repositories

Replace direct Firestore calls:

```java
// BEFORE (direct):
db.collection("appointments").whereEqualTo("counselorId", counselorId).get()...

// AFTER (repository):
AppointmentRepository appointmentRepo = new AppointmentRepository();
appointmentRepo.getAppointmentsForCounselor(counselorId,
    new AppointmentRepository.OnAppointmentsLoadedCallback() {
        @Override
        public void onSuccess(List<Appointment> appointments) {
            masterAppointments = appointments;
            filterByTab(0); // Default to Today tab
        }
        @Override
        public void onFailure(Exception e) {
            Toast.makeText(..., getString(R.string.error_loading_appointments), ...);
        }
    });
```

```java
// BEFORE (direct slot write):
db.collection("slots").add(slotData)...

// AFTER (repository):
AvailabilityRepository availRepo = new AvailabilityRepository();
availRepo.addSlot(counselorId, date, time,
    new AvailabilityRepository.OnSlotActionCallback() { ... });
```

---

### 5.4 `AppointmentAdapter.java` — Student Name Lookup + Status Badges

**Current problems:**
1. Student name is not looked up from `users` — shows generic text.
2. All 5 action buttons (Join, No-Show, Crisis, Profile, Notes) show placeholder toasts.
3. No status badge styling (CONFIRMED/COMPLETED/CANCELLED all look the same).

**Sprint 3 changes:**

#### 5.4.1 Student Name Lookup

```java
// In onBindViewHolder:
UserRepository userRepo = new UserRepository();
userRepo.getUserName(appointment.getStudentId(),
    name -> holder.textStudentName.setText(name));
```

**Performance note:** This makes one Firestore read per visible appointment card. For a counselor with many appointments, this could be slow. A future optimization would be to batch-fetch all student names in the Activity and pass a `Map<String, String>` to the adapter. For Sprint 3 with typical appointment counts (~5-20), the per-card lookup is acceptable.

#### 5.4.2 Status Badge Styling

```java
String status = appointment.getStatus();
if ("CONFIRMED".equals(status)) {
    holder.statusBadge.setText("Confirmed");
    holder.statusBadge.setBackgroundTintList(
        ColorStateList.valueOf(Color.parseColor("#4CAF50"))); // green
    holder.itemView.setAlpha(1.0f);
} else if ("COMPLETED".equals(status)) {
    holder.statusBadge.setText("Completed");
    holder.statusBadge.setBackgroundTintList(
        ColorStateList.valueOf(Color.parseColor("#9E9E9E"))); // gray
    holder.itemView.setAlpha(0.6f); // muted
} else if ("CANCELLED".equals(status)) {
    holder.statusBadge.setText("Cancelled");
    holder.statusBadge.setBackgroundTintList(
        ColorStateList.valueOf(Color.parseColor("#F44336"))); // red
    holder.textStudentName.setPaintFlags(
        holder.textStudentName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
}
```

#### 5.4.3 No-Show Action Button

Wire the No-Show button to update appointment status via `AppointmentRepository`:

```java
holder.buttonNoShow.setOnClickListener(v -> {
    appointmentRepository.updateAppointmentStatus(
        appointment.getId(), "NO_SHOW",
        new AppointmentRepository.OnStatusUpdateCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(context, "Marked as No Show", Toast.LENGTH_SHORT).show();
                // Refresh the adapter
            }
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(context, "Failed to update", Toast.LENGTH_SHORT).show();
            }
        });
});
```

The remaining action buttons (Join, Notes, Profile) remain as placeholder toasts for Sprint 3 — they depend on features (video calls, session records) deferred to Phase 4.

---

### 5.5 `AvailabilitySetupActivity.java` — Counselor Slot Management

**Purpose:** Dedicated screen where a counselor can view their existing slots, add new ones, and remove old ones. Replaces the inline DatePicker/TimePicker currently embedded in `CounselorDashboardActivity`.

**Layout — `activity_availability_setup.xml`:**

```xml
LinearLayout (vertical)
├── Toolbar: "Manage Availability" + back arrow
│
├── MaterialButton: "Add New Slot" (outlined, full width, marginTop 16dp)
│     └── Opens DatePickerDialog → TimePickerDialog → AvailabilityRepository.addSlot()
│
├── TextView: "Your Time Slots" (section header, 16sp, bold, marginTop 16dp)
│
├── RecyclerView (id: recyclerSlots, vertical)
│     └── Each item shows: date, time, status (Available/Booked)
│     └── Swipe-to-delete for available slots only
│
└── TextView: "No slots added yet" (id: textEmptySlots, GONE initially)
```

**Runtime behavior:**

```
AvailabilitySetupActivity opens
  │
  ├── counselorId = FirebaseAuth.getCurrentUser().getUid()
  ├── AvailabilityRepository.getSlotsForCounselor(counselorId, callback)
  │     └── onSuccess: display slots in RecyclerView, group by date
  │
  ├── "Add New Slot" tap:
  │     ├── DatePickerDialog → user picks date
  │     ├── TimePickerDialog → user picks time
  │     └── AvailabilityRepository.addSlot(counselorId, date, time, callback)
  │           ├── onSuccess: refresh slot list, Toast "Slot added"
  │           └── onFailure: Toast error
  │
  └── Swipe-to-delete on an available slot:
        └── AvailabilityRepository.removeSlot(slotId, callback)
              ├── onSuccess: remove from adapter, Toast "Slot removed"
              └── onFailure: Toast error, restore item
```

**Entry point change:** In `CounselorDashboardActivity`, the existing "Add Availability Slot" banner should navigate to `AvailabilitySetupActivity` instead of showing the inline DatePicker. The inline picker can be kept as a quick-add shortcut, but the dedicated screen provides full management capabilities.

```java
// In CounselorDashboardActivity
bannerAddSlot.setOnClickListener(v -> {
    startActivity(new Intent(this, AvailabilitySetupActivity.class));
});
```

Override `onResume()` to refresh the appointment list when returning from the setup screen:

```java
@Override
protected void onResume() {
    super.onResume();
    loadAppointments(); // Re-fetch to reflect any slot changes
}
```

---

### 5.6 `EmergencyDialogFragment.java` — Crisis Dial Dialog

**Purpose:** A `DialogFragment` that shows crisis line phone numbers with `ACTION_DIAL` intents. Zero network dependency — works offline.

```java
package com.example.moogerscouncil;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

/**
 * Emergency contact dialog accessible from the student home screen.
 * Displays crisis line phone numbers and campus emergency contacts.
 * Tapping a number triggers ACTION_DIAL, handing off to the phone app.
 *
 * This dialog has zero network dependencies — it must work offline.
 * Phone numbers are loaded from string resources so they can be
 * updated without code changes.
 *
 * Implements the CrisisIntervention CRC card's dial-only responsibility.
 * Audit logging (per CRC card) is deferred to Phase 4.
 */
public class EmergencyDialogFragment extends DialogFragment {

    public static EmergencyDialogFragment newInstance() {
        return new EmergencyDialogFragment();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(getString(R.string.emergency_title))
            .setMessage(getString(R.string.emergency_message))
            .setPositiveButton(getString(R.string.call_crisis_line), (dialog, which) -> {
                dialNumber(getString(R.string.crisis_line_number));
            })
            .setNeutralButton(getString(R.string.call_campus_security), (dialog, which) -> {
                dialNumber(getString(R.string.campus_security_number));
            })
            .setNegativeButton(getString(R.string.dismiss), (dialog, which) -> {
                dismiss();
            });
        return builder.create();
    }

    private void dialNumber(String phoneNumber) {
        Intent dialIntent = new Intent(Intent.ACTION_DIAL,
            Uri.parse("tel:" + phoneNumber));
        startActivity(dialIntent);
    }
}
```

**Why `ACTION_DIAL` and not `ACTION_CALL`:** `ACTION_DIAL` opens the dialer with the number pre-filled but does not make the call. This avoids needing the `CALL_PHONE` permission and gives the user a final confirmation step before dialing — appropriate for an emergency context where an accidental tap shouldn't immediately place a call.

**Wiring into `StudentHomeActivity`:**

Add a red FloatingActionButton to `activity_student_home.xml`:

```xml
<com.google.android.material.floatingactionbutton.FloatingActionButton
    android:id="@+id/fabEmergency"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_gravity="bottom|end"
    android:layout_margin="16dp"
    android:contentDescription="@string/emergency_button_desc"
    android:src="@android:drawable/ic_dialog_alert"
    app:backgroundTint="#C0392B"
    app:tint="@android:color/white"
    app:layout_constraintBottom_toTopOf="@id/bottomNav"
    app:layout_constraintEnd_toEndOf="parent" />
```

In `StudentHomeActivity.java`:

```java
FloatingActionButton fabEmergency = findViewById(R.id.fabEmergency);
fabEmergency.setOnClickListener(v -> {
    EmergencyDialogFragment.newInstance()
        .show(getSupportFragmentManager(), "emergency");
});
```

The existing inline crisis banner can remain as supplementary information. The FAB ensures the emergency contact is **always one tap away** regardless of scroll position.

---

### 5.7 Refactoring `CalendarActivity.java` and `HistoryActivity.java`

Both activities currently use direct Firestore calls. Refactor to use `AppointmentRepository`:

**CalendarActivity:**
```java
// BEFORE:
db.collection("appointments")
    .whereEqualTo("studentId", studentId)
    .whereEqualTo("date", date)
    .get()...

// AFTER:
AppointmentRepository repo = new AppointmentRepository();
repo.getAppointmentsForStudentOnDate(studentId, date,
    new AppointmentRepository.OnAppointmentsLoadedCallback() { ... });
```

**HistoryActivity:**
```java
// BEFORE:
db.collection("appointments")
    .whereEqualTo("studentId", studentId)
    .orderBy("date", Query.Direction.DESCENDING)
    .get()...

// AFTER:
AppointmentRepository repo = new AppointmentRepository();
repo.getAppointmentsForStudent(studentId,
    new AppointmentRepository.OnAppointmentsLoadedCallback() { ... });
```

---

## 6. Implementation Details — Strings

All new user-facing text for Sprint 3:

```xml
<!-- Booking Flow (US-01) -->
<string name="title_booking">Book with %s</string>
<string name="label_available_times">Available Times</string>
<string name="no_slots_for_date">No available slots for this date.</string>
<string name="no_slots_available">This counselor has no available slots at the moment.</string>
<string name="booking_success">Appointment booked successfully!</string>
<string name="error_slot_taken">This slot was just booked by someone else. Please choose another.</string>
<string name="error_booking_failed">Unable to book appointment. Please try again.</string>
<string name="error_loading_slots">Unable to load available slots.</string>
<string name="confirm_booking_title">Confirm Your Appointment</string>
<string name="label_counselor">Counselor:</string>
<string name="label_date">Date:</string>
<string name="label_time">Time:</string>
<string name="button_confirm_booking">Confirm Booking</string>
<string name="button_cancel">Cancel</string>

<!-- Counselor Dashboard Tabs (US-05/US-10) -->
<string name="tab_today">Today</string>
<string name="tab_this_week">This Week</string>
<string name="tab_this_month">This Month</string>
<string name="error_loading_appointments">Unable to load appointments.</string>
<string name="status_confirmed">Confirmed</string>
<string name="status_completed">Completed</string>
<string name="status_cancelled">Cancelled</string>
<string name="status_no_show">No Show</string>
<string name="marked_no_show">Marked as No Show</string>

<!-- Availability Setup (US-05/US-10) -->
<string name="title_manage_availability">Manage Availability</string>
<string name="button_add_slot">Add New Slot</string>
<string name="label_your_slots">Your Time Slots</string>
<string name="no_slots_added">No slots added yet.</string>
<string name="slot_added">Slot added successfully.</string>
<string name="slot_removed">Slot removed.</string>
<string name="error_adding_slot">Failed to add slot.</string>
<string name="error_removing_slot">Failed to remove slot.</string>

<!-- Emergency Button (US-20) -->
<string name="emergency_title">Emergency Contacts</string>
<string name="emergency_message">If you or someone you know is in immediate danger, please contact one of the following services.</string>
<string name="call_crisis_line">Call Crisis Line</string>
<string name="call_campus_security">Campus Emergency</string>
<string name="crisis_line_number">0311-7786264</string>
<string name="campus_security_number">042-35608000</string>
<string name="dismiss">Dismiss</string>
<string name="emergency_button_desc">Emergency contact button</string>
```

---

## 7. Data Flow Diagrams

### 7.1 Complete Booking Flow (End-to-End)

```
Student             CounselorProfile   BookingActivity     AvailabilityRepo   AppointmentRepo    Firestore
  │                       │                  │                   │                  │                │
  │  taps "Book"         │                  │                   │                  │                │
  │ ─────────────────►   │                  │                   │                  │                │
  │                       │  Intent(         │                   │                  │                │
  │                       │  counselorId,    │                   │                  │                │
  │                       │  counselorName)  │                   │                  │                │
  │                       │ ────────────────►│                   │                  │                │
  │                       │                  │                   │                  │                │
  │                       │                  │ getAvailableSlots │                  │                │
  │                       │                  │ ─────────────────►│                  │                │
  │                       │                  │                   │  query slots     │                │
  │                       │                  │                   │ ─────────────────┼───────────────►│
  │                       │                  │                   │  slot list       │                │
  │                       │                  │                   │ ◄────────────────┼────────────────│
  │                       │                  │ onSuccess(slots)  │                  │                │
  │                       │                  │ ◄─────────────────│                  │                │
  │                       │                  │                   │                  │                │
  │                       │                  │ schedule =        │                  │                │
  │                       │                  │ AvailabilitySchedule                 │                │
  │                       │                  │ .fromSlots(slots) │                  │                │
  │                       │                  │                   │                  │                │
  │  sees calendar       │                  │                   │                  │                │
  │ ◄────────────────────┼──────────────────│                   │                  │                │
  │                       │                  │                   │                  │                │
  │  taps date           │                  │                   │                  │                │
  │ ─────────────────────┼─────────────────►│                   │                  │                │
  │                       │                  │ getSlotsForDate() │                  │                │
  │  sees time slots     │                  │ (local, instant)  │                  │                │
  │ ◄────────────────────┼──────────────────│                   │                  │                │
  │                       │                  │                   │                  │                │
  │  taps slot "Book"    │                  │                   │                  │                │
  │ ─────────────────────┼─────────────────►│                   │                  │                │
  │                       │                  │ show Confirmation │                  │                │
  │  sees summary sheet  │                  │ BottomSheet       │                  │                │
  │ ◄────────────────────┼──────────────────│                   │                  │                │
  │                       │                  │                   │                  │                │
  │  taps "Confirm"      │                  │                   │                  │                │
  │ ─────────────────────┼─────────────────►│                   │                  │                │
  │                       │                  │                   │ bookAppointment  │                │
  │                       │                  │ ──────────────────┼─────────────────►│                │
  │                       │                  │                   │                  │ runTransaction │
  │                       │                  │                   │                  │ ──────────────►│
  │                       │                  │                   │                  │   slot=false   │
  │                       │                  │                   │                  │   appt=new     │
  │                       │                  │                   │                  │ ◄──────────────│
  │                       │                  │ onSuccess()       │                  │                │
  │                       │                  │ ◄─────────────────┼──────────────────│                │
  │  Toast "Booked!"     │                  │                   │                  │                │
  │  → StudentHome       │                  │                   │                  │                │
  │ ◄────────────────────┼──────────────────│                   │                  │                │
```

### 7.2 Counselor Dashboard Load Flow

```
Counselor          CounselorDashboard    AppointmentRepo    UserRepository    Firestore
  │                       │                    │                  │                │
  │  logs in (role=       │                    │                  │                │
  │  counselor)           │                    │                  │                │
  │ ─────────────────────►│                    │                  │                │
  │                       │ getAppointments    │                  │                │
  │                       │ ForCounselor(id)   │                  │                │
  │                       │ ──────────────────►│                  │                │
  │                       │                    │  query appts     │                │
  │                       │                    │ ────────────────►│───────────────►│
  │                       │                    │  appt list       │                │
  │                       │                    │ ◄────────────────│◄───────────────│
  │                       │ onSuccess(appts)   │                  │                │
  │                       │ ◄──────────────────│                  │                │
  │                       │                    │                  │                │
  │                       │ masterAppts = appts│                  │                │
  │                       │ filterByTab(0)     │                  │                │
  │                       │ → Today filter     │                  │                │
  │                       │                    │                  │                │
  │                       │ AppointmentAdapter │                  │                │
  │                       │ .setData(filtered) │                  │                │
  │                       │                    │                  │                │
  │                       │ For each card:     │                  │                │
  │                       │   getUserName(     │                  │                │
  │                       │   studentId)       │ ────────────────►│                │
  │                       │                    │                  │  get user      │
  │                       │                    │                  │ ──────────────►│
  │                       │                    │                  │  name          │
  │                       │                    │                  │ ◄──────────────│
  │                       │   setText(name)    │ ◄────────────────│                │
  │                       │                    │                  │                │
  │  sees tabbed dashboard│                    │                  │                │
  │ ◄─────────────────────│                    │                  │                │
```

---

## 8. Testing Plan

### 8.1 Unit Tests

#### `AvailabilityScheduleTest.java`

| Test | What it verifies |
|---|---|
| `testFromSlotsFiltersUnavailable` | `fromSlots()` with a mix of available/unavailable slots only includes available ones |
| `testGetSlotsForDateReturnsCorrectSubset` | Slots for a specific date match expected count and content |
| `testGetSlotsForDateReturnsEmptyForNoSlots` | A date with no slots returns an empty list, not null |
| `testGetDatesWithAvailability` | Returns correct set of dates that have available slots |
| `testEmptySlotListProducesEmptySchedule` | `fromSlots()` with empty list produces schedule with no dates |

#### `AppointmentTest.java`

| Test | What it verifies |
|---|---|
| `testConstructorSetsAllFields` | All fields are accessible after construction |
| `testEmptyConstructorForFirestore` | No-arg constructor creates instance with null fields |
| `testStatusValues` | Status can be set to CONFIRMED, COMPLETED, CANCELLED |
| `testSettersOverrideFields` | Each setter correctly updates its field |

### 8.2 UI Tests (Espresso)

#### `BookingFlowTest.java`

| Test | What it asserts |
|---|---|
| `testCalendarViewIsDisplayed` | `CalendarView` is visible after BookingActivity opens |
| `testSlotListAppearsOnDateTap` | Tapping a date shows the slot RecyclerView |
| `testBookButtonOpensConfirmation` | Tapping "Book" on a slot shows the confirmation bottom sheet |
| `testConfirmationShowsCorrectDetails` | Bottom sheet displays counselor name, date, time |

#### `EmergencyButtonTest.java`

| Test | What it asserts |
|---|---|
| `testEmergencyFabIsVisible` | Red FAB is displayed on StudentHomeActivity |
| `testFabTapOpensDialog` | Tapping FAB shows EmergencyDialogFragment |
| `testDialogShowsCrisisLineOption` | "Call Crisis Line" button is visible in dialog |
| `testDialogShowsCampusSecurityOption` | "Campus Emergency" button is visible in dialog |

#### `CounselorDashboardTest.java`

| Test | What it asserts |
|---|---|
| `testTabLayoutIsDisplayed` | Three tabs (Today, This Week, This Month) are visible |
| `testAppointmentListLoads` | RecyclerView has items after Firestore fetch |
| `testTabSwitchChangesContent` | Switching tabs updates the displayed appointment count |

---

## 9. Task Breakdown and Sequencing

```
┌──────────────────────────────────────────────────────┐
│ Phase A — Repository Foundation (no UI dependency)    │
│                                                      │
│  A1. AvailabilityRepository.java                     │
│  A2. AppointmentRepository.java                      │
│  A3. AvailabilitySchedule.java                       │
│  A4. UserRepository.java — add getUserName()         │
│  A5. AvailabilityScheduleTest.java + AppointmentTest │
│                                                      │
│  All can be done in parallel. No UI changes yet.     │
└──────────────────────┬───────────────────────────────┘
                       │
┌──────────────────────▼───────────────────────────────┐
│ Phase B — Booking Flow UI (depends on A1, A2, A3)    │
│                                                      │
│  B1. BookingActivity — calendar + date-based slots   │
│  B2. BookingConfirmationFragment — bottom sheet      │
│  B3. activity_booking.xml update                     │
│  B4. fragment_booking_confirmation.xml               │
│  B5. strings.xml — booking strings                   │
│                                                      │
│  B1/B2 can be done in parallel.                      │
└──────────────────────┬───────────────────────────────┘
                       │
┌──────────────────────▼───────────────────────────────┐
│ Phase C — Dashboard + Emergency (depends on A2, A4)  │
│                                                      │
│  C1. CounselorDashboardActivity — tabs, stats,       │
│      refactor to AppointmentRepo + AvailabilityRepo  │
│  C2. AppointmentAdapter — student name lookup,       │
│      status badges, No-Show button                   │
│  C3. AvailabilitySetupActivity + layout              │
│  C4. EmergencyDialogFragment + FAB in StudentHome    │
│  C5. activity_counselor_dashboard.xml — TabLayout    │
│  C6. AndroidManifest + strings                       │
│                                                      │
│  C1/C2 depend on each other.                         │
│  C3 depends on A1. C4 is independent.                │
└──────────────────────┬───────────────────────────────┘
                       │
┌──────────────────────▼───────────────────────────────┐
│ Phase D — Refactor + Testing                         │
│                                                      │
│  D1. Refactor CalendarActivity → AppointmentRepo     │
│  D2. Refactor HistoryActivity → AppointmentRepo      │
│  D3. BookingFlowTest.java (Espresso)                 │
│  D4. EmergencyButtonTest.java (Espresso)             │
│  D5. CounselorDashboardTest.java (Espresso)          │
│  D6. Javadoc pass on all new files                   │
│  D7. Move stories to Done on Kanban board            │
│                                                      │
│  D1/D2 are independent. D3-D5 are independent.      │
└──────────────────────────────────────────────────────┘
```

---

## 10. Acceptance Criteria Checklist

### US-01 — View Available Slots and Book Appointment

- [ ] From counselor profile, student taps "Book Appointment" → BookingActivity opens
- [ ] CalendarView displays; tapping a date shows available time slots for that day
- [ ] Dates with no available slots show "No available slots for this date"
- [ ] Tapping a time slot shows BookingConfirmationFragment with counselor name, date, time
- [ ] "Confirm Booking" creates appointment (CONFIRMED) + marks slot unavailable atomically via Firestore transaction
- [ ] On booking success: Toast + navigate to StudentHomeActivity
- [ ] If slot was taken (race condition): Toast "Slot taken", remove slot from UI
- [ ] Already-booked slots are never shown
- [ ] All Firestore ops go through `AppointmentRepository` and `AvailabilityRepository`
- [ ] All strings in `strings.xml`
- [ ] Javadoc on all new public methods

### US-05/US-10 — Counselor Appointment Dashboard

- [ ] Counselor lands on CounselorDashboardActivity after login
- [ ] Three tabs: Today, This Week, This Month
- [ ] Each tab filters appointments by relevant date range
- [ ] Each appointment card shows: student name (from `users`), date, time, status badge
- [ ] CONFIRMED = full color, COMPLETED = muted, CANCELLED = struck through
- [ ] "My Availability" navigates to AvailabilitySetupActivity
- [ ] Counselor can add/remove slots in AvailabilitySetupActivity
- [ ] Dashboard refreshes on return from AvailabilitySetupActivity
- [ ] Stats show correct Today / Total / This Week counts
- [ ] No-Show button updates appointment status in Firestore
- [ ] All Firestore ops go through repositories

### US-20 — Emergency Button

- [ ] Red emergency FAB always visible on StudentHomeActivity (not hidden behind nav)
- [ ] Tapping FAB shows EmergencyDialogFragment immediately (no loading, no network)
- [ ] Dialog offers "Call Crisis Line" and "Campus Emergency" — both trigger ACTION_DIAL
- [ ] "Dismiss" button closes dialog
- [ ] Phone numbers stored in `strings.xml`, not hardcoded in Java
- [ ] Works offline — zero Firestore dependency

---

## 11. Definition of Done (Sprint 3)

| # | Criterion | How to verify |
|---|---|---|
| 1 | All acceptance criteria pass when manually tested | Demo walkthrough: register → browse → book → counselor sees it → emergency button |
| 2 | Firestore reads/writes are live (no stubbed data) | Code review — all through repositories |
| 3 | Unit tests pass | `./gradlew test` |
| 4 | Intent tests for booking, emergency, dashboard pass | `./gradlew connectedAndroidTest` |
| 5 | File header comment in every new `.java` file | Code review |
| 6 | Javadoc on all new model/repository public methods | Code review |
| 7 | No hardcoded user-facing strings | All in `strings.xml` |
| 8 | All Firestore calls have `.addOnFailureListener` | Code review |
| 9 | PR linked to GitHub Issue, reviewed by teammate | GitHub |
| 10 | Stories moved to "Done" on Kanban board | Assignee |
| 11 | **Repository pattern enforced** — no direct `db.collection()` calls in any Activity | Code review (new requirement for Sprint 3) |
