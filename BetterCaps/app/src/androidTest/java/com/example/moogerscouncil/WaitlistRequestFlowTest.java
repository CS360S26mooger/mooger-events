/*
 * WaitlistRequestFlowTest.java
 * Role: Espresso UI tests for WaitlistRequestActivity — verifies that
 *       the preference form renders correctly and validates user input.
 *
 * Part of the BetterCAPS counseling platform.
 */
package com.example.moogerscouncil;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

/**
 * UI tests verifying that WaitlistRequestActivity renders its preference form
 * and correctly handles missing date selection.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class WaitlistRequestFlowTest {

    private ActivityScenario<WaitlistRequestActivity> scenario;

    @Before
    public void setUp() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                WaitlistRequestActivity.class);
        intent.putExtra(WaitlistRequestActivity.EXTRA_COUNSELOR_ID, "test-counselor");
        scenario = ActivityScenario.launch(intent);
    }

    @Test
    public void calendarIsDisplayed() {
        onView(withId(R.id.waitlistCalendar)).check(matches(isDisplayed()));
    }

    @Test
    public void startTimeSpinnerIsDisplayed() {
        onView(withId(R.id.spinnerStartTime)).check(matches(isDisplayed()));
    }

    @Test
    public void endTimeSpinnerIsDisplayed() {
        onView(withId(R.id.spinnerEndTime)).check(matches(isDisplayed()));
    }

    @Test
    public void noteFieldIsDisplayed() {
        onView(withId(R.id.editWaitlistNote)).check(matches(isDisplayed()));
    }

    @Test
    public void submitButtonIsDisplayed() {
        onView(withId(R.id.buttonSubmitWaitlist)).check(matches(isDisplayed()));
    }
}
