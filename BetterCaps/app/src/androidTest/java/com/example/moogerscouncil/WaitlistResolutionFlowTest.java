/*
 * WaitlistResolutionFlowTest.java
 * Role: Espresso UI tests verifying that AvailabilitySetupActivity still
 *       renders correctly after the auto-resolution hook was wired in.
 *       End-to-end resolution testing requires a live Firestore emulator
 *       and is covered by integration tests.
 *
 * Part of the BetterCAPS counseling platform.
 */
package com.example.moogerscouncil;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.firebase.auth.FirebaseAuth;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

/**
 * Smoke tests confirming AvailabilitySetupActivity renders after the Sprint-8
 * auto-resolution hook was added. Full resolution flows require an authenticated
 * counselor session and are verified manually against the staging environment.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class WaitlistResolutionFlowTest {

    @Before
    public void setUp() throws Exception {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            com.google.android.gms.tasks.Tasks.await(
                    auth.signInAnonymously(), 30, TimeUnit.SECONDS);
        }
    }

    @Test
    public void availabilitySetupActivityRendersAddSlotButton() {
        ActivityScenario.launch(AvailabilitySetupActivity.class);
        onView(withId(R.id.buttonAddSlot)).check(matches(isDisplayed()));
    }

    @Test
    public void availabilitySetupActivityRendersSlotList() {
        ActivityScenario.launch(AvailabilitySetupActivity.class);
        onView(withId(R.id.recyclerSlots)).check(matches(isDisplayed()));
    }
}
