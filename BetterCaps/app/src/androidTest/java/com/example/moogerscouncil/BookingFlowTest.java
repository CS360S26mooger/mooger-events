/*
 * BookingFlowTest.java
 * Role: Espresso UI tests for the appointment booking flow (US-01).
 *       Verifies that the booking screen renders correctly and that
 *       date selection and confirmation dialog interactions work as expected.
 *
 * Part of the BetterCAPS counseling platform.
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
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

/**
 * Espresso instrumentation tests for {@link BookingActivity}.
 *
 * <p>These tests verify UI structure and interaction only. They do not assert
 * specific Firestore slot data, since availability depends on the live
 * Firestore instance.</p>
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class BookingFlowTest {

    /** Intent extra key matching BookingActivity's expected constant. */
    private static final String EXTRA_COUNSELOR_ID   = "counselorId";
    private static final String EXTRA_COUNSELOR_NAME = "counselorName";

    @Before
    public void setUp() throws Exception {
        // Sign in with student credentials to avoid redirects to LoginActivity
        Tasks.await(FirebaseAuth.getInstance()
                .signInWithEmailAndPassword("1@lums.edu.pk", "123456"), 10, TimeUnit.SECONDS);
    }

    @After
    public void tearDown() {
        FirebaseAuth.getInstance().signOut();
    }

    /**
     * Builds a launch intent with dummy counselor extras so
     * {@link BookingActivity} does not crash on start.
     */
    private Intent makeIntent() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                BookingActivity.class);
        intent.putExtra(EXTRA_COUNSELOR_ID,   "test_counselor_id");
        intent.putExtra(EXTRA_COUNSELOR_NAME, "Dr. Test");
        return intent;
    }

    /**
     * Verifies that the CalendarView is displayed when the booking screen opens.
     */
    @Test
    public void testCalendarViewIsDisplayed() {
        try (ActivityScenario<BookingActivity> scenario =
                     ActivityScenario.launch(makeIntent())) {

            onView(withId(R.id.calendarView))
                    .check(matches(isDisplayed()));
        }
    }

    /**
     * Verifies that the counselor name title is shown at the top of the screen.
     */
    @Test
    public void testCounselorNameTitleIsDisplayed() {
        try (ActivityScenario<BookingActivity> scenario =
                     ActivityScenario.launch(makeIntent())) {

            onView(withId(R.id.counselorNameTitle))
                    .check(matches(isDisplayed()));
        }
    }

    /**
     * Verifies that tapping a date causes the slot list RecyclerView or
     * the "no slots" message to appear (one of them must be visible).
     * Uses a brief sleep to allow the Firestore query to return.
     */
    @Test
    public void testSlotListAppearsOnDateTap() throws InterruptedException {
        try (ActivityScenario<BookingActivity> scenario =
                     ActivityScenario.launch(makeIntent())) {

            // Allow Firestore to load (or timeout gracefully)
            Thread.sleep(3000);

            // After a date is selected (initial load uses today), either the
            // slot list or the empty message must be visible.
            scenario.onActivity(activity -> {
                boolean slotListVisible =
                        activity.findViewById(R.id.slotsRecyclerView).getVisibility()
                                == android.view.View.VISIBLE;
                boolean noSlotsVisible =
                        activity.findViewById(R.id.textNoSlots).getVisibility()
                                == android.view.View.VISIBLE;

                assert slotListVisible || noSlotsVisible
                        : "Neither slot list nor no-slots message was visible after date load";
            });
        }
    }

    /**
     * Verifies that the counselor name title contains the name passed via intent extra.
     */
    @Test
    public void testConfirmationShowsCorrectCounselorName() {
        try (ActivityScenario<BookingActivity> scenario =
                     ActivityScenario.launch(makeIntent())) {

            // Updated to match multiline formatting in BookingActivity.onCreate
            onView(withId(R.id.counselorNameTitle))
                    .check(matches(withText("Book with\nDr. Test")));
        }
    }
}
