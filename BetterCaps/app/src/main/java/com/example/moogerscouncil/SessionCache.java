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

    private Counselor cachedSingleCounselor;
    private String cachedSingleCounselorId;
    private long singleCounselorFetchedAt;

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
     * Returns the cached counselor list if still fresh, or null if expired/absent.
     */
    public List<Counselor> getCounselors() {
        if (cachedCounselors != null && !isExpired(counselorsFetchedAt, COUNSELORS_TTL_MS)) {
            return cachedCounselors;
        }
        return null;
    }

    /** Stores the counselor list in cache. */
    public void putCounselors(List<Counselor> counselors) {
        this.cachedCounselors = counselors;
        this.counselorsFetchedAt = now();
    }

    // ── Student appointments ──

    /**
     * Returns the cached appointment list if still fresh and for the same student,
     * or null if expired/absent/wrong student.
     */
    public List<Appointment> getStudentAppointments(String studentId) {
        if (cachedStudentAppointments != null
                && studentId.equals(cachedAppointmentsStudentId)
                && !isExpired(appointmentsFetchedAt, APPOINTMENTS_TTL_MS)) {
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
     * Returns the cached single counselor if still fresh and matching the ID.
     */
    public Counselor getSingleCounselor(String counselorId) {
        if (cachedSingleCounselor != null
                && counselorId.equals(cachedSingleCounselorId)
                && !isExpired(singleCounselorFetchedAt, COUNSELORS_TTL_MS)) {
            return cachedSingleCounselor;
        }
        return null;
    }

    /** Stores a single counselor in cache. */
    public void putSingleCounselor(String counselorId, Counselor counselor) {
        this.cachedSingleCounselor = counselor;
        this.cachedSingleCounselorId = counselorId;
        this.singleCounselorFetchedAt = now();
    }

    // ── Invalidation ──

    /** Call after a booking or cancellation to force appointment refresh. */
    public void invalidateAppointments() {
        cachedStudentAppointments = null;
    }

    /** Call after profile edit to force counselor list refresh. */
    public void invalidateCounselors() {
        cachedCounselors = null;
        cachedSingleCounselor = null;
    }

    /** Call on logout to wipe everything. */
    public void clearAll() {
        cachedStudent = null;
        cachedCounselors = null;
        cachedStudentAppointments = null;
        cachedSingleCounselor = null;
    }

    // ── Internal ──

    private boolean isExpired(long fetchedAt, long ttlMs) {
        return (now() - fetchedAt) > ttlMs;
    }

    private long now() {
        return System.currentTimeMillis();
    }
}
