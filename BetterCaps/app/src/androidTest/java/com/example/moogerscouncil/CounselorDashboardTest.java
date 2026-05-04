package com.example.moogerscouncil;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withSubstring;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class CounselorDashboardTest {
    @Before
    public void setUp() throws Exception {
        // Sign in with counselor credentials to avoid redirects to LoginActivity
        Tasks.await(FirebaseAuth.getInstance()
                .signInWithEmailAndPassword("shaaahbaz@lums.edu.pk", "shahbaz"), 10, TimeUnit.SECONDS);
        Thread.sleep(1000); // Increased guard to ensure CounselorDashboard stays open
    }

    @After
    public void tearDown() {
        FirebaseAuth.getInstance().signOut();
    }

    private Intent makeIntent() {
        return new Intent(ApplicationProvider.getApplicationContext(), CounselorDashboardActivity.class);
    }

    @Test
    public void testTabLayoutIsDisplayed() {
        try (ActivityScenario<CounselorDashboardActivity> scenario = ActivityScenario.launch(makeIntent())) {
            onView(withId(R.id.tabLayout)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void testAppointmentListLoads() {
        try (ActivityScenario<CounselorDashboardActivity> scenario = ActivityScenario.launch(makeIntent())) {
            // A RecyclerView might be 0-height if empty, which isDisplayed() will reject.
            onView(withId(R.id.appointmentsRecyclerView))
                    .check(matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
        }
    }

    @Test
    public void testTodayTabIsPresent() throws InterruptedException {
        try (ActivityScenario<CounselorDashboardActivity> scenario = ActivityScenario.launch(makeIntent())) {
            Thread.sleep(1000);
            // Qualify the matcher to look inside the TabLayout to avoid matching the stat card
            onView(allOf(withText(R.string.tab_today), isDescendantOfA(withId(R.id.tabLayout))))
                    .check(matches(isDisplayed()));
        }
    }

    @Test
    public void testThisWeekTabIsPresent() throws InterruptedException {
        try (ActivityScenario<CounselorDashboardActivity> scenario = ActivityScenario.launch(makeIntent())) {
            Thread.sleep(2000);
            // Narrowing down to the tab layout to avoid matching the stat card with the same text
            onView(allOf(withSubstring("Week"), isDescendantOfA(withId(R.id.tabLayout))))
                    .perform(click());

            onView(allOf(withSubstring("Week"), isDescendantOfA(withId(R.id.tabLayout))))
                    .check(matches(isDisplayed()));
        }
    }

    @Test
    public void testTabSwitchChangesContent() throws InterruptedException {
        try (ActivityScenario<CounselorDashboardActivity> scenario = ActivityScenario.launch(makeIntent())) {
            Thread.sleep(2000);
            onView(allOf(withSubstring("Month"), isDescendantOfA(withId(R.id.tabLayout))))
                    .perform(click());
            onView(withId(R.id.appointmentsRecyclerView))
                    .check(matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
        }
    }

    @Test
    public void testTodayStatCardIsDisplayed() {
        try (ActivityScenario<CounselorDashboardActivity> scenario = ActivityScenario.launch(makeIntent())) {
            onView(withId(R.id.todaySessionCount)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void testAddSlotBannerIsDisplayed() {
        try (ActivityScenario<CounselorDashboardActivity> scenario = ActivityScenario.launch(makeIntent())) {
            onView(withId(R.id.addSlotBanner)).check(matches(isDisplayed()));
        }
    }
}
