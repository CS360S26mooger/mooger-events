# Sprint 9 — Counselor Dashboard UI Overhaul + Notes Hardening + No-Show Safeguards + Waitlist Spillover
### Counselor-Side Polish, Consistency, and Remaining Sprint 8 Integration

---

## 0. Pre-Sprint Status Report

### Sprint 8 Review — Waitlist Preference Flow: COMPLETE

Sprint 8 delivered the preference-based waitlist request form, student waitlist tab, counselor waitlist queue, and auto-resolution when slots are created. The following is now in place:

| Deliverable | Status |
|---|---|
| `WaitlistEntry.java` with preference fields | Done |
| `WaitlistMatcher.java` | Done |
| `WaitlistRepository.java` overhauled | Done |
| `WaitlistRequestActivity.java` | Done |
| `StudentWaitlistActivity.java` | Done |
| `CounselorWaitlistActivity.java` | Done |
| Auto-resolution in `AvailabilitySetupActivity` | Done |
| Legacy offer-on-cancel removed | Done |

### Current Counselor Dashboard State — Problems Identified

| Problem | Location | Impact |
|---|---|---|
| **Notes are write-only (duplicates).** Counselor can open the notes dialog for the same student/appointment repeatedly and each save writes a *new* Firestore document. No check for existing notes; no ability to edit. | `AppointmentAdapter.showNoteDialog()`, `SessionNoteRepository.saveNote()` | Duplicate notes accumulate in `sessionNotes` collection. |
| **No-Show button has no status guard.** A counselor can mark a CANCELLED appointment as NO_SHOW. The button is always visible regardless of current status. | `AppointmentAdapter.markNoShow()` | Data integrity issue — cancelled appointments should never be marked no-show. |
| **"Join" button is a non-functional placeholder.** Shows a "Phase 4" toast. | `AppointmentAdapter`, `item_appointment.xml` | Confusing dead UI. Feature covered by secure messaging (Sprint 10). Remove. |
| **Notes dialog has raw Material default styling.** Does not match the student-side pastel pink/white rounded design language. | `dialog_session_note.xml`, `AppointmentAdapter` dialog inflation | Visual inconsistency. |
| **Logout has no confirmation.** Tapping logout immediately signs out and redirects. Student side has a two-step popup confirmation. | `CounselorDashboardActivity` logout button handler | UX inconsistency, risk of accidental logout. |
| **"Total Patients" stat is mislabeled.** Actually shows total appointment count, not unique student count. Students booked back-to-back appear as duplicates. | `CounselorDashboardActivity.updateStats()` | Misleading counselor-facing data. |
| **Visual inconsistency.** Dashboard uses square/angular elements, non-matching icon styles, and a different color vocabulary than the student side (pastel pink, rounded corners, minimalist icons). | `activity_counselor_dashboard.xml`, `item_appointment.xml` | Professional appearance gap. |

---

## 1. Sprint 9 Objective

By the end of this sprint:

1. **Session notes are upsert-based** — the dialog checks if a note already exists for that appointment. If yes, it loads the existing text and saves via `update()`. If no, it creates a new document. One note per appointment, editable in place.
2. **No-Show button is guarded** — it is hidden or disabled for appointments with status CANCELLED or already NO_SHOW. Only CONFIRMED appointments can be marked no-show.
3. **"Join" button is removed** from the appointment card layout and adapter. Secure messaging (Sprint 10) handles meeting link delivery.
4. **Notes dialog UI matches the student-side design** — pastel pink/white palette, rounded corners, matching typography and spacing.
5. **Counselor logout has a two-step confirmation popup** identical to the student side — AlertDialog with "Are you sure?" before signing out.
6. **"Total Patients" becomes "Total Bookings"** — label renamed and the stat correctly shows total appointments (or optionally, unique students — user decided on "Total Bookings").
7. **Counselor dashboard UI is overhauled** — rounded elements, pastel pink/white/purple accent palette inline with the student side, minimalist icons replacing the current heavier ones, the same logout icon as the student side.
8. **Appointment card (`item_appointment.xml`) is redesigned** — rounded card, consistent color theming, action buttons reformatted without the Join button, cleaner status badges.
9. **Remaining Sprint 8 waitlist spillover** — any unfinished integration from the waitlist hardening sprint (if `CounselorWaitlistActivity` navigation from dashboard, or the waitlist guard preventing requests when slots exist, or student waitlist tab navigation from home — if incomplete) is resolved.

---

## 2. What Already Exists vs. What Sprint 9 Builds

| Component | Current State | Sprint 9 Target |
|---|---|---|
| `SessionNoteRepository.java` | `saveNote()` only — always creates new doc. No `updateNote()`. No `getNoteForAppointment()` (only `getNotesForAppointment()` plural). | Add `getNoteForAppointment()` returning single note or null. Add `updateNote()`. Rename/adjust `saveNote()` or keep as create-only. |
| `AppointmentAdapter.java` — notes dialog | Always inflates empty dialog. Never checks if note exists. On save, always creates new. | Load existing note on dialog open. If exists, pre-fill. On save, call update or create based on existence. |
| `AppointmentAdapter.java` — No-Show button | Always visible. No status check before `markNoShow()`. | Hide button if status is CANCELLED or NO_SHOW or COMPLETED. Only show for CONFIRMED. |
| `AppointmentAdapter.java` — Join button | Shows placeholder toast. | Remove entirely from layout and adapter. |
| `dialog_session_note.xml` | Default Material AlertDialog with ChipGroup + EditText. No custom styling. | Custom dialog layout matching student-side design (pink header, white body, rounded corners, pink accent buttons). |
| `CounselorDashboardActivity.java` — logout | Immediate `mAuth.signOut()` + navigate. | Show AlertDialog "Are you sure you want to logout?" → Yes/Cancel, matching student-side `dialog_exit.xml` style. |
| `CounselorDashboardActivity.java` — stats | "Total Patients" shows `masterAppointments.size()`. | Rename to "Total Bookings". Keep same computation (total appointments, which is what the user wants). |
| `activity_counselor_dashboard.xml` | Square-ish cards, mixed colors, non-matching icons. | Rounded cards (20dp), pastel pink/purple/white palette, minimalist icon set matching student side. |
| `item_appointment.xml` | 5 buttons (Join, NoShow, Crisis, Profile, Notes), angular styling. | 4 buttons (NoShow, Crisis, Profile, Notes), rounded, pastel-themed, conditionally visible. |

---

## 3. Files to Create or Modify

### 3.1 New Files

```text
src/main/res/layout/
├── dialog_session_note_styled.xml       // Redesigned notes dialog matching student-side design

src/main/res/drawable/
├── ic_logout_minimal.xml                // Minimalist logout icon (matching student side)
├── bg_rounded_card_pink.xml             // Rounded pink background for stat cards
├── bg_rounded_card_white.xml            // Rounded white background for appointment cards
├── bg_button_pastel_pink.xml            // Rounded button background
```

### 3.2 Files to Modify

```text
SessionNoteRepository.java          // Add getNoteForAppointment(), updateNote()
AppointmentAdapter.java             // Notes dialog upsert, no-show guard, remove Join, styling
CounselorDashboardActivity.java     // Logout confirmation, "Total Bookings" rename, UI references
activity_counselor_dashboard.xml    // Full visual overhaul — rounded elements, pastel palette, minimalist icons
item_appointment.xml                // Remove Join button, restyle remaining buttons, round card
dialog_session_note.xml             // Replace with styled version or redirect inflation to new layout
strings.xml                         // Update stat label, add logout confirmation strings
```

---

## 4. Implementation Details — Session Notes Upsert

### 4.1 `SessionNoteRepository.java` — New Methods

```java
/**
 * Fetches the single note for a specific appointment, if one exists.
 * Returns null via callback if no note has been written yet.
 *
 * @param appointmentId The appointment to look up.
 * @param callback Returns the existing note or null.
 */
public void getNoteForAppointment(String appointmentId, OnSingleNoteCallback callback) {
    notesCollection.whereEqualTo("appointmentId", appointmentId)
            .limit(1)
            .get()
            .addOnSuccessListener(snapshot -> {
                if (snapshot.isEmpty()) {
                    callback.onSuccess(null);
                    return;
                }
                SessionNote note = snapshot.getDocuments().get(0).toObject(SessionNote.class);
                if (note != null) {
                    note.setId(snapshot.getDocuments().get(0).getId());
                }
                callback.onSuccess(note);
            })
            .addOnFailureListener(callback::onFailure);
}

public interface OnSingleNoteCallback {
    void onSuccess(SessionNote note); // null if no existing note
    void onFailure(Exception e);
}

/**
 * Updates an existing note document in place.
 *
 * @param noteId The document ID of the existing note.
 * @param newText The updated note text.
 * @param templateKey The updated template key (may be unchanged).
 * @param callback Success/failure.
 */
public void updateNote(String noteId, String newText, String templateKey,
                       OnNoteActionCallback callback) {
    java.util.Map<String, Object> updates = new java.util.HashMap<>();
    updates.put("noteText", newText);
    updates.put("templateKey", templateKey);
    updates.put("updatedAt", com.google.firebase.Timestamp.now());
    notesCollection.document(noteId)
            .update(updates)
            .addOnSuccessListener(unused -> callback.onSuccess(noteId))
            .addOnFailureListener(callback::onFailure);
}
```

### 4.2 `AppointmentAdapter.java` — Notes Dialog Rewrite

```java
private void showNoteDialog(Appointment apt) {
    SessionNoteRepository noteRepo = new SessionNoteRepository();

    // First check if a note already exists for this appointment
    noteRepo.getNoteForAppointment(apt.getId(), new SessionNoteRepository.OnSingleNoteCallback() {
        @Override
        public void onSuccess(SessionNote existingNote) {
            // Inflate the styled dialog
            View dialogView = LayoutInflater.from(context)
                    .inflate(R.layout.dialog_session_note_styled, null);
            ChipGroup templateChips = dialogView.findViewById(R.id.chipGroupNoteTemplates);
            EditText noteEdit = dialogView.findViewById(R.id.editSessionNote);
            final String[] selectedTemplate = {NoteTemplate.GENERAL_SESSION};

            // Pre-fill if note exists
            if (existingNote != null) {
                noteEdit.setText(existingNote.getNoteText());
                if (existingNote.getTemplateKey() != null) {
                    selectedTemplate[0] = existingNote.getTemplateKey();
                }
            }

            // Build template chips
            for (String key : NoteTemplate.ALL_KEYS) { ... }

            AlertDialog dialog = new AlertDialog.Builder(context, R.style.PinkDialogTheme)
                    .setView(dialogView)
                    .setPositiveButton(R.string.save_note, (d, w) -> {
                        String text = noteEdit.getText().toString().trim();
                        if (text.isEmpty()) {
                            Toast.makeText(context, R.string.note_empty_error, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (existingNote != null) {
                            // UPDATE existing note
                            noteRepo.updateNote(existingNote.getId(), text, selectedTemplate[0],
                                    new SessionNoteRepository.OnNoteActionCallback() {
                                @Override public void onSuccess(String noteId) {
                                    Toast.makeText(context, R.string.note_updated, Toast.LENGTH_SHORT).show();
                                }
                                @Override public void onFailure(Exception e) {
                                    Toast.makeText(context, R.string.note_save_error, Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            // CREATE new note
                            SessionNote note = new SessionNote(apt.getId(), apt.getCounselorId(),
                                    apt.getStudentId(), selectedTemplate[0], text);
                            noteRepo.saveNote(note, new SessionNoteRepository.OnNoteActionCallback() {
                                @Override public void onSuccess(String noteId) {
                                    Toast.makeText(context, R.string.note_saved, Toast.LENGTH_SHORT).show();
                                }
                                @Override public void onFailure(Exception e) {
                                    Toast.makeText(context, R.string.note_save_error, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .create();
            dialog.show();
        }

        @Override
        public void onFailure(Exception e) {
            Toast.makeText(context, R.string.note_load_error, Toast.LENGTH_SHORT).show();
        }
    });
}
```

---

## 5. Implementation Details — No-Show Status Guard

### 5.1 `AppointmentAdapter.java` — Conditional Button Visibility

In `onBindViewHolder()`, after setting up the appointment data:

```java
// Guard: No-Show button only for CONFIRMED appointments
String status = apt.getStatus();
if ("CANCELLED".equals(status) || "NO_SHOW".equals(status) || "COMPLETED".equals(status)) {
    holder.noShowButton.setVisibility(View.GONE);
} else {
    holder.noShowButton.setVisibility(View.VISIBLE);
}

// Guard: Crisis only for CONFIRMED (active session)
if ("CANCELLED".equals(status) || "COMPLETED".equals(status)) {
    holder.crisisButton.setVisibility(View.GONE);
} else {
    holder.crisisButton.setVisibility(View.VISIBLE);
}
```

### 5.2 Additional Defence in `markNoShow()`

Even with the button hidden, add a programmatic check at the start of `markNoShow()`:

```java
private void markNoShow(Appointment apt, ViewHolder holder, int position) {
    if (!"CONFIRMED".equals(apt.getStatus())) {
        return; // Safety — should never reach here if button is properly hidden
    }
    // ... existing logic
}
```

---

## 6. Implementation Details — Join Button Removal

### 6.1 `item_appointment.xml`

Remove the `joinButton` element entirely from the first action button row. The remaining two buttons in that row (Profile and Notes) should take equal width:

```xml
<!-- First action button row (was 3 buttons, now 2) -->
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="horizontal"
    android:layout_marginTop="8dp">

    <com.google.android.material.button.MaterialButton
        android:id="@+id/profileButton"
        android:layout_width="0dp"
        android:layout_weight="1"
        ... />

    <com.google.android.material.button.MaterialButton
        android:id="@+id/notesButton"
        android:layout_width="0dp"
        android:layout_weight="1"
        ... />
</LinearLayout>
```

### 6.2 `AppointmentAdapter.java`

Remove all references to `joinButton`:
- Remove `holder.joinButton = itemView.findViewById(R.id.joinButton);`
- Remove `holder.joinButton.setOnClickListener(...)` and the placeholder toast
- Remove the field declaration in the ViewHolder

---

## 7. Implementation Details — Logout Confirmation Popup

### 7.1 `CounselorDashboardActivity.java`

Replace the current immediate logout with a two-step confirmation matching the student side:

```java
private void showLogoutConfirmation() {
    new AlertDialog.Builder(this, R.style.PinkDialogTheme)
            .setTitle(R.string.logout_confirm_title)
            .setMessage(R.string.logout_confirm_message)
            .setPositiveButton(R.string.logout_confirm_yes, (dialog, which) -> {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(CounselorDashboardActivity.this, LoginActivity.class));
                finish();
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
}
```

Wire the logout button:

```java
// Replace: logoutBtn.setOnClickListener(v -> { mAuth.signOut(); ... })
// With:
logoutBtn.setOnClickListener(v -> showLogoutConfirmation());
```

### 7.2 Logout Icon

Replace the current logout icon with the same minimalist icon used on the student side. Check `StudentHomeActivity`'s logout reference and use the same drawable (likely `ic_logout` or the eye-exit icon). If a matching drawable doesn't exist for this purpose, create `ic_logout_minimal.xml` as a simple door-with-arrow vector.

---

## 8. Implementation Details — "Total Bookings" Rename

### 8.1 `activity_counselor_dashboard.xml`

Change the label text in the stats card:

```xml
<!-- Before -->
<TextView android:text="Total Patients" ... />

<!-- After -->
<TextView android:text="@string/stat_total_bookings" ... />
```

### 8.2 `strings.xml`

```xml
<string name="stat_total_bookings">Total Bookings</string>
```

### 8.3 `CounselorDashboardActivity.java`

The computation (`masterAppointments.size()`) is correct for "Total Bookings" — no code change needed for the value, only the label is misleading.

---

## 9. Implementation Details — Dashboard UI Overhaul

### 9.1 Design Principles (Matching Student Side)

| Element | Student Side Reference | Counselor Side Target |
|---|---|---|
| Background | `#FFFFFF` or very light pink (`#FFF5F7`) | Same |
| Primary accent | Pastel pink (`#F8D7E3`), deep rose (`#C96B8E`) | Same |
| Card backgrounds | White with subtle pink border or shadow | Same |
| Card corners | 20dp radius | 20dp radius (currently 16dp — increase) |
| Buttons | Rounded MaterialButton with pink/rose fill or outline | Same |
| Icons | Minimalist line icons, tinted pink/grey | Replace current heavier icons |
| Typography | Clean sans-serif, consistent sizing | Match weights and sizes |
| Status badges | Rounded pill, pastel bg + bold text | Already close — keep pattern |

### 9.2 `activity_counselor_dashboard.xml` — Key Visual Changes

1. **Top bar**: Light pink background (`#FFF5F7`), rounded bottom corners. Logout icon → minimalist version matching student side.
2. **Stats cards**: Increase corner radius to 20dp. Backgrounds shift to white with thin pink border (`#F8D7E3` stroke, 1dp). Stat numbers in deep rose (`#C96B8E`). Labels in muted grey.
3. **Waitlist count card**: Round corners 20dp, pastel pink bg `#FFF0F5`, rose text for count.
4. **"Add Availability Slot" banner**: Keep purple accent but round to 20dp, add subtle shadow.
5. **TabLayout**: Pink indicator instead of blue. Tab text in grey (unselected) / rose (selected).
6. **Buttons** ("Availability Settings", "Export to Calendar"): Pink outline, rounded 12dp, consistent sizing.

### 9.3 `item_appointment.xml` — Card Redesign

1. **Card**: Corner radius 20dp (up from 16dp). Subtle elevation (2dp). White background.
2. **Avatar circle**: Keep pink bg but add slight border for polish.
3. **Action buttons**: Pastel themed:
   - Profile → light grey bg, dark text, rounded
   - Notes → light pink bg (`#FFF0F5`), rose text, rounded
   - No-Show → cream bg unchanged (already fine)
   - Crisis → pink-red bg unchanged (already fine)
4. **Remove Join button** entirely (see Section 6).
5. **Status badge pill**: Keep existing color coding but ensure 12dp rounded corners.

---

## 10. Implementation Details — Notes Dialog Styled

### 10.1 `dialog_session_note_styled.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:background="@drawable/bg_rounded_card_white"
    android:padding="24dp">

    <!-- Pink header -->
    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/session_notes_title"
        android:textSize="20sp"
        android:textStyle="bold"
        android:textColor="#C96B8E"
        android:layout_marginBottom="16dp" />

    <!-- Template chips (horizontal scroll) -->
    <HorizontalScrollView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginBottom="12dp">

        <com.google.android.material.chip.ChipGroup
            android:id="@+id/chipGroupNoteTemplates"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            app:singleSelection="true" />
    </HorizontalScrollView>

    <!-- Note text area -->
    <com.google.android.material.textfield.TextInputLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="@string/note_text_hint"
        app:boxCornerRadiusTopStart="12dp"
        app:boxCornerRadiusTopEnd="12dp"
        app:boxCornerRadiusBottomStart="12dp"
        app:boxCornerRadiusBottomEnd="12dp"
        app:boxStrokeColor="#F8D7E3"
        app:hintTextColor="#C96B8E">

        <com.google.android.material.textfield.TextInputEditText
            android:id="@+id/editSessionNote"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:minLines="5"
            android:gravity="top"
            android:inputType="textMultiLine" />
    </com.google.android.material.textfield.TextInputLayout>
</LinearLayout>
```

The `AlertDialog.Builder` uses a custom style (`R.style.PinkDialogTheme`) that sets:
- Dialog background: white rounded
- Button text color: `#C96B8E`
- Title text color: `#C96B8E`

---

## 11. Waitlist Sprint 8 Spillover

If any of the following from Sprint 8 remain incomplete, they are resolved in this sprint:

| Item | Check |
|---|---|
| `StudentHomeActivity` waitlist card tap → `StudentWaitlistActivity` navigation | Verify wired |
| `CounselorDashboardActivity` waitlist card tap → `CounselorWaitlistActivity` navigation | Verify wired |
| Guard check in `WaitlistRequestActivity` blocking submission when slots exist | Verify functional |
| `AvailabilitySetupActivity` auto-resolution hook firing after slot creation | Verify functional |
| All Sprint 8 AndroidManifest registrations in place | Verify |
| All Sprint 8 strings in `strings.xml` | Verify |
| Sprint 8 unit and UI tests passing | Verify |

---

## 12. Strings to Add/Update

```xml
<!-- Notes dialog -->
<string name="session_notes_title">Session Notes</string>
<string name="note_text_hint">Write your session notes...</string>
<string name="note_updated">Note updated.</string>
<string name="note_saved">Note saved.</string>
<string name="note_save_error">Could not save note.</string>
<string name="note_load_error">Could not load existing note.</string>
<string name="note_empty_error">Note cannot be empty.</string>
<string name="save_note">Save Note</string>

<!-- Logout -->
<string name="logout_confirm_title">Logout</string>
<string name="logout_confirm_message">Are you sure you want to logout?</string>
<string name="logout_confirm_yes">Yes, logout</string>

<!-- Stats -->
<string name="stat_total_bookings">Total Bookings</string>
<string name="stat_today_sessions">Today\'s Sessions</string>
<string name="stat_this_week">This Week</string>
```

---

## 13. Testing Requirements

### 13.1 Unit Tests

#### Updates to existing `SessionNoteTest.java`

| Test | Assertion |
|---|---|
| `updateNote_changesTextAndTimestamp` | Updated text and updatedAt differ from original |

#### Updates to existing `AppointmentTest.java`

| Test | Assertion |
|---|---|
| `noShowNotAllowedOnCancelled` | Verify through adapter logic or model guard |

### 13.2 UI Tests

#### New: `NotesUpsertFlowTest.java`

| Test | Assertion |
|---|---|
| `notesDialog_opensForAppointment` | Dialog inflates and is visible |
| `notesDialog_prefillsExistingNote` | If note exists in DB, EditText pre-populated |
| `notesDialog_savesNewNote` | Empty appointment → save creates new doc |
| `notesDialog_updatesExistingNote` | Filled appointment → save updates, no duplicate |

#### New: `NoShowGuardTest.java`

| Test | Assertion |
|---|---|
| `noShowButton_hiddenForCancelled` | CANCELLED appointment → No-Show button GONE |
| `noShowButton_hiddenForNoShow` | NO_SHOW appointment → No-Show button GONE |
| `noShowButton_hiddenForCompleted` | COMPLETED appointment → No-Show button GONE |
| `noShowButton_visibleForConfirmed` | CONFIRMED appointment → No-Show button VISIBLE |

#### New: `CounselorLogoutFlowTest.java`

| Test | Assertion |
|---|---|
| `logoutButton_showsConfirmation` | Tap logout → AlertDialog appears |
| `logoutConfirm_navigatesToLogin` | Tap "Yes" → LoginActivity opens |
| `logoutCancel_staysOnDashboard` | Tap "Cancel" → dialog dismissed, stays on dashboard |

---

## 14. Definition of Done

Sprint 9 is done only when:

- [ ] Notes dialog checks for existing note before opening — pre-fills if found.
- [ ] Saving a note for an appointment that already has one **updates** the existing document (no duplicate).
- [ ] `SessionNoteRepository` has `getNoteForAppointment()` and `updateNote()` methods.
- [ ] No-Show button is hidden for CANCELLED, NO_SHOW, and COMPLETED appointments.
- [ ] `markNoShow()` has a programmatic guard rejecting non-CONFIRMED statuses.
- [ ] Join button is removed from `item_appointment.xml` and `AppointmentAdapter`.
- [ ] Notes dialog uses the styled layout matching student-side pastel pink/white design.
- [ ] Counselor logout shows a confirmation popup before signing out.
- [ ] Logout icon matches the student-side minimalist icon.
- [ ] "Total Patients" stat renamed to "Total Bookings" (label only — computation unchanged).
- [ ] `activity_counselor_dashboard.xml` has 20dp rounded cards, pastel palette, minimalist icons.
- [ ] `item_appointment.xml` is redesigned with 20dp corners, pink accents, no Join button.
- [ ] TabLayout uses pink indicator instead of blue.
- [ ] All Sprint 8 waitlist spillover items verified functional.
- [ ] All new strings in `strings.xml`.
- [ ] Unit and UI tests pass.
- [ ] The app builds and the full student + counselor flow still works end-to-end.

---

## 15. Deliverables Checklist

| Deliverable | Type | Status |
|---|---|---|
| `SessionNoteRepository.java` — `getNoteForAppointment()` + `updateNote()` | Repository | Todo |
| `AppointmentAdapter.java` — notes upsert logic | Adapter | Todo |
| `AppointmentAdapter.java` — No-Show button guard | Adapter | Todo |
| `AppointmentAdapter.java` — Join button removal | Adapter | Todo |
| `CounselorDashboardActivity.java` — logout confirmation | Activity | Todo |
| `CounselorDashboardActivity.java` — "Total Bookings" rename | Activity | Todo |
| `activity_counselor_dashboard.xml` — visual overhaul | Layout | Todo |
| `item_appointment.xml` — redesign (rounded, pink, no Join) | Layout | Todo |
| `dialog_session_note_styled.xml` — new styled layout | Layout | Todo |
| `bg_rounded_card_pink.xml` | Drawable | Todo |
| `bg_rounded_card_white.xml` | Drawable | Todo |
| `bg_button_pastel_pink.xml` | Drawable | Todo |
| `ic_logout_minimal.xml` | Drawable | Todo |
| `strings.xml` updates | Resources | Todo |
| Sprint 8 waitlist spillover verification | Integration | Todo |
| `NotesUpsertFlowTest.java` | UI Test | Todo |
| `NoShowGuardTest.java` | UI Test | Todo |
| `CounselorLogoutFlowTest.java` | UI Test | Todo |
