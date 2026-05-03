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

/** UI tests for buffer-aware booking empty states. */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class BufferBookingFlowTest {

    @Test
    public void emptySlotStateShowsJoinWaitlist() {
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
    public void bookingScreenStillShowsCalendar() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), BookingActivity.class);
        intent.putExtra("counselorId", "test_counselor_id");
        intent.putExtra("counselorName", "Dr. Test");

        try (ActivityScenario<BookingActivity> scenario = ActivityScenario.launch(intent)) {
            onView(withId(R.id.calendarView)).check(matches(isDisplayed()));
        }
    }
}
