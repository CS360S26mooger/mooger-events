package com.example.moogerscouncil;

import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Unit tests for the no-show 10-minute window logic in {@link AppointmentAdapter}. */
public class NoShowTimeWindowTest {

    @Test
    public void nullDate_returnsFalse() {
        assertFalse(AppointmentAdapter.isNoShowWindowOpen(null, "10:00"));
    }

    @Test
    public void nullTime_returnsFalse() {
        assertFalse(AppointmentAdapter.isNoShowWindowOpen("2026-05-04", null));
    }

    @Test
    public void futureSlot_returnsFalse() {
        assertFalse(AppointmentAdapter.isNoShowWindowOpen("2099-12-31", "23:59"));
    }

    @Test
    public void distantPastSlot_returnsTrue() {
        assertTrue(AppointmentAdapter.isNoShowWindowOpen("2020-01-01", "08:00"));
    }

    @Test
    public void slotExactly10MinutesAgo_returnsTrue() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        SimpleDateFormat tf = new SimpleDateFormat("HH:mm", Locale.US);
        Date now = new Date(System.currentTimeMillis() - 10 * 60 * 1000);
        assertTrue(AppointmentAdapter.isNoShowWindowOpen(sdf.format(now), tf.format(now)));
    }

    @Test
    public void slotJust5MinutesAgo_returnsFalse() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        SimpleDateFormat tf = new SimpleDateFormat("HH:mm", Locale.US);
        Date fiveMinAgo = new Date(System.currentTimeMillis() - 5 * 60 * 1000);
        assertFalse(AppointmentAdapter.isNoShowWindowOpen(sdf.format(fiveMinAgo), tf.format(fiveMinAgo)));
    }

    @Test
    public void invalidDateFormat_returnsFalse() {
        assertFalse(AppointmentAdapter.isNoShowWindowOpen("not-a-date", "10:00"));
    }

    @Test
    public void invalidTimeFormat_returnsFalse() {
        assertFalse(AppointmentAdapter.isNoShowWindowOpen("2026-05-04", "not-a-time"));
    }
}
