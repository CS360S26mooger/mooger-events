# Moogers Council - Campus Counseling Appointment System

An Android application for LUMS students to book counseling appointments discreetly and efficiently.

## Core Features
- **Student Authentication:** Secure Login and Registration via Firebase Auth.
- **Counselor List:** Real-time list of available counselors fetched from Firestore.
- **Booking System:** Atomic transactions to book time slots and create appointments.
- **Counselor Dashboard:** Dedicated view for counselors to manage their sessions.
- **Crisis Support:** Quick access to emergency helplines.

## Project Structure (Agile/TDD)
- **OOD:** Clean separation of View (XML/Activities), Controller (Activity Logic), and Entity (POJOs).
- **TDD:** Unit tests for business logic (`TimeSlot`, `Appointment`).
- **NFRs:** Discreet mode and Slide-to-cancel (Work in Progress).

## Setup Instructions
1.  **Firebase:** Connect your Android project to Firebase.
2.  **Firestore:** Follow the [FIRESTORE_SETUP.md](FIRESTORE_SETUP.md) guide to seed initial data.
3.  **Authentication:** Enable Email/Password authentication in the Firebase Console.

## Development Status (Sprint 1)
- [x] Firebase Connectivity
- [x] Model Classes
- [x] Login & Register (Firebase Auth)
- [x] Counselor List & Booking Flow
- [x] Counselor Dashboard
- [x] Unit Tests (TimeSlot)
- [ ] UI Tests (Espresso)
- [ ] Discreet Mode & Slide-to-Cancel
