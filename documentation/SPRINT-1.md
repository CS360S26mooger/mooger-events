# Sprint 1 — US-22: Student Registration and Authentication
### Detailed Implementation Guide

---

## 1. Sprint Objective

By the end of this sprint, the app supports a complete auth cycle:

1. A new user registers with a university email, password, preferred name, and pronouns.
2. A `users/{uid}` document is created in Firestore.
3. The user reads and accepts a privacy policy.
4. Based on the user's `role` field in Firestore, they are routed to `StudentHomeActivity` or `CounselorDashboardActivity`.
5. Returning users with a live Firebase Auth session skip login entirely.

---

## 2. What Already Exists

| File | State | What it does today |
|---|---|---|
| `LoginActivity.java` | Functional | Role selector (Student/Counselor/Admin) with animated button transitions. Email + password fields. Firebase Auth `signInWithEmailAndPassword`. Redirects to `HomeActivity` on success. Auto-redirects if session exists. |
| `RegisterActivity.java` | Empty stub | Just EdgeToEdge boilerplate. No form, no logic. |
| `HomeActivity.java` | Empty stub | Just EdgeToEdge boilerplate. No content. |
| `activity_login.xml` | Complete UI | Full login form — brain icon, role buttons, email/password inputs, SSO button, sign-up link. |
| `activity_register.xml` | Empty | `ConstraintLayout` with no children. |
| `activity_home.xml` | Empty | `ConstraintLayout` with no children. |
| `Counselor.java` | Model exists | **Wrong package** (`com.mooger.moogerscouncil`). Must be moved to `com.example.moogerscouncil`. |
| `TimeSlot.java` | Model exists | Fully functional with `book()` method. |
| `Appointment.java` | Model exists | Fully functional. |
| `Student.java` | Does not exist | Must be created. |
| `UserRepository.java` | Does not exist | Must be created. |
| `strings.xml` | Minimal | Only has `app_name`. All UI strings for registration, privacy policy, and student home must be added. |

---

## 3. Files to Create or Modify

### 3.1 New Files

```
src/main/java/com/example/moogerscouncil/
├── Student.java                    // Model
├── UserRepository.java             // Repository
├── PrivacyPolicyActivity.java      // UI
├── StudentHomeActivity.java        // UI
├── UserRole.java                   // Constants

src/main/res/layout/
├── activity_register.xml           // Overwrite the empty stub
├── activity_privacy_policy.xml     // New
├── activity_student_home.xml       // New

src/test/java/com/example/moogerscouncil/
├── StudentTest.java                // Unit test

src/androidTest/java/com/example/moogerscouncil/
├── RegistrationFlowTest.java       // Intent test
├── LoginRoleRoutingTest.java       // Intent test
```

### 3.2 Files to Modify

```
LoginActivity.java          // Add Firestore role-check after auth success
RegisterActivity.java       // Replace stub with full registration logic
activity_home.xml           // No longer needed as a destination (replaced by StudentHomeActivity)
AndroidManifest.xml         // Register new activities
strings.xml                 // Add all user-facing strings
Counselor.java              // Fix package declaration
```

---

## 4. Implementation Details — Model Layer

### 4.1 `Student.java` — Model Class

**Purpose:** Firestore-mapped data container for the `users` collection (where `role == "student"`).

**Design decisions:**
- Empty no-arg constructor required by Firestore's `toObject()` deserialization.
- No business logic. Pure data holder — validation belongs in the Activity or Repository.
- Uses `com.google.firebase.Timestamp` for `createdAt` so Firestore serializes it natively as a server timestamp.

```java
package com.example.moogerscouncil;

import com.google.firebase.Timestamp;

/**
 * Represents a registered student user in the BetterCAPS system.
 * Maps directly to a document in the Firestore 'users' collection
 * where role == "student".
 *
 * This class follows the Firestore model convention: no-argument
 * constructor, private fields, public getters/setters.
 */
public class Student {

    private String uid;
    private String name;
    private String email;
    private String preferredName;
    private String pronouns;
    private String role;
    private Timestamp createdAt;

    /** Required empty constructor for Firestore deserialization. */
    public Student() {}

    /**
     * Constructs a Student with all required registration fields.
     *
     * @param uid           Firebase Auth UID, also the Firestore document ID.
     * @param name          Full legal name.
     * @param email         University email address (must end in @lums.edu.pk).
     * @param preferredName The name the student wants to be called.
     * @param pronouns      Pronoun preference (e.g. "he/him", "she/her", "they/them").
     */
    public Student(String uid, String name, String email,
                   String preferredName, String pronouns) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.preferredName = preferredName;
        this.pronouns = pronouns;
        this.role = UserRole.STUDENT;
        this.createdAt = Timestamp.now();
    }

    // --- Getters ---

    public String getUid() { return uid; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPreferredName() { return preferredName; }
    public String getPronouns() { return pronouns; }
    public String getRole() { return role; }
    public Timestamp getCreatedAt() { return createdAt; }

    // --- Setters (required by Firestore) ---

    public void setUid(String uid) { this.uid = uid; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setPreferredName(String preferredName) { this.preferredName = preferredName; }
    public void setPronouns(String pronouns) { this.pronouns = pronouns; }
    public void setRole(String role) { this.role = role; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
```

**Relationship to CRC card:** The Student CRC card lists responsibilities like "request and filter Appointment" and "complete IntakeAssessment" — those are handled by Activities and Repositories, not the model. The model holds only the data shape that maps to Firestore.

### 4.2 `UserRole.java` — Constants

```java
package com.example.moogerscouncil;

/**
 * String constants for user roles stored in Firestore.
 * Used for role-based routing after login and for the
 * 'role' field in the users collection.
 */
public final class UserRole {
    public static final String STUDENT = "student";
    public static final String COUNSELOR = "counselor";
    public static final String ADMIN = "admin";

    private UserRole() {}  // prevent instantiation
}
```

**Why a constants class instead of an enum?** Firestore stores roles as plain strings. Using constants avoids the need to convert between enums and strings on every read/write, and matches the convention used by `AppointmentStatus` and `SpecializationTags` elsewhere in the codebase.

### 4.3 Fix `Counselor.java` — Package Correction

The file lives at `com/example/moogerscouncil/Counselor.java` but declares `package com.mooger.moogerscouncil`. This will cause a build failure once any other class tries to import it.

**Fix:** Change line 1:

```java
// FROM:
package com.mooger.moogerscouncil;

// TO:
package com.example.moogerscouncil;
```

No other changes needed. All fields and methods stay the same.

---

## 5. Implementation Details — Repository Layer

### 5.1 `UserRepository.java`

**Purpose:** Single point of access for all Firestore operations on the `users` collection. Activities never touch `FirebaseFirestore` directly.

**Design pattern:** Repository pattern. The class holds a `CollectionReference` to `users` and exposes methods that accept callback interfaces for async results.

```java
package com.example.moogerscouncil;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Repository for the Firestore 'users' collection.
 * All reads and writes to user documents flow through this class.
 * Activities depend on this class, never on FirebaseFirestore directly.
 *
 * Follows the Repository design pattern to isolate the data layer.
 */
public class UserRepository {

    private final CollectionReference usersCollection;
    private final FirebaseAuth auth;

    public UserRepository() {
        this.usersCollection = FirebaseFirestore.getInstance().collection("users");
        this.auth = FirebaseAuth.getInstance();
    }

    // --- Callback interfaces ---

    public interface OnUserCreatedCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface OnUserFetchedCallback {
        void onSuccess(Student student);
        void onFailure(Exception e);
    }

    public interface OnRoleFetchedCallback {
        void onSuccess(String role);
        void onFailure(Exception e);
    }

    // --- Write operations ---

    /**
     * Creates a new user document in Firestore.
     * Uses the Student's UID as the document ID so it matches
     * the Firebase Auth UID for easy lookup.
     *
     * @param student  The Student object to persist.
     * @param callback Success/failure callback.
     */
    public void createUser(Student student, OnUserCreatedCallback callback) {
        usersCollection.document(student.getUid())
            .set(student)
            .addOnSuccessListener(unused -> callback.onSuccess())
            .addOnFailureListener(callback::onFailure);
    }

    // --- Read operations ---

    /**
     * Fetches the Firestore document for the currently authenticated user
     * and deserializes it into a Student object.
     *
     * @param callback Receives the Student on success, or an Exception on failure.
     */
    public void getCurrentUser(OnUserFetchedCallback callback) {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) {
            callback.onFailure(new IllegalStateException("No authenticated user"));
            return;
        }
        usersCollection.document(firebaseUser.getUid())
            .get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    Student student = doc.toObject(Student.class);
                    callback.onSuccess(student);
                } else {
                    callback.onFailure(new IllegalStateException("User document not found"));
                }
            })
            .addOnFailureListener(callback::onFailure);
    }

    /**
     * Fetches only the role field for the currently authenticated user.
     * Used for post-login routing to the correct home screen.
     *
     * @param callback Receives the role string ("student", "counselor", "admin")
     *                 on success, or an Exception on failure.
     */
    public void getCurrentUserRole(OnRoleFetchedCallback callback) {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) {
            callback.onFailure(new IllegalStateException("No authenticated user"));
            return;
        }
        usersCollection.document(firebaseUser.getUid())
            .get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    String role = doc.getString("role");
                    callback.onSuccess(role);
                } else {
                    callback.onFailure(new IllegalStateException("User document not found"));
                }
            })
            .addOnFailureListener(callback::onFailure);
    }
}
```

**How Activities use this:**

```java
// In LoginActivity, after Firebase Auth succeeds:
UserRepository repo = new UserRepository();
repo.getCurrentUserRole(new UserRepository.OnRoleFetchedCallback() {
    @Override
    public void onSuccess(String role) {
        if (UserRole.COUNSELOR.equals(role)) {
            startActivity(new Intent(LoginActivity.this, CounselorDashboardActivity.class));
        } else {
            startActivity(new Intent(LoginActivity.this, StudentHomeActivity.class));
        }
        finish();
    }

    @Override
    public void onFailure(Exception e) {
        Toast.makeText(LoginActivity.this,
            getString(R.string.error_fetching_role), Toast.LENGTH_LONG).show();
    }
});
```

---

## 6. Implementation Details — UI Layer

### 6.1 `RegisterActivity.java` — Full Registration Flow

**Current state:** Empty stub.

**What it needs to do:**

```
User taps "Sign Up" on LoginActivity
        │
        ▼
RegisterActivity opens
  ├── Form fields: name, email, preferred name, pronouns, password, confirm password
  ├── Email domain validation (@lums.edu.pk)
  ├── Password strength check (min 6 chars, Firebase requirement)
  ├── Password match check
  │
  ▼ User taps "Create Account"
  │
  ├── 1. FirebaseAuth.createUserWithEmailAndPassword(email, password)
  │       └── onFailure → show error Toast (e.g. "Email already in use")
  │
  ├── 2. On auth success, build a Student object:
  │       Student s = new Student(auth.getUid(), name, email, preferredName, pronouns);
  │
  ├── 3. UserRepository.createUser(s, callback)
  │       └── onFailure → show error Toast, but account is created.
  │           User can still proceed (Firestore doc can be retried later).
  │
  └── 4. On Firestore success → navigate to PrivacyPolicyActivity
                                  └── finish() RegisterActivity so back button
                                      doesn't return here.
```

**Key wiring:**
- `RegisterActivity` creates a `FirebaseAuth` instance and a `UserRepository` instance.
- The auth call and the Firestore write are **sequential, not parallel** — the Firestore write needs the UID from the auth result.
- The selected role from LoginActivity is passed via Intent extra. If no role is passed, default to `UserRole.STUDENT`.

**Layout — `activity_register.xml`:**

The layout mirrors the login screen's design language (same card style, colors, spacing) for visual consistency:

```
ScrollView
└── LinearLayout (vertical, padding 24dp)
    ├── Back arrow (ImageButton, navigates back to LoginActivity)
    ├── Title: "Create Account"
    ├── Subtitle: "Join BetterCAPS to access counseling services"
    │
    ├── CardView (white, rounded 16dp, elevation 2dp)
    │   └── LinearLayout (vertical, padding 24dp)
    │       ├── TextInputLayout: "Full Name" (editTextName)
    │       ├── TextInputLayout: "University Email" (editTextEmail)
    │       │     └── helperText: "Must be a @lums.edu.pk address"
    │       ├── TextInputLayout: "Preferred Name" (editTextPreferredName)
    │       │     └── helperText: "What should we call you?"
    │       ├── TextInputLayout: "Pronouns" (editTextPronouns)
    │       │     └── helperText: "e.g. he/him, she/her, they/them"
    │       ├── TextInputLayout: "Password" (editTextPassword)
    │       │     └── passwordToggle enabled
    │       ├── TextInputLayout: "Confirm Password" (editTextConfirmPassword)
    │       │     └── passwordToggle enabled
    │       └── MaterialButton: "Create Account" (buttonRegister)
    │             └── full width, primary_blue background, 56dp height
    │
    └── TextView: "Already have an account? Log in" (linkLogin)
          └── clickable, navigates back to LoginActivity
```

**Validation logic (runs on "Create Account" tap, before any Firebase call):**

```java
private boolean validateInputs() {
    boolean valid = true;

    String name = editTextName.getText().toString().trim();
    String email = editTextEmail.getText().toString().trim();
    String password = editTextPassword.getText().toString();
    String confirmPassword = editTextConfirmPassword.getText().toString();

    if (name.isEmpty()) {
        editTextName.setError(getString(R.string.error_name_required));
        valid = false;
    }

    if (email.isEmpty()) {
        editTextEmail.setError(getString(R.string.error_email_required));
        valid = false;
    } else if (!email.endsWith("@lums.edu.pk")) {
        editTextEmail.setError(getString(R.string.error_email_domain));
        valid = false;
    }

    if (password.length() < 6) {
        editTextPassword.setError(getString(R.string.error_password_length));
        valid = false;
    }

    if (!password.equals(confirmPassword)) {
        editTextConfirmPassword.setError(getString(R.string.error_password_mismatch));
        valid = false;
    }

    return valid;
}
```

### 6.2 `PrivacyPolicyActivity.java` — Privacy Gate

**Purpose:** A mandatory screen shown once after registration. The user must tap "I Agree" to proceed. This is a regulatory/ethical requirement for a counseling platform.

**Flow:**
```
PrivacyPolicyActivity opens
  ├── ScrollView with privacy policy text (loaded from strings.xml)
  ├── "I Agree" button at the bottom (initially visible, always active)
  │
  └── On "I Agree" tap:
      ├── Navigate to StudentHomeActivity (or CounselorDashboardActivity based on role)
      └── finish() so back button doesn't return here
```

**Layout — `activity_privacy_policy.xml`:**
```
ConstraintLayout
├── Toolbar: "Privacy Policy" with back arrow
├── NestedScrollView (fills remaining space above button)
│   └── TextView: @string/privacy_policy_text
│       └── Long-form text, styled with textSize 14sp, lineSpacingExtra 6dp
└── MaterialButton: "I Agree and Continue" (pinned to bottom, 56dp, full width)
```

**Why no checkbox + button pattern?** Simpler is better for a half-way prototype. The user reads and taps "I Agree." A checkbox adds complexity with no functional benefit at this stage.

**Implementation notes:**
- The privacy policy text goes in `strings.xml` as `@string/privacy_policy_text`. This is a long string — use `\n\n` for paragraph breaks and CDATA if needed.
- The activity receives the user's role via Intent extra to route to the correct home screen.

### 6.3 `LoginActivity.java` — Modifications

The existing `LoginActivity` does Firebase Auth but then blindly navigates to `HomeActivity`. It needs two changes:

**Change 1: Post-login role-based routing**

After `signInWithEmailAndPassword` succeeds, instead of going to `HomeActivity`, query Firestore for the user's role and route accordingly.

```
Current flow:
  auth.signIn → success → startActivity(HomeActivity)

New flow:
  auth.signIn → success → UserRepository.getCurrentUserRole()
                              ├── "student"   → StudentHomeActivity
                              ├── "counselor"  → CounselorDashboardActivity
                              └── failure      → Toast error, stay on login
```

**Where this happens in the code:** Inside the `addOnSuccessListener` of `signInWithEmailAndPassword`. Replace the direct `HomeActivity` intent with the `UserRepository` role-fetch call shown in Section 5.1.

**Change 2: Auto-redirect role check**

The existing `onCreate` checks `if (auth.getCurrentUser() != null)` and goes to `HomeActivity`. This needs the same role-based routing:

```java
// In onCreate, replace:
if (auth.getCurrentUser() != null) {
    startActivity(new Intent(this, HomeActivity.class));
    finish();
}

// With:
if (auth.getCurrentUser() != null) {
    routeToHome();  // extracted method that calls UserRepository.getCurrentUserRole
    return;         // don't inflate login UI
}
```

**Change 3: Pass selected role to RegisterActivity**

When the user taps "Sign Up", the currently selected role should be passed as an Intent extra so the registration screen knows what role to assign:

```java
Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
intent.putExtra("selected_role", selectedRole);  // "student", "counselor", or "admin"
startActivity(intent);
```

### 6.4 `StudentHomeActivity.java` — Student Landing Page

**Purpose:** The main screen students see after login. For Sprint 1, this is a minimal but functional home page. It will be enriched in later sprints (counselor directory button in Sprint 2, emergency FAB in Sprint 4).

**Layout — `activity_student_home.xml`:**
```
ConstraintLayout (background: @color/background)
├── LinearLayout (top section, vertical, padding 24dp)
│   ├── TextView: "Welcome back," (textSize 16sp, text_gray)
│   ├── TextView: "{preferredName}" (textSize 24sp, bold, text_dark)
│   │     └── Populated from UserRepository.getCurrentUser() on load
│   └── TextView: "BetterCAPS" (textSize 14sp, text_gray, below name)
│
├── CardView (white, centered, padding 32dp)
│   ├── ImageView: brain/counseling icon
│   ├── TextView: "Your counseling hub"
│   └── TextView: "Features coming soon: browse counselors, book appointments"
│
└── MaterialButton: "Log Out" (outlined, bottom of screen)
      └── FirebaseAuth.signOut() → navigate to LoginActivity, finish()
```

**Runtime behavior:**
- On `onCreate`, call `UserRepository.getCurrentUser()` to fetch the student's preferred name.
- Display the preferred name in the welcome header.
- Show a logout button that calls `FirebaseAuth.getInstance().signOut()` and navigates back to `LoginActivity`.
- This screen is the insertion point for Sprint 2's "Find a Counselor" button and Sprint 4's emergency FAB.

### 6.5 `AndroidManifest.xml` — Updates

Add the two new activities:

```xml
<activity
    android:name=".PrivacyPolicyActivity"
    android:exported="false" />
<activity
    android:name=".StudentHomeActivity"
    android:exported="false" />
```

`HomeActivity` stays registered (it's used as a generic fallback during development), but the launcher remains `LoginActivity`.

---

## 7. Implementation Details — Strings

All user-facing text must be in `strings.xml`. Here is the complete set needed for Sprint 1:

```xml
<!-- Registration Screen -->
<string name="title_register">Create Account</string>
<string name="subtitle_register">Join BetterCAPS to access counseling services</string>
<string name="hint_full_name">Full Name</string>
<string name="hint_email">University Email</string>
<string name="helper_email">Must be a @lums.edu.pk address</string>
<string name="hint_preferred_name">Preferred Name</string>
<string name="helper_preferred_name">What should we call you?</string>
<string name="hint_pronouns">Pronouns</string>
<string name="helper_pronouns">e.g. he/him, she/her, they/them</string>
<string name="hint_password">Password</string>
<string name="hint_confirm_password">Confirm Password</string>
<string name="button_register">Create Account</string>
<string name="link_login">Already have an account? Log in</string>

<!-- Validation Errors -->
<string name="error_name_required">Name is required</string>
<string name="error_email_required">Email is required</string>
<string name="error_email_domain">Must be a @lums.edu.pk email address</string>
<string name="error_password_length">Password must be at least 6 characters</string>
<string name="error_password_mismatch">Passwords do not match</string>
<string name="error_registration_failed">Registration failed. Please try again.</string>
<string name="error_firestore_write">Account created but profile save failed. Please log in again.</string>
<string name="error_fetching_role">Unable to determine account type. Please try again.</string>
<string name="error_login_failed">Login failed. Please check your credentials.</string>

<!-- Privacy Policy -->
<string name="title_privacy_policy">Privacy Policy</string>
<string name="button_agree">I Agree and Continue</string>
<string name="privacy_policy_text">BetterCAPS Privacy Policy\n\nYour privacy is important to us. This policy explains how we collect, use, and protect your personal information when you use the BetterCAPS counseling platform.\n\n1. Information We Collect\nWe collect your university email address, name, preferred name, and pronouns when you register. Appointment records are stored to provide you with counseling services.\n\n2. How We Use Your Information\nYour information is used solely to connect you with university counseling services. We do not sell or share your data with third parties.\n\n3. Anonymity of Feedback\nPost-session feedback is fully anonymous. Your identity is never linked to feedback submissions.\n\n4. Data Security\nAll data is stored in secure cloud infrastructure with encryption at rest and in transit.\n\n5. Your Rights\nYou may request deletion of your account and associated data at any time by contacting campus counseling services.\n\nBy tapping "I Agree and Continue", you acknowledge that you have read and understood this policy.</string>

<!-- Student Home -->
<string name="greeting_prefix">Welcome back,</string>
<string name="home_subtitle">BetterCAPS</string>
<string name="home_placeholder">Your counseling hub</string>
<string name="home_coming_soon">Features coming soon: browse counselors, book appointments</string>
<string name="button_logout">Log Out</string>
```

---

## 8. Data Flow Diagrams

### 8.1 Registration Flow (end-to-end)

```
User                    RegisterActivity              FirebaseAuth              UserRepository (Firestore)
 │                            │                            │                            │
 │  fills form, taps         │                            │                            │
 │  "Create Account"         │                            │                            │
 │ ─────────────────────────►│                            │                            │
 │                            │  validateInputs()          │                            │
 │                            │  (local, synchronous)      │                            │
 │                            │                            │                            │
 │                            │  createUserWithEmail       │                            │
 │                            │  AndPassword(email, pass)  │                            │
 │                            │ ──────────────────────────►│                            │
 │                            │                            │                            │
 │                            │    onSuccess(authResult)   │                            │
 │                            │ ◄──────────────────────────│                            │
 │                            │                            │                            │
 │                            │  uid = authResult.getUid() │                            │
 │                            │  student = new Student(    │                            │
 │                            │    uid, name, email,       │                            │
 │                            │    preferredName, pronouns)│                            │
 │                            │                            │                            │
 │                            │  createUser(student, cb)   │                            │
 │                            │ ───────────────────────────┼───────────────────────────►│
 │                            │                            │                            │
 │                            │                            │    .set(student) to         │
 │                            │                            │    users/{uid}              │
 │                            │                            │                            │
 │                            │         onSuccess()        │                            │
 │                            │ ◄──────────────────────────┼──────────────────────────── │
 │                            │                            │                            │
 │                            │  startActivity(            │                            │
 │                            │    PrivacyPolicyActivity)  │                            │
 │                            │  finish()                  │                            │
 │                            │                            │                            │
```

### 8.2 Login Flow (existing user, with new role routing)

```
User                    LoginActivity              FirebaseAuth              UserRepository (Firestore)
 │                            │                            │                            │
 │  enters email + pass,      │                            │                            │
 │  taps "Login"             │                            │                            │
 │ ─────────────────────────►│                            │                            │
 │                            │                            │                            │
 │                            │  signInWithEmail           │                            │
 │                            │  AndPassword(email, pass)  │                            │
 │                            │ ──────────────────────────►│                            │
 │                            │                            │                            │
 │                            │    onSuccess(authResult)   │                            │
 │                            │ ◄──────────────────────────│                            │
 │                            │                            │                            │
 │                            │  getCurrentUserRole(cb)    │                            │
 │                            │ ───────────────────────────┼───────────────────────────►│
 │                            │                            │    .get() users/{uid}       │
 │                            │                            │    .getString("role")       │
 │                            │                            │                            │
 │                            │    onSuccess("student")    │                            │
 │                            │ ◄──────────────────────────┼──────────────────────────── │
 │                            │                            │                            │
 │                            │  startActivity(            │                            │
 │   sees StudentHomeActivity │    StudentHomeActivity)    │                            │
 │ ◄──────────────────────────│  finish()                  │                            │
```

### 8.3 Auto-Redirect Flow (returning user, session alive)

```
User                    LoginActivity              FirebaseAuth              UserRepository
 │                            │                            │                            │
 │  opens app                │                            │                            │
 │ ─────────────────────────►│                            │                            │
 │                            │  auth.getCurrentUser()     │                            │
 │                            │ ──────────────────────────►│                            │
 │                            │  returns non-null          │                            │
 │                            │ ◄──────────────────────────│                            │
 │                            │                            │                            │
 │                            │  getCurrentUserRole(cb)    │                            │
 │                            │ ───────────────────────────┼───────────────────────────►│
 │                            │    onSuccess(role)         │                            │
 │                            │ ◄──────────────────────────┼──────────────────────────── │
 │                            │                            │                            │
 │   routed to correct home   │  route based on role       │                            │
 │ ◄──────────────────────────│  finish()                  │                            │
 │   (never sees login form)  │                            │                            │
```

---

## 9. How Elements Connect — Dependency Map

```
                    ┌──────────────┐
                    │  UserRole    │  (constants: STUDENT, COUNSELOR, ADMIN)
                    └──────┬───────┘
                           │ used by
              ┌────────────┼────────────┐
              │            │            │
              ▼            ▼            ▼
       ┌──────────┐  ┌───────────┐  ┌─────────────────┐
       │ Student  │  │ UserRepo  │  │ LoginActivity   │
       │ (model)  │  │           │  │                 │
       └────┬─────┘  └─────┬─────┘  └───────┬─────────┘
            │              │                │
            │  serialized  │                │ calls after auth
            │  to/from     │                │ success
            └──────────────┤                │
                           │◄───────────────┘
                           │
                    ┌──────┴───────┐
                    │  Firestore   │
                    │  users/{uid} │
                    └──────────────┘


  Navigation chain:
  ┌───────────┐      ┌────────────────┐      ┌──────────────────┐      ┌──────────────────┐
  │  Login    │─────►│  Register      │─────►│  PrivacyPolicy   │─────►│ StudentHome      │
  │  Activity │      │  Activity      │      │  Activity        │      │ Activity         │
  └─────┬─────┘      └────────────────┘      └──────────────────┘      └──────────────────┘
        │                                                                       ▲
        │  (returning user, role == "student")                                  │
        └───────────────────────────────────────────────────────────────────────┘
```

---

## 10. Testing Plan

### 10.1 Unit Tests — `StudentTest.java`

```java
@Test public void testDefaultRoleIsStudent()
  // new Student(...) sets role to "student"

@Test public void testConstructorSetsAllFields()
  // verify uid, name, email, preferredName, pronouns are set

@Test public void testEmptyConstructorForFirestore()
  // new Student() does not throw; all fields are null

@Test public void testSettersOverrideConstructorValues()
  // after setPreferredName("New"), getPreferredName() returns "New"

@Test public void testCreatedAtIsNotNull()
  // new Student(...).getCreatedAt() is non-null
```

### 10.2 Intent Tests — `RegistrationFlowTest.java`

Uses Espresso + Firebase Emulator (or a test Firebase project).

```java
@Test public void testRegistrationFormDisplaysAllFields()
  // launch RegisterActivity
  // assert editTextName, editTextEmail, editTextPreferredName,
  //        editTextPronouns, editTextPassword, editTextConfirmPassword are displayed

@Test public void testInvalidEmailShowsError()
  // type "user@gmail.com" into email field
  // tap register
  // assert error text contains "lums.edu.pk"

@Test public void testPasswordMismatchShowsError()
  // type "abc123" and "xyz789" into password fields
  // tap register
  // assert error on confirm password field

@Test public void testSuccessfulRegistrationNavigatesToPrivacyPolicy()
  // type valid inputs, tap register
  // assert PrivacyPolicyActivity is launched (use Intents.intended)
```

### 10.3 Intent Tests — `LoginRoleRoutingTest.java`

```java
@Test public void testStudentRoleRoutesToStudentHome()
  // pre-seed a Firestore user with role "student"
  // log in with that user's credentials
  // assert StudentHomeActivity is launched

@Test public void testCounselorRoleRoutesToDashboard()
  // pre-seed a Firestore user with role "counselor"
  // log in with that user's credentials
  // assert CounselorDashboardActivity is launched
  // (CounselorDashboardActivity doesn't exist yet in Sprint 1 —
  //  for now, assert the intent is fired with the correct class.
  //  The activity itself is built in Sprint 4.)
```

---

## 11. Implementation Order

Tasks within this sprint, in the order they should be built:

| # | Task | Depends On | Output |
|---|---|---|---|
| 1 | Fix `Counselor.java` package | None | Build compiles without package errors |
| 2 | Create `UserRole.java` | None | Constants available for all auth code |
| 3 | Create `Student.java` model | `UserRole` | Model ready for Firestore serialization |
| 4 | Write `StudentTest.java` | `Student` | Unit tests pass |
| 5 | Create `UserRepository.java` | `Student` | Repository ready for Activities to call |
| 6 | Add all Sprint 1 strings to `strings.xml` | None | String resources available for layouts |
| 7 | Build `activity_register.xml` layout | Strings | Registration form UI ready |
| 8 | Implement `RegisterActivity.java` | `UserRepository`, layout | Registration flow works end-to-end |
| 9 | Build `activity_privacy_policy.xml` layout | Strings | Privacy policy screen UI ready |
| 10 | Implement `PrivacyPolicyActivity.java` | Layout | Privacy gate works |
| 11 | Build `activity_student_home.xml` layout | Strings | Student home screen UI ready |
| 12 | Implement `StudentHomeActivity.java` | `UserRepository`, layout | Student home loads preferred name, logout works |
| 13 | Modify `LoginActivity.java` — add role routing | `UserRepository`, `UserRole` | Login routes to correct home screen |
| 14 | Update `AndroidManifest.xml` | New activities | All activities registered |
| 15 | Write `RegistrationFlowTest.java` | Steps 7–10 | Intent tests pass |
| 16 | Write `LoginRoleRoutingTest.java` | Step 13 | Intent tests pass |
| 17 | Javadoc pass on `Student.java` and `UserRepository.java` | Steps 3, 5 | Documentation complete |
| 18 | File header comments on all new/modified `.java` files | All | Code documentation deliverable met |

---

## 12. Edge Cases and Error Handling

| Scenario | Expected Behavior |
|---|---|
| User enters non-university email (e.g. `@gmail.com`) | Validation error shown inline on the email field. Registration button does nothing. |
| User enters mismatched passwords | Validation error shown on confirm password field. |
| Firebase Auth fails (e.g. email already registered) | Toast with `getString(R.string.error_registration_failed)` + the Firebase error message. User stays on RegisterActivity. |
| Auth succeeds but Firestore write fails | Toast with `getString(R.string.error_firestore_write)`. The Firebase Auth account is still created. On next login, `getCurrentUserRole` will fail because no Firestore doc exists — this is caught and the user sees an error. A future improvement could retry the Firestore write. |
| User taps back on PrivacyPolicyActivity | Standard Android back behavior — returns to RegisterActivity. User must re-register or close the app. The Firebase Auth account already exists, so the email is "taken". Consider: should PrivacyPolicyActivity use `finish()` on back press and route to LoginActivity instead? |
| Firestore role field is null or missing | `getCurrentUserRole` callback fails with an error. Toast shown. User stays on login screen. |
| Network unavailable during registration | Firebase Auth will fail with a network error. Toast shown. Firestore calls will also fail. The app does not attempt offline registration. |
| User kills app between registration and privacy policy | On next launch, Firebase Auth session exists. Auto-redirect triggers. `getCurrentUserRole` succeeds (Firestore doc exists). User goes to StudentHomeActivity, skipping the privacy policy. This is acceptable for a prototype — a production app would track a `policyAccepted` flag. |
