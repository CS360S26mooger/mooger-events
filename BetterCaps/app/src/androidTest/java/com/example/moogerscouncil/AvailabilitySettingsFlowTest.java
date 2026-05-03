package com.example.moogerscouncil;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

/** UI tests for the availability settings screen. */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class AvailabilitySettingsFlowTest {

    @Test
    public void settingsScreenShowsBufferOptions() {
        try (ActivityScenario<AvailabilitySettingsActivity> scenario =
                     ActivityScenario.launch(AvailabilitySettingsActivity.class)) {
            scenario.onActivity(activity -> activity.setContentView(R.layout.activity_availability_settings));
            onView(withText(R.string.availability_settings_title)).check(matches(isDisplayed()));
            onView(withId(R.id.radioBuffer15)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void saveButtonIsVisible() {
        try (ActivityScenario<AvailabilitySettingsActivity> scenario =
                     ActivityScenario.launch(AvailabilitySettingsActivity.class)) {
            scenario.onActivity(activity -> activity.setContentView(R.layout.activity_availability_settings));
            onView(withId(R.id.buttonSaveAvailabilitySettings)).check(matches(isDisplayed()));
        }
    }
}
