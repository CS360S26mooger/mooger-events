package com.example.moogerscouncil;

import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the dashboard filtering logic used in
 * {@link CounselorDashboardActivity}: active-only filter,
 * past sessions filter, and CONFIRMED-only stat counts.
 */
public class DashboardFilterTest {

    private static final String TODAY = new SimpleDateFormat("yyyy-MM-dd", Locale.US)
            .format(new Date());

    @Test
    public void activeOnlyFilter_excludesCancelledAndNoShow() {
        List<Appointment> all = Arrays.asList(
                appointment("1", TODAY, "CONFIRMED"),
                appointment("2", TODAY, "CANCELLED"),
                appointment("3", TODAY, "NO_SHOW"),
                appointment("4", TODAY, "COMPLETED")
        );

        List<Appointment> visible = all.stream()
                .filter(a -> !"CANCELLED".equals(a.getStatus())
                        && !"NO_SHOW".equals(a.getStatus()))
                .collect(Collectors.toList());

        assertEquals(2, visible.size());
        assertEquals("CONFIRMED", visible.get(0).getStatus());
        assertEquals("COMPLETED", visible.get(1).getStatus());
    }

    @Test
    public void confirmedOnlyStats_countsCorrectly() {
        List<Appointment> all = Arrays.asList(
                appointment("1", TODAY, "CONFIRMED"),
                appointment("2", TODAY, "CONFIRMED"),
                appointment("3", TODAY, "CANCELLED"),
                appointment("4", TODAY, "COMPLETED"),
                appointment("5", TODAY, "NO_SHOW")
        );

        long confirmed = all.stream()
                .filter(a -> "CONFIRMED".equals(a.getStatus()))
                .count();

        assertEquals(2, confirmed);
    }

    @Test
    public void pastSessionsFilter_showsOnlyBookedPastAppointments() {
        List<Appointment> all = Arrays.asList(
                appointment("1", "2020-01-01", "CONFIRMED"),
                appointment("2", "2020-02-15", "COMPLETED"),
                appointment("3", "2020-03-01", "NO_SHOW"),
                appointment("4", "2020-04-01", "CANCELLED"),
                appointment("5", "2099-12-31", "CONFIRMED")
        );

        List<Appointment> past = all.stream()
                .filter(a -> a.getDate() != null && a.getDate().compareTo(TODAY) < 0)
                .filter(a -> {
                    String s = a.getStatus();
                    return "CONFIRMED".equals(s) || "COMPLETED".equals(s) || "NO_SHOW".equals(s);
                })
                .collect(Collectors.toList());

        assertEquals(3, past.size());
        // CANCELLED past appointments excluded
        assertTrue(past.stream().noneMatch(a -> "CANCELLED".equals(a.getStatus())));
        // Future appointments excluded
        assertTrue(past.stream().noneMatch(a -> a.getDate().compareTo(TODAY) >= 0));
    }

    @Test
    public void pastSessionsFilter_sortedNewestFirst() {
        List<Appointment> all = Arrays.asList(
                appointment("1", "2020-01-01", "COMPLETED"),
                appointment("2", "2020-06-15", "COMPLETED"),
                appointment("3", "2020-03-10", "COMPLETED")
        );

        List<Appointment> past = all.stream()
                .filter(a -> a.getDate().compareTo(TODAY) < 0)
                .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                .collect(Collectors.toList());

        assertEquals("2020-06-15", past.get(0).getDate());
        assertEquals("2020-03-10", past.get(1).getDate());
        assertEquals("2020-01-01", past.get(2).getDate());
    }

    @Test
    public void emptyMasterList_producesEmptyResults() {
        List<Appointment> all = Arrays.asList();

        List<Appointment> visible = all.stream()
                .filter(a -> !"CANCELLED".equals(a.getStatus()))
                .collect(Collectors.toList());

        assertEquals(0, visible.size());
    }

    // ── appointmentsChanged diff tests ──

    @Test
    public void appointmentsChanged_sameData_returnsFalse() {
        List<Appointment> current = Arrays.asList(
                appointment("1", TODAY, "CONFIRMED"),
                appointment("2", TODAY, "COMPLETED"));
        List<Appointment> incoming = Arrays.asList(
                appointment("1", TODAY, "CONFIRMED"),
                appointment("2", TODAY, "COMPLETED"));

        assertFalse(CounselorDashboardActivity.appointmentsChanged(current, incoming));
    }

    @Test
    public void appointmentsChanged_differentSize_returnsTrue() {
        List<Appointment> current = Arrays.asList(
                appointment("1", TODAY, "CONFIRMED"));
        List<Appointment> incoming = Arrays.asList(
                appointment("1", TODAY, "CONFIRMED"),
                appointment("2", TODAY, "CONFIRMED"));

        assertTrue(CounselorDashboardActivity.appointmentsChanged(current, incoming));
    }

    @Test
    public void appointmentsChanged_statusChanged_returnsTrue() {
        List<Appointment> current = Arrays.asList(
                appointment("1", TODAY, "CONFIRMED"));
        List<Appointment> incoming = Arrays.asList(
                appointment("1", TODAY, "NO_SHOW"));

        assertTrue(CounselorDashboardActivity.appointmentsChanged(current, incoming));
    }

    @Test
    public void appointmentsChanged_differentId_returnsTrue() {
        List<Appointment> current = Arrays.asList(
                appointment("1", TODAY, "CONFIRMED"));
        List<Appointment> incoming = Arrays.asList(
                appointment("99", TODAY, "CONFIRMED"));

        assertTrue(CounselorDashboardActivity.appointmentsChanged(current, incoming));
    }

    @Test
    public void appointmentsChanged_bothEmpty_returnsFalse() {
        assertFalse(CounselorDashboardActivity.appointmentsChanged(
                Arrays.asList(), Arrays.asList()));
    }

    private static Appointment appointment(String id, String date, String status) {
        Appointment apt = new Appointment();
        apt.setId(id);
        apt.setDate(date);
        apt.setTime("10:00");
        apt.setStatus(status);
        return apt;
    }
}
