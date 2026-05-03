/*
 * FeedbackFlowTest.java
 * Role: Espresso UI tests for the anonymous post-session feedback flow (US-21).
 *       Verifies dialog display, rating validation, and prompt card visibility.
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
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

/**
 * Espresso instrumentation tests for the anonymous feedback flow (US-21).
 *
 * <p>Tests verify the feedback dialog UI elements and submit-button validation.
 * The prompt card appearance test requires a COMPLETED appointment in Firestore
 * without existing feedback.</p>
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class FeedbackFlowTest {

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
     * Builds a launch intent for {@link StudentHomeActivity}.
     */
    private Intent homeIntent() {
        return new Intent(ApplicationProvider.getApplicationContext(),
                StudentHomeActivity.class);
    }

    /**
     * Verifies that the feedback dialog contains a RatingBar when triggered via
     * the post-session feedback button.
     */
    @Test
    public void testFeedbackDialogDisplaysRatingBar() {
        try (ActivityScenario<StudentHomeActivity> scenario =
                     ActivityScenario.launch(homeIntent())) {

            onView(withId(R.id.btnPostSessionFeedback)).perform(click());

            // If pendingFeedbackAppointment is null the button shows a toast;
            // set up a COMPLETED appointment in test Firestore to get the dialog.
            // This test verifies the dialog layout when it does appear.
            onView(withId(R.id.feedbackRating))
                    .check(matches(isDisplayed()));
        }
    }

    /**
     * Verifies that the feedback dialog shows the comment EditText field.
     */
    @Test
    public void testFeedbackDialogDisplaysCommentField() {
        try (ActivityScenario<StudentHomeActivity> scenario =
                     ActivityScenario.launch(homeIntent())) {

            onView(withId(R.id.btnPostSessionFeedback)).perform(click());

            onView(withId(R.id.feedbackComment))
                    .check(matches(isDisplayed()));
        }
    }

    /**
     * Verifies that tapping Submit without selecting a rating shows the error toast.
     * The rating bar starts at 0, so tapping Submit immediately should trigger validation.
     */
    @Test
    public void testSubmitWithZeroRatingShowsError() {
        try (ActivityScenario<StudentHomeActivity> scenario =
                     ActivityScenario.launch(homeIntent())) {

            onView(withId(R.id.btnPostSessionFeedback)).perform(click());

            // Tap submit without setting a rating
            onView(withId(R.id.btnSubmitFeedback)).perform(click());

            onView(withText(R.string.error_rating_required))
                    .check(matches(isDisplayed()));
        }
    }

    /**
     * Verifies that the feedback prompt card appears on StudentHomeActivity
     * when there is a COMPLETED appointment without feedback.
     * Note: requires a COMPLETED appointment in Firestore for the test user.
     */
    @Test
    public void testFeedbackPromptCardAppears() {
        try (ActivityScenario<StudentHomeActivity> scenario =
                     ActivityScenario.launch(homeIntent())) {

            onView(withId(R.id.cardFeedbackPrompt))
                    .check(matches(isDisplayed()));
        }
    }
}
