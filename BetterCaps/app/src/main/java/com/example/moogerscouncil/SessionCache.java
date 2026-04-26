/*
 * SessionCache.java
 * Role: Simple in-memory cache that survives Activity recreation within
 *       a single app session. Eliminates redundant Firestore fetches for
 *       data that changes infrequently (student profile, counselor list)
 *       while still allowing instant invalidation when mutations happen
 *       (new booking, cancellation, feedback submission).
 *
 * Design: Singleton holding typed nullable fields with timestamps.
 *         No disk persistence — cache is cleared on process death, which
 *         is acceptable since Firestore has its own offline cache layer.
 *
 * Part of the BetterCAPS counseling platform.
 */
package com.example.moogerscouncil;

import java.util.List;

/**
 * Application-scoped in-memory cache for frequently accessed Firestore data.
 *
 * <p>Cached entries have a time-to-live (TTL). Reads check the timestamp
 * and return null if the entry has expired, triggering a fresh fetch.
 * Mutations (booking, cancellation, feedback) call the appropriate
 * {@code invalidate*()} method to force the next read to go to Firestore.</p>
 *
 * <p>Thread safety: all access happens on the main (UI) thread in this app,
 * so no synchronization is needed.</p>
 */
public final class SessionCache {

    private static final SessionCache INSTANCE = new SessionCache();

    /** TTL for student profile data (name rarely changes). */
    private static final long PROFILE_TTL_MS = 10 * 60 * 1000;  // 10 minutes

    /** TTL for the counselor list (new counselors are rare). */
    private static final long COUNSELORS_TTL_MS = 5 * 60 * 1000; // 5 minutes

    /** TTL for appointment data (shorter — bookings can happen any time). */
    private static final long APPOINTMENTS_TTL_MS = 60 * 1000;   // 1 minute

    // ── Cached entries ──

    private Student cachedStudent;
    private long studentFetchedAt;

    private List<Counselor> cachedCounselors;
    private long counselorsFetchedAt;

    private List<Appointment> cachedStudentAppointments;
    private String cachedAppointmentsStudentId;
    private long appointmentsFetchedAt;

    private final java.util.Map<String, Counselor> cachedSingleCounselors = new java.util.HashMap<>();
    private long singleCounselorFetchedAt;

    // Slots per counselor — keyed by both Auth UID and Firestore doc ID so any lookup hits.
    // Session-scoped; invalidated after a booking so stale availability is never shown.
    private final java.util.Map<String, List<TimeSlot>> cachedSlots = new java.util.HashMap<>();

    private SessionCache() {}

    /** Returns the singleton instance. */
    public static SessionCache getInstance() {
        return INSTANCE;
    }

    // ── Student profile ──

    /**
     * Returns the cached student if still fresh, or null if expired/absent.
     *
     * @param uid The expected student UID (guards against stale data after logout).
     */
    public Student getStudent(String uid) {
        if (cachedStudent != null
                && uid.equals(cachedStudent.getUid())
                && !isExpired(studentFetchedAt, PROFILE_TTL_MS)) {
            return cachedStudent;
        }
        return null;
    }

    /** Stores the student profile in cache. */
    public void putStudent(Student student) {
        this.cachedStudent = student;
        this.studentFetchedAt = now();
    }

    // ── Counselor list ──

    /**
     * Returns the cached counselor list if present, or null if never loaded.
     * No TTL — the list is session-scoped and cleared on logout via {@link #clearAll()}.
     */
    public List<Counselor> getCounselors() {
        return cachedCounselors;
    }

    /** Stores the counselor list in cache. */
    public void putCounselors(List<Counselor> counselors) {
        this.cachedCounselors = counselors;
        this.counselorsFetchedAt = now();
    }

    // ── Student appointments ──

    /**
     * Returns the cached appointment list for the given student, or null if not yet loaded.
     * Session-scoped — invalidated explicitly by booking/cancellation via
     * {@link #invalidateAppointments()}, not by a TTL.
     */
    public List<Appointment> getStudentAppointments(String studentId) {
        if (cachedStudentAppointments != null
                && studentId.equals(cachedAppointmentsStudentId)) {
            return cachedStudentAppointments;
        }
        return null;
    }

    /** Stores the student appointment list in cache. */
    public void putStudentAppointments(String studentId, List<Appointment> appointments) {
        this.cachedStudentAppointments = appointments;
        this.cachedAppointmentsStudentId = studentId;
        this.appointmentsFetchedAt = now();
    }

    // ── Single counselor ──

    /**
     * Returns the cached counselor for the given ID if still fresh, or null if
     * expired or not stored. Accepts both Firestore doc IDs and Auth UIDs as keys
     * since {@link #putSingleCounselor} stores under both.
     */
    public Counselor getSingleCounselor(String counselorId) {
        if (!isExpired(singleCounselorFetchedAt, COUNSELORS_TTL_MS)) {
            return cachedSingleCounselors.get(counselorId);
        }
        return null;
    }

    /** Stores a counselor under the given key (doc ID or Auth UID). */
    public void putSingleCounselor(String counselorId, Counselor counselor) {
        cachedSingleCounselors.put(counselorId, counselor);
        singleCounselorFetchedAt = now();
    }

    // ── Slots ──

    /**
     * Returns the cached available-slot list for the given counselor key (uid or doc ID),
     * or null if not yet pre-warmed. Session-scoped — invalidated after a booking.
     */
    public List<TimeSlot> getSlots(String counselorKey) {
        if (counselorKey == null) return null;
        return cachedSlots.get(counselorKey);
    }

    /** Stores the available-slot list under the given key (uid or doc ID, or both). */
    public void putSlots(String counselorKey, List<TimeSlot> slots) {
        if (counselorKey == null) return;
        cachedSlots.put(counselorKey, slots);
    }

    /** Removes cached slots for the given counselor keys (call after booking). */
    public void invalidateSlots(String... counselorKeys) {
        for (String key : counselorKeys) {
            if (key != null) cachedSlots.remove(key);
        }
    }

    // ── Invalidation ──

    /** Call after a booking or cancellation to force appointment refresh. */
    public void invalidateAppointments() {
        cachedStudentAppointments = null;
    }

    /** Call after profile edit to force counselor list refresh. */
    public void invalidateCounselors() {
        cachedCounselors = null;
        cachedSingleCounselors.clear();
        cachedSlots.clear();
    }

    /** Call on logout to wipe everything. */
    public void clearAll() {
        cachedStudent = null;
        cachedCounselors = null;
        cachedStudentAppointments = null;
        cachedSingleCounselors.clear();
        cachedSlots.clear();
    }

    // ── Internal ──

    private boolean isExpired(long fetchedAt, long ttlMs) {
        return (now() - fetchedAt) > ttlMs;
    }

    private long now() {
        return System.currentTimeMillis();
    }
}
