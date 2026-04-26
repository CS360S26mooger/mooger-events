/*
 * EmergencyButtonTest.java
 * Role: Espresso UI tests for the emergency FAB and crisis dialog (US-20).
 *       Verifies that the emergency button is visible on the student home screen
 *       and that tapping it opens the emergency contact dialog with the correct options.
 *
 * Part of the BetterCAPS counseling platform.
 */
package com.example.moogerscouncil;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

/**
 * Espresso instrumentation tests for the emergency FAB and
 * {@link EmergencyDialogFragment} (US-20).
 *
 * <p>These tests verify that the emergency button is always accessible on the
 * student home screen and that the dialog surface contains the correct
 * contact options.</p>
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class EmergencyButtonTest {

    /**
     * Builds a launch intent for {@link StudentHomeActivity}.
     */
    private Intent makeIntent() {
        return new Intent(ApplicationProvider.getApplicationContext(),
                StudentHomeActivity.class);
    }

    /**
     * Verifies that the emergency FAB is visible on the student home screen.
     * The FAB must never be hidden behind the bottom navigation.
     */
    @Test
    public void testEmergencyFabIsVisible() {
        try (ActivityScenario<StudentHomeActivity> scenario =
                     ActivityScenario.launch(makeIntent())) {

            onView(withId(R.id.crisisBanner))
                    .check(matches(isDisplayed()));
        }
    }

    /**
     * Taps the emergency FAB and verifies that the emergency dialog opens.
     * Checks for the dialog title string defined in strings.xml.
     */
    @Test
    public void testFabTapOpensDialog() {
        try (ActivityScenario<StudentHomeActivity> scenario =
                     ActivityScenario.launch(makeIntent())) {

            onView(withId(R.id.crisisBanner)).perform(click());

            onView(withText(R.string.emergency_title))
                    .check(matches(isDisplayed()));
        }
    }

    /**
     * Verifies that the dialog shows the "Call Crisis Line" option after the FAB is tapped.
     */
    @Test
    public void testDialogShowsCrisisLineOption() {
        try (ActivityScenario<StudentHomeActivity> scenario =
                     ActivityScenario.launch(makeIntent())) {

            onView(withId(R.id.crisisBanner)).perform(click());

            onView(withText(R.string.call_crisis_line))
                    .check(matches(isDisplayed()));
        }
    }

    /**
     * Verifies that the dialog shows the "Campus Emergency" option after the FAB is tapped.
     */
    @Test
    public void testDialogShowsCampusSecurityOption() {
        try (ActivityScenario<StudentHomeActivity> scenario =
                     ActivityScenario.launch(makeIntent())) {

            onView(withId(R.id.crisisBanner)).perform(click());

            onView(withText(R.string.call_campus_security))
                    .check(matches(isDisplayed()));
        }
    }

    /**
     * Verifies that the Dismiss button is present in the emergency dialog.
     */
    @Test
    public void testDialogShowsDismissButton() {
        try (ActivityScenario<StudentHomeActivity> scenario =
                     ActivityScenario.launch(makeIntent())) {

            onView(withId(R.id.crisisBanner)).perform(click());

            onView(withText(R.string.dismiss))
                    .check(matches(isDisplayed()));
        }
    }
}
