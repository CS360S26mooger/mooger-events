/*
 * CounselorLogoutFlowTest.java
 * Role: Espresso UI tests verifying the two-step logout confirmation flow on the
 *       counselor dashboard. Tapping logout must show a confirmation dialog;
 *       tapping Cancel must dismiss it without signing out.
 *
 * Part of the BetterCAPS counseling platform — Sprint 9.
 */
package com.example.moogerscouncil;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
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
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

/**
 * Verifies the counselor logout confirmation dialog appears and responds correctly.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class CounselorLogoutFlowTest {

    @Before
    public void setUp() throws Exception {
        // Sign in with counselor credentials to avoid redirects to LoginActivity
        Tasks.await(FirebaseAuth.getInstance()
                .signInWithEmailAndPassword("shaaahbaz@lums.edu.pk", "shahbaz"), 10, TimeUnit.SECONDS);
    }

    @After
    public void tearDown() {
        FirebaseAuth.getInstance().signOut();
    }

    @Test
    public void logoutButton_isVisible() {
        try (ActivityScenario<CounselorDashboardActivity> scenario =
                     ActivityScenario.launch(makeIntent())) {
            onView(withId(R.id.logoutBtn)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void logoutButton_tapShowsConfirmationDialog() {
        try (ActivityScenario<CounselorDashboardActivity> scenario =
                     ActivityScenario.launch(makeIntent())) {
            onView(withId(R.id.logoutBtn)).perform(click());
            onView(withText(R.string.logout_confirm_message)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void logoutDialog_cancelDismissesWithoutNavigation() {
        try (ActivityScenario<CounselorDashboardActivity> scenario =
                     ActivityScenario.launch(makeIntent())) {
            onView(withId(R.id.logoutBtn)).perform(click());
            onView(withText(R.string.button_cancel)).perform(click());
            // Dashboard should still be visible after cancel
            onView(withId(R.id.logoutBtn)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void logoutDialog_confirmButtonVisible() {
        try (ActivityScenario<CounselorDashboardActivity> scenario =
                     ActivityScenario.launch(makeIntent())) {
            onView(withId(R.id.logoutBtn)).perform(click());
            onView(withText(R.string.logout_confirm_yes)).check(matches(isDisplayed()));
        }
    }

    private Intent makeIntent() {
        return new Intent(ApplicationProvider.getApplicationContext(),
                CounselorDashboardActivity.class);
    }
}
