/*
 * OnLeaveFlowTest.java
 * Role: Espresso UI tests for the counselor on-leave flow (US-19).
 *       Verifies the toggle, message/referral fields, directory badge,
 *       and profile on-leave card display.
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
import static org.hamcrest.Matchers.not;

/**
 * Espresso instrumentation tests for the counselor on-leave flow (US-19).
 *
 * <p>Tests cover the profile edit screen toggle behaviour and the student-facing
 * directory badge and profile card display.</p>
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class OnLeaveFlowTest {

    /**
     * Builds a launch intent for {@link CounselorProfileEditActivity}.
     */
    private Intent editProfileIntent() {
        return new Intent(ApplicationProvider.getApplicationContext(),
                CounselorProfileEditActivity.class);
    }

    /**
     * Verifies that the on-leave SwitchMaterial is visible in
     * {@link CounselorProfileEditActivity}.
     */
    @Test
    public void testOnLeaveToggleIsDisplayed() {
        try (ActivityScenario<CounselorProfileEditActivity> scenario =
                     ActivityScenario.launch(editProfileIntent())) {

            onView(withId(R.id.switchOnLeave))
                    .check(matches(isDisplayed()));
        }
    }

    /**
     * Verifies that toggling the on-leave switch on reveals the leave message
     * TextInputLayout (which is initially gone).
     */
    @Test
    public void testToggleShowsMessageField() {
        try (ActivityScenario<CounselorProfileEditActivity> scenario =
                     ActivityScenario.launch(editProfileIntent())) {

            // Initially hidden
            onView(withId(R.id.layoutLeaveMessage))
                    .check(matches(not(isDisplayed())));

            // Toggle on
            onView(withId(R.id.switchOnLeave)).perform(click());

            // Now visible
            onView(withId(R.id.layoutLeaveMessage))
                    .check(matches(isDisplayed()));
        }
    }

    /**
     * Verifies that toggling the on-leave switch on reveals the referral counselor
     * dropdown (which is initially gone).
     */
    @Test
    public void testToggleShowsReferralDropdown() {
        try (ActivityScenario<CounselorProfileEditActivity> scenario =
                     ActivityScenario.launch(editProfileIntent())) {

            // Initially hidden
            onView(withId(R.id.layoutReferralCounselor))
                    .check(matches(not(isDisplayed())));

            // Toggle on
            onView(withId(R.id.switchOnLeave)).perform(click());

            // Now visible
            onView(withId(R.id.layoutReferralCounselor))
                    .check(matches(isDisplayed()));
        }
    }

    /**
     * Verifies that the counselor directory shows the "Currently Away" badge
     * for counselors with onLeave == true.
     * Note: requires a test counselor document with onLeave=true in Firestore.
     */
    @Test
    public void testOnLeaveCounselorShowsBadgeInDirectory() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                CounselorListActivity.class);
        try (ActivityScenario<CounselorListActivity> scenario =
                     ActivityScenario.launch(intent)) {

            // The badge text is "Currently Away" for on-leave counselors
            onView(withText(R.string.currently_away))
                    .check(matches(isDisplayed()));
        }
    }

    /**
     * Verifies that launching {@link CounselorProfileActivity} for an on-leave counselor
     * shows the on-leave card.
     * Note: requires a COUNSELOR_ID extra pointing to a counselor with onLeave=true.
     */
    @Test
    public void testOnLeaveCounselorProfileShowsLeaveCard() {
        // Use a known test counselor ID with onLeave=true in Firestore
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                CounselorProfileActivity.class);
        intent.putExtra("COUNSELOR_ID", "test_on_leave_counselor");

        try (ActivityScenario<CounselorProfileActivity> scenario =
                     ActivityScenario.launch(intent)) {

            onView(withId(R.id.cardOnLeave))
                    .check(matches(isDisplayed()));
        }
    }
}
