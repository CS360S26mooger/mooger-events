package com.example.moogerscouncil;

import android.content.Intent;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Unit tests for {@link CalendarSyncHelper}. */
public class CalendarSyncHelperTest {

    @Test
    public void insertIntentUsesActionInsert() {
        assertEquals(Intent.ACTION_INSERT, CalendarSyncHelper.getInsertEventAction());
    }

    @Test
    public void icsStringContainsEventAndSummary() {
        String ics = CalendarSyncHelper.buildIcsLine(appointment(), "Dr. Test");

        assertTrue(ics.contains("BEGIN:VEVENT"));
        assertTrue(ics.contains("SUMMARY"));
    }

    private Appointment appointment() {
        Appointment appointment = new Appointment();
        appointment.setDate("2026-05-03");
        appointment.setTime("10:00");
        appointment.setStatus("CONFIRMED");
        return appointment;
    }
}
