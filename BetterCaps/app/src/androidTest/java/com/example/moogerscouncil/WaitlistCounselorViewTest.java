/*
 * WaitlistCounselorViewTest.java
 * Role: Espresso UI tests for CounselorWaitlistActivity — verifies that
 *       the queue screen renders its core views.
 *
 * Part of the BetterCAPS counseling platform.
 */
package com.example.moogerscouncil;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

/**
 * UI tests verifying that CounselorWaitlistActivity renders its core views.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class WaitlistCounselorViewTest {

    @Before
    public void setUp() {
        ActivityScenario.launch(CounselorWaitlistActivity.class);
    }

    @Test
    public void recyclerViewIsPresent() {
        onView(withId(R.id.recyclerCounselorWaitlist)).check(matches(isDisplayed()));
    }

    @Test
    public void toolbarIsDisplayed() {
        onView(withId(R.id.toolbar)).check(matches(isDisplayed()));
    }
}
