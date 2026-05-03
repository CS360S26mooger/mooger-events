package com.example.moogerscouncil;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Instrumented tests for WaitlistAdapter.statusLabel() — verifies the mapping
 * from raw Firestore status strings to user-facing display labels.
 *
 * statusLabel() is package-private to make it reachable from this test class.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class WaitlistAdapterLogicTest {

    private Context ctx;
    private WaitlistAdapter adapter;

    @Before
    public void setUp() {
        ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
        adapter = new WaitlistAdapter(ctx, Collections.emptyList(), new HashMap<>(), entry -> {});
    }

    @Test
    public void activeStatus_showsActiveLabel() {
        String label = adapter.statusLabel(WaitlistEntry.STATUS_ACTIVE);
        assertEquals(ctx.getString(R.string.waitlist_status_active), label);
    }

    @Test
    public void resolvedStatus_showsResolvedLabel() {
        String label = adapter.statusLabel(WaitlistEntry.STATUS_RESOLVED);
        assertEquals(ctx.getString(R.string.waitlist_status_resolved), label);
    }

    @Test
    public void cancelledStatus_showsCancelledLabel() {
        String label = adapter.statusLabel(WaitlistEntry.STATUS_CANCELLED);
        assertEquals(ctx.getString(R.string.waitlist_status_cancelled), label);
    }

    @Test
    public void legacyOffered_mapsToResolvedLabel() {
        String label = adapter.statusLabel(WaitlistEntry.STATUS_OFFERED);
        assertEquals(ctx.getString(R.string.waitlist_status_resolved), label);
    }

    @Test
    public void legacyBooked_mapsToResolvedLabel() {
        String label = adapter.statusLabel(WaitlistEntry.STATUS_BOOKED);
        assertEquals(ctx.getString(R.string.waitlist_status_resolved), label);
    }

    @Test
    public void legacyExpired_mapsToCancelledLabel() {
        String label = adapter.statusLabel(WaitlistEntry.STATUS_EXPIRED);
        assertEquals(ctx.getString(R.string.waitlist_status_cancelled), label);
    }

    @Test
    public void unknownStatus_defaultsToActiveLabel() {
        String label = adapter.statusLabel("UNKNOWN_FUTURE_STATUS");
        assertEquals(ctx.getString(R.string.waitlist_status_active), label);
        assertNotNull(label);
    }

    @Test
    public void nullStatus_defaultsToActiveLabel() {
        String label = adapter.statusLabel(null);
        assertEquals(ctx.getString(R.string.waitlist_status_active), label);
    }
}
