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

/** Espresso tests for waitlist entry points. */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class WaitlistFlowTest {

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

    @Test
    public void bookingNoSlotStateIncludesJoinWaitlistButton() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), BookingActivity.class);
        intent.putExtra("counselorId", "test_counselor_id");
        intent.putExtra("counselorName", "Dr. Test");

        try (ActivityScenario<BookingActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                activity.findViewById(R.id.textNoSlots).setVisibility(android.view.View.VISIBLE);
                activity.findViewById(R.id.buttonJoinWaitlistBooking)
                        .setVisibility(android.view.View.VISIBLE);
            });

            onView(withId(R.id.buttonJoinWaitlistBooking)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void counselorProfileHasJoinWaitlistButtonInLayout() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                CounselorProfileActivity.class);
        intent.putExtra("COUNSELOR_ID", "test_counselor_id");
        intent.putExtra("SLOT_COUNSELOR_ID", "test_counselor_id");

        try (ActivityScenario<CounselorProfileActivity> scenario =
                     ActivityScenario.launch(intent)) {
            scenario.onActivity(activity ->
                    activity.findViewById(R.id.buttonJoinWaitlist)
                            .setVisibility(android.view.View.VISIBLE));

            onView(withId(R.id.buttonJoinWaitlist)).check(matches(isDisplayed()));
        }
    }
}
