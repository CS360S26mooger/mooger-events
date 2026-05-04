package com.example.moogerscouncil;

import java.util.List;

/**
 * Computes whether an appointment belongs to a student who has prior session history.
 */
public final class ReturningStudentHelper {
    private ReturningStudentHelper() {}

    public static void markReturningStudents(List<Appointment> appointments) {
        if (appointments == null) return;
        for (Appointment current : appointments) {
            current.setReturningStudent(isReturningStudent(appointments, current));
        }
    }

    public static boolean isReturningStudent(List<Appointment> appointments, Appointment current) {
        if (appointments == null || current == null || current.getStudentId() == null
                || current.getDate() == null) {
            return false;
        }
        for (Appointment previous : appointments) {
            if (previous == null || previous == current) continue;
            if (current.getId() != null && current.getId().equals(previous.getId())) continue;
            if (!current.getStudentId().equals(previous.getStudentId())) continue;
            if (!isQualifyingPriorStatus(previous.getStatus())) continue;
            if (previous.getDate() == null || previous.getDate().compareTo(current.getDate()) >= 0) {
                continue;
            }
            return true;
        }
        return false;
    }

    private static boolean isQualifyingPriorStatus(String status) {
        return "COMPLETED".equals(status) || "CONFIRMED".equals(status);
    }
}
