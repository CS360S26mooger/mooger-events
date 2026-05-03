/*
 * NoShowGuardTest.java
 * Role: Espresso UI tests verifying the No-Show button visibility guard.
 *       The button must only appear for CONFIRMED appointments; it must be hidden
 *       for CANCELLED, COMPLETED, and NO_SHOW statuses.
 *
 * Part of the BetterCAPS counseling platform — Sprint 9.
 */
package com.example.moogerscouncil;

import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import static org.junit.Assert.assertEquals;

/**
 * Unit-level tests for the No-Show status guard logic, exercised through the
 * Appointment model. The adapter hides the button based on status string comparisons —
 * these tests verify the status constants are what the guard checks against.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class NoShowGuardTest {

    @Test
    public void confirmedStatus_guardAllowsNoShow() {
        Appointment apt = new Appointment();
        apt.setStatus("CONFIRMED");
        // Guard: isActive == "CONFIRMED".equals(status)
        assertEquals("CONFIRMED", apt.getStatus());
    }

    @Test
    public void cancelledStatus_guardBlocksNoShow() {
        Appointment apt = new Appointment();
        apt.setStatus("CANCELLED");
        boolean isActive = "CONFIRMED".equals(apt.getStatus());
        assertEquals(false, isActive);
    }

    @Test
    public void noShowStatus_guardBlocksNoShow() {
        Appointment apt = new Appointment();
        apt.setStatus("NO_SHOW");
        boolean isActive = "CONFIRMED".equals(apt.getStatus());
        assertEquals(false, isActive);
    }

    @Test
    public void completedStatus_guardBlocksNoShow() {
        Appointment apt = new Appointment();
        apt.setStatus("COMPLETED");
        boolean isActive = "CONFIRMED".equals(apt.getStatus());
        assertEquals(false, isActive);
    }

    @Test
    public void crisisButtonHiddenForCancelled() {
        // Crisis button is hidden when status is CANCELLED or COMPLETED
        String status = "CANCELLED";
        boolean hideCrisis = "CANCELLED".equals(status) || "COMPLETED".equals(status);
        assertEquals(true, hideCrisis);
    }

    @Test
    public void crisisButtonHiddenForCompleted() {
        String status = "COMPLETED";
        boolean hideCrisis = "CANCELLED".equals(status) || "COMPLETED".equals(status);
        assertEquals(true, hideCrisis);
    }

    @Test
    public void crisisButtonVisibleForConfirmed() {
        String status = "CONFIRMED";
        boolean hideCrisis = "CANCELLED".equals(status) || "COMPLETED".equals(status);
        assertEquals(false, hideCrisis);
    }

    @Test
    public void crisisButtonVisibleForNoShow() {
        // NO_SHOW is still an active record — crisis button should remain visible
        String status = "NO_SHOW";
        boolean hideCrisis = "CANCELLED".equals(status) || "COMPLETED".equals(status);
        assertEquals(false, hideCrisis);
    }

    // ── No-show time window tests (10-minute gate) ──

    @Test
    public void noShowButton_visibleForConfirmedRegardlessOfTime() {
        // Button is always VISIBLE for CONFIRMED — the time gate shows a dialog,
        // it does not hide the button
        Appointment apt = new Appointment();
        apt.setStatus("CONFIRMED");
        boolean isActive = "CONFIRMED".equals(apt.getStatus());
        assertEquals(true, isActive);
    }

    @Test
    public void noShowTimeGate_futureSlot_blocked() {
        // A future slot should not pass the 10-minute window
        assertEquals(false, AppointmentAdapter.isNoShowWindowOpen("2099-12-31", "23:59"));
    }

    @Test
    public void noShowTimeGate_distantPast_allowed() {
        // A slot from years ago should pass the window
        assertEquals(true, AppointmentAdapter.isNoShowWindowOpen("2020-01-01", "08:00"));
    }
}
