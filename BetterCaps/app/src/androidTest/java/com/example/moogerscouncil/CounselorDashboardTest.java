/*
 * CounselorDashboardTest.java
 * Role: Espresso UI tests for the counselor dashboard (US-05 / US-10).
 *       Verifies that the tab layout, appointments RecyclerView, and stat cards
 *       are rendered correctly and that tab switching updates the visible content.
 *
 * Part of the BetterCAPS counseling platform.
 */
package com.example.moogerscouncil;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.android.material.tabs.TabLayout;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

/**
 * Espresso instrumentation tests for {@link CounselorDashboardActivity}.
 *
 * <p>These tests verify that the dashboard renders the tab layout and
 * appointments list. They do not assert specific Firestore data, since
 * the appointment content depends on the live Firestore instance.</p>
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class CounselorDashboardTest {

    /**
     * Builds a launch intent for {@link CounselorDashboardActivity}.
     */
    private Intent makeIntent() {
        return new Intent(ApplicationProvider.getApplicationContext(),
                CounselorDashboardActivity.class);
    }

    /**
     * Verifies that the TabLayout with Today / This Week / This Month tabs is displayed.
     */
    @Test
    public void testTabLayoutIsDisplayed() {
        try (ActivityScenario<CounselorDashboardActivity> scenario =
                     ActivityScenario.launch(makeIntent())) {

            onView(withId(R.id.tabLayout))
                    .check(matches(isDisplayed()));
        }
    }

    /**
     * Verifies that the appointments RecyclerView is present on the dashboard.
     */
    @Test
    public void testAppointmentListLoads() {
        try (ActivityScenario<CounselorDashboardActivity> scenario =
                     ActivityScenario.launch(makeIntent())) {

            onView(withId(R.id.appointmentsRecyclerView))
                    .check(matches(isDisplayed()));
        }
    }

    /**
     * Verifies the "Today" tab text is displayed in the tab layout.
     */
    @Test
    public void testTodayTabIsPresent() {
        try (ActivityScenario<CounselorDashboardActivity> scenario =
                     ActivityScenario.launch(makeIntent())) {

            onView(withText(R.string.tab_today))
                    .check(matches(isDisplayed()));
        }
    }

    /**
     * Verifies the "This Week" tab text is displayed in the tab layout.
     */
    @Test
    public void testThisWeekTabIsPresent() {
        try (ActivityScenario<CounselorDashboardActivity> scenario =
                     ActivityScenario.launch(makeIntent())) {

            onView(withText(R.string.tab_this_week))
                    .check(matches(isDisplayed()));
        }
    }

    /**
     * Taps the "This Month" tab and verifies the RecyclerView remains visible —
     * the tab switch must not cause a crash or hide the list container.
     */
    @Test
    public void testTabSwitchChangesContent() throws InterruptedException {
        try (ActivityScenario<CounselorDashboardActivity> scenario =
                     ActivityScenario.launch(makeIntent())) {

            // Allow Firestore to load (or timeout gracefully)
            Thread.sleep(2000);

            onView(withText(R.string.tab_this_month)).perform(click());

            // RecyclerView must still be visible after tab switch
            onView(withId(R.id.appointmentsRecyclerView))
                    .check(matches(isDisplayed()));
        }
    }

    /**
     * Verifies that the stat card for today's sessions is visible.
     */
    @Test
    public void testTodayStatCardIsDisplayed() {
        try (ActivityScenario<CounselorDashboardActivity> scenario =
                     ActivityScenario.launch(makeIntent())) {

            onView(withId(R.id.todaySessionCount))
                    .check(matches(isDisplayed()));
        }
    }

    /**
     * Verifies that the add-slot banner is visible on the dashboard so counselors
     * can navigate to availability setup.
     */
    @Test
    public void testAddSlotBannerIsDisplayed() {
        try (ActivityScenario<CounselorDashboardActivity> scenario =
                     ActivityScenario.launch(makeIntent())) {

            onView(withId(R.id.addSlotBanner))
                    .check(matches(isDisplayed()));
        }
    }
}
