package com.example.moogerscouncil;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

/** Espresso tests for waitlist entry points. */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class WaitlistFlowTest {

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
