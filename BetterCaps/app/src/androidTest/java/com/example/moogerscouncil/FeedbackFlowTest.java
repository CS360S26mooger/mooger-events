package com.example.moogerscouncil;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

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

@RunWith(AndroidJUnit4.class)
@LargeTest
public class FeedbackFlowTest {

    @Before
    public void setUp() throws Exception {
        Tasks.await(FirebaseAuth.getInstance()
                .signInWithEmailAndPassword("1@lums.edu.pk", "123456"), 10, TimeUnit.SECONDS);
        Thread.sleep(500); // Guard against late redirects

        Appointment testAppt = new Appointment();
        testAppt.setId("test_feedback_appt");
        testAppt.setStudentId(FirebaseAuth.getInstance().getCurrentUser().getUid());
        testAppt.setCounselorId("test_counselor_id");
        testAppt.setStatus("COMPLETED");

        Tasks.await(FirebaseFirestore.getInstance().collection("appointments")
                .document("test_feedback_appt").set(testAppt), 10, TimeUnit.SECONDS);
    }

    @After
    public void tearDown() throws Exception {
        Tasks.await(FirebaseFirestore.getInstance().collection("appointments")
                .document("test_feedback_appt").delete(), 10, TimeUnit.SECONDS);
        FirebaseAuth.getInstance().signOut();
    }

    private Intent homeIntent() {
        return new Intent(ApplicationProvider.getApplicationContext(), StudentHomeActivity.class);
    }

    @Test
    public void testFeedbackDialogDisplaysRatingBar() throws InterruptedException {
        try (ActivityScenario<StudentHomeActivity> scenario = ActivityScenario.launch(homeIntent())) {
            Thread.sleep(2000); // Allow Firestore fetch to complete
            onView(withId(R.id.btnPostSessionFeedback)).perform(click());
            onView(withId(R.id.feedbackRating)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void testFeedbackDialogDisplaysCommentField() throws InterruptedException {
        try (ActivityScenario<StudentHomeActivity> scenario = ActivityScenario.launch(homeIntent())) {
            Thread.sleep(2000);
            onView(withId(R.id.btnPostSessionFeedback)).perform(click());
            onView(withId(R.id.feedbackComment)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void testSubmitWithZeroRatingShowsError() throws InterruptedException {
        try (ActivityScenario<StudentHomeActivity> scenario = ActivityScenario.launch(homeIntent())) {
            Thread.sleep(2000);
            onView(withId(R.id.btnPostSessionFeedback)).perform(click());
            onView(withId(R.id.btnSubmitFeedback)).perform(click());

            // Allow toast to appear/be seen, then verify dialog persists (implies no submission)
            Thread.sleep(1000);
            onView(withId(R.id.feedbackRating)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void testFeedbackPromptCardAppears() throws InterruptedException {
        try (ActivityScenario<StudentHomeActivity> scenario = ActivityScenario.launch(homeIntent())) {
            Thread.sleep(2000);
            onView(withId(R.id.cardFeedbackPrompt)).check(matches(isDisplayed()));
        }
    }
}
