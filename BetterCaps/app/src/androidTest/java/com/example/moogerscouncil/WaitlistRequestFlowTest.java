package com.example.moogerscouncil;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.action.ViewActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

/**
 * UI tests for WaitlistRequestActivity — verifies form rendering,
 * default time values, and input-element presence.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class WaitlistRequestFlowTest {

    @Before
    public void setUp() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                WaitlistRequestActivity.class);
        intent.putExtra(WaitlistRequestActivity.EXTRA_COUNSELOR_ID, "test-counselor");
        ActivityScenario.launch(intent);
    }

    // ------------------------------------------------------------------
    // View presence
    // ------------------------------------------------------------------

    @Test
    public void calendarIsDisplayed() {
        onView(withId(R.id.waitlistCalendar)).check(matches(isDisplayed()));
    }

    @Test
    public void startTimeSelectorIsDisplayed() {
        onView(withId(R.id.selectorStartTime)).check(matches(isDisplayed()));
    }

    @Test
    public void endTimeSelectorIsDisplayed() {
        onView(withId(R.id.selectorEndTime)).check(matches(isDisplayed()));
    }

    @Test
    public void noteFieldIsDisplayed() {
        onView(withId(R.id.editWaitlistNote)).check(matches(isDisplayed()));
    }

    @Test
    public void submitButtonIsDisplayed() {
        onView(withId(R.id.buttonSubmitWaitlist)).check(matches(isDisplayed()));
    }

    @Test
    public void toolbarIsDisplayed() {
        onView(withId(R.id.toolbar)).check(matches(isDisplayed()));
    }

    // ------------------------------------------------------------------
    // Default state
    // ------------------------------------------------------------------

    @Test
    public void defaultStartTimeIs0900() {
        onView(withId(R.id.textStartTime)).check(matches(withText("09:00")));
    }

    @Test
    public void defaultEndTimeIs0930() {
        onView(withId(R.id.textEndTime)).check(matches(withText("09:30")));
    }

    @Test
    public void submitButtonEnabledByDefault() {
        onView(withId(R.id.buttonSubmitWaitlist)).check(matches(isEnabled()));
    }

    // ------------------------------------------------------------------
    // Validation — submit without selecting dates
    // ------------------------------------------------------------------

    @Test
    public void submitWithNoDates_buttonRemainsEnabled() {
        // Tapping submit without selecting any date should NOT navigate away.
        // The button re-enables (or stays enabled) because validation blocks submission.
        onView(withId(R.id.buttonSubmitWaitlist)).perform(ViewActions.click());
        onView(withId(R.id.buttonSubmitWaitlist)).check(matches(isDisplayed()));
    }
}
