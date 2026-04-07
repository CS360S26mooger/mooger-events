# Firestore Setup Guide

To make the Moogers Council app functional, you need to set up the following collections and sample data in your Firebase Console.

## 1. `users` Collection
Stores profile information for both Students and Counselors.
- **Document ID:** Use the Firebase Auth UID (auto-generated when you register).
- **Fields:**
  - `uid`: string
  - `name`: string
  - `email`: string
  - `role`: string ("Student" or "Counselor")

## 2. `counselors` Collection
Stores the list of available counselors.
- **Document ID:** Auto-ID
- **Fields:**
  - `name`: string (e.g., "Dr. Ali Khan")
  - `specializations`: array of strings (e.g., ["Anxiety", "Academic Stress"])
  - `bio`: string
  - `gender`: string
  - `language`: string
  - `isOnLeave`: boolean (false)

## 3. `slots` Collection
Stores individual time slots for counseling.
- **Document ID:** Auto-ID
- **Fields:**
  - `counselorId`: string (Copy the Document ID from a counselor in the `counselors` collection)
  - `date`: string (e.g., "2026-04-10")
  - `time`: string (e.g., "10:00 AM - 11:00 AM")
  - `available`: boolean (true)

## 4. `appointments` Collection
This collection will be populated automatically by the app when a student books a slot.
- **Fields (Auto-created):**
  - `id`: string
  - `studentId`: string
  - `counselorId`: string
  - `slotId`: string
  - `date`: string
  - `time`: string
  - `status`: string ("CONFIRMED")

---

### Pro-Tip for Testing:
Add at least **2 counselors** and **3 slots** for each counselor to verify the RecyclerView and filtering logic works correctly.
