/*
 * LoginRoleRoutingTest.java
 * Role: Espresso instrumented tests for the LoginActivity role-selector UI.
 *       Verifies that all role buttons are displayed and that the login button
 *       text updates to reflect the selected role.
 *
 * Note: Tests that require actual Firebase Auth (sign-in success routing) are
 *       only run with a configured Firebase emulator and are not included here.
 *
 * Part of the BetterCAPS counseling platform.
 */
package com.example.moogerscouncil;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Espresso tests for the role-selector UI in {@link LoginActivity}.
 */
@RunWith(AndroidJUnit4.class)
public class LoginRoleRoutingTest {

    @Rule
    public ActivityScenarioRule<LoginActivity> activityRule =
            new ActivityScenarioRule<>(LoginActivity.class);

    /**
     * All three role buttons must be visible when the login screen opens.
     */
    @Test
    public void testRoleButtonsAreDisplayed() {
        onView(withId(R.id.btnStudent)).check(matches(isDisplayed()));
        onView(withId(R.id.btnCounselor)).check(matches(isDisplayed()));
        onView(withId(R.id.btnAdmin)).check(matches(isDisplayed()));
    }

    /**
     * Tapping the Counselor button must update the login button label to
     * include "Counselor".
     */
    @Test
    public void testSelectCounselorUpdatesLoginButtonText() {
        onView(withId(R.id.btnCounselor)).perform(click());
        onView(withId(R.id.loginButton)).check(matches(withText(containsString("Counselor"))));
    }

    /**
     * Tapping the Admin button must update the login button label to include "Admin".
     */
    @Test
    public void testSelectAdminUpdatesLoginButtonText() {
        onView(withId(R.id.btnAdmin)).perform(click());
        onView(withId(R.id.loginButton)).check(matches(withText(containsString("Admin"))));
    }

    /**
     * The sign-up link must be visible on the login screen.
     */
    @Test
    public void testSignUpLinkIsDisplayed() {
        onView(withId(R.id.registerLink)).check(matches(isDisplayed()));
    }
}
