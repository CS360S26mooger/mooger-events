# Slot Booking — Changes Summary

## Root Cause

Counselor documents in Firestore were manually created in the Firebase console, giving them
auto-generated Firestore document IDs that do not match the counselor's Firebase Auth UID.
Slots were written with the Auth UID but queried using the Firestore document ID — always
returning 0 results on the student side.

---

## Firestore Structure Change

**Before:** flat `slots/` collection, all counselors mixed together, queried via `whereEqualTo("counselorId", uid)`.

**After:** hierarchical `Slots/{counselorAuthUID}/slots/{slotId}` — each counselor's slots live
under their own Auth UID branch. Direct path lookup, no index required, no scan.

---

## Code Changes

### `AvailabilityRepository.java`
Replaced flat `slots/` collection reference stored as a field with a private helper:
```java
private CollectionReference slotsFor(String counselorId) {
    return db.collection("Slots").document(counselorId).collection("slots");
}
```
All reads and writes now go through this helper.
`removeSlot(slotId, cb)` → `removeSlot(counselorId, slotId, cb)` — counselorId needed to build the path.

---

### `AppointmentRepository.java`
`bookAppointment`: slot document reference changed from:
```java
db.collection("slots").document(slot.getId())
```
to:
```java
db.collection("Slots").document(counselorId).collection("slots").document(slot.getId())
```
`cancelAppointment(appointmentId, slotId, cb)` → `cancelAppointment(appointmentId, counselorId, slotId, cb)`
so the slot's availability can be restored at the correct hierarchical path after cancellation.

---

### `AvailabilitySetupActivity.java`
`removeSlot` call updated to pass `counselorId`:
```java
// before
availabilityRepository.removeSlot(slot.getId(), callback)
// after
availabilityRepository.removeSlot(counselorId, slot.getId(), callback)
```

---

### `StudentHomeActivity.java`
`cancelAppointment` call updated to pass `counselorId` (read from the appointment object):
```java
// before
appointmentRepository.cancelAppointment(id, slotId, callback)
// after
appointmentRepository.cancelAppointment(id, appointment.getCounselorId(), slotId, callback)
```

---

### `CounselorDashboardActivity.java`
Two fixes:

1. **Broken fallback removed.** The old code picked the first counselor whose name was non-null
   and whose doc ID differed from the Auth UID — effectively a random counselor. Replaced with:
   - Primary: find counselor where `counselor.getUid() == authUID`
   - Secondary: find counselor where `counselor.getId() == authUID` (old console pattern where doc ID happened to equal Auth UID)
   - Last resort: use Auth UID directly as the document ID

2. **UID stamping on login.** After the correct counselor document is found, if its `uid` field
   does not already equal the Auth UID, `CounselorRepository.stampAuthUid()` is called to write it.
   This is a one-time self-healing step — once stamped, subsequent logins skip the write.

---

### `CounselorProfileEditActivity.java`
Profile save was setting `uid = Firestore document ID` instead of the Auth UID:
```java
// before
updatedCounselor.setUid(counselorId);   // counselorId was the Firestore doc ID
// after
String authUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
updatedCounselor.setUid(authUid);
```

---

### `CounselorProfileActivity.java`
The referral counselor launch (on-leave redirect) was not passing `SLOT_COUNSELOR_ID`, causing
the booking screen to fall back to the Firestore doc ID. Fixed to look up the referral's Auth UID
from the session cache before launching:
```java
Counselor referral = SessionCache.getInstance().getSingleCounselor(referralId);
String referralSlotId = (referral != null && referral.getUid() != null)
        ? referral.getUid() : referralId;
intent.putExtra("SLOT_COUNSELOR_ID", referralSlotId);
```

---

### `CounselorRepository.java`
Three changes:

1. **`createCounselorProfile(authUid, name, cb)` added** — creates `counselors/{authUID}` with
   `set()` so the Firestore document ID always equals the Auth UID. Called by `RegisterActivity`
   on counselor signup.

2. **`stampAuthUid(counselorDocId, authUid, cb)` added** — writes `uid = authUid` onto an
   existing counselor document. Called by `CounselorDashboardActivity` on login as a one-time
   self-healing write for manually-provisioned counselors.

3. **`updateCounselorProfile` switched from `set()` to `update()`** — the old `set()` was
   overwriting the entire document on every profile save, wiping the `name` field because
   the profile edit screen does not expose a name input. `update()` with an explicit map of
   only the editable fields (bio, language, gender, specializations, onLeave, etc.) fixes this.

---

### `RegisterActivity.java`
**This is where the signup flow was changed.**

Before this change, registering as a counselor through the app created only a `users/{uid}`
document (for role lookup) but no document in the `counselors/` collection. The counselor could
log in but had no profile, and slot queries broke immediately because there was no
`counselors/{uid}` document for the student side to resolve the Auth UID from.

After the change, a counselor registration does both writes in sequence:

```
1. Firebase Auth creates account → returns authUID
2. users/{authUID}         ← written by userRepository.createUser()  (unchanged, for role lookup)
3. counselors/{authUID}    ← written by counselorRepository.createCounselorProfile()  (NEW)
     uid: authUID
     name: <from registration form>
     bio: ""
     language: ""
     gender: ""
     onLeave: false
     specializations: []
```

Because the document ID is set to the Auth UID explicitly (not auto-generated by Firestore),
the slot query chain works for every new counselor without any Firebase console intervention.

Student registrations are completely unchanged — they still only write `users/{uid}`.

The navigation after registration was also extracted into a `navigateNext()` helper to remove
the duplicated `startActivity` + `finish()` calls that existed in both the success and failure
branches.

---

### `UserRepository.java`
`getCurrentUserRole` was hardened to handle the doc-ID-vs-Auth-UID mismatch for existing
manually-created counselors:

1. First checks `counselors/{authUID}` directly by document ID.
2. If not found, queries `counselors` where `uid == authUID` (catches docs that were stamped
   but have a different doc ID).
3. Falls back to `users/{uid}` for students and admins.

`stampUidOnRealCounselorDoc` (private helper) also added — called after step 1 succeeds to
ensure the `uid` field on the real counselor document is always up to date.

---

### `BookingActivity.java`
Removed a debug Toast that was firing on every booking screen open, showing the counselor ID
and slot count to the user.

---

## One-Time Manual Firebase Step (Existing Test Counselor Only)

In the Firebase console, open `counselors/HWhCN1a4hhlOkjtJhIhP` and add:
```
uid: "s9nktaZ8mafd7kY2s3MSDXt3TPl2"
```
Then re-add slots through the app's availability screen (the old flat `slots/` collection is
no longer read by the app). All new counselors who sign up through the app are handled
automatically — no console steps needed.
