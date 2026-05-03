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
import static org.hamcrest.Matchers.not;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class OnLeaveFlowTest {

    @Before
    public void setUp() throws Exception {
        Counselor c = new Counselor();
        c.setId("test_on_leave_counselor");
        c.setUid("test_on_leave_counselor");
        c.setName("Dr. OnLeave");
        c.setOnLeave(true);

        Tasks.await(FirebaseFirestore.getInstance().collection("counselors")
                .document("test_on_leave_counselor").set(c), 10, TimeUnit.SECONDS);
    }

    @After
    public void tearDown() throws Exception {
        Tasks.await(FirebaseFirestore.getInstance().collection("counselors")
                .document("test_on_leave_counselor").delete(), 10, TimeUnit.SECONDS);
        FirebaseAuth.getInstance().signOut();
    }

    private void signInCounselor() throws Exception {
        Tasks.await(FirebaseAuth.getInstance()
                .signInWithEmailAndPassword("shaaahbaz@lums.edu.pk", "shahbaz"), 10, TimeUnit.SECONDS);
    }

    private void signInStudent() throws Exception {
        Tasks.await(FirebaseAuth.getInstance()
                .signInWithEmailAndPassword("1@lums.edu.pk", "123456"), 10, TimeUnit.SECONDS);
    }

    private Intent editProfileIntent() {
        return new Intent(ApplicationProvider.getApplicationContext(),
                CounselorProfileEditActivity.class);
    }

    @Test
    public void testOnLeaveToggleIsDisplayed() throws Exception {
        signInCounselor();
        try (ActivityScenario<CounselorProfileEditActivity> scenario = ActivityScenario.launch(editProfileIntent())) {
            onView(withId(R.id.switchOnLeave)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void testToggleShowsMessageField() throws Exception {
        signInCounselor();
        try (ActivityScenario<CounselorProfileEditActivity> scenario = ActivityScenario.launch(editProfileIntent())) {
            onView(withId(R.id.layoutLeaveMessage)).check(matches(not(isDisplayed())));
            onView(withId(R.id.switchOnLeave)).perform(click());
            onView(withId(R.id.layoutLeaveMessage)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void testToggleShowsReferralDropdown() throws Exception {
        signInCounselor();
        try (ActivityScenario<CounselorProfileEditActivity> scenario = ActivityScenario.launch(editProfileIntent())) {
            onView(withId(R.id.layoutReferralCounselor)).check(matches(not(isDisplayed())));
            onView(withId(R.id.switchOnLeave)).perform(click());
            onView(withId(R.id.layoutReferralCounselor)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void testOnLeaveCounselorShowsBadgeInDirectory() throws Exception {
        signInStudent();
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), CounselorListActivity.class);
        try (ActivityScenario<CounselorListActivity> scenario = ActivityScenario.launch(intent)) {
            Thread.sleep(2000); // Give firestore time to load list
            onView(withText(R.string.currently_away)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void testOnLeaveCounselorProfileShowsLeaveCard() throws Exception {
        signInStudent();
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), CounselorProfileActivity.class);
        intent.putExtra("COUNSELOR_ID", "test_on_leave_counselor");

        try (ActivityScenario<CounselorProfileActivity> scenario = ActivityScenario.launch(intent)) {
            Thread.sleep(1000);
            onView(withId(R.id.cardOnLeave)).check(matches(isDisplayed()));
        }
    }
}
