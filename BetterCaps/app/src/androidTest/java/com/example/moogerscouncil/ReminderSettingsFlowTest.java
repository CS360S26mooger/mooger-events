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

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ReminderSettingsFlowTest {
    @Test
    public void settingsScreenShowsReminderControls() {
        try (ActivityScenario<ReminderSettingsActivity> scenario =
                     ActivityScenario.launch(ReminderSettingsActivity.class)) {
            onView(withId(R.id.switch24HourReminder)).check(matches(isDisplayed()));
            onView(withId(R.id.switch1HourReminder)).check(matches(isDisplayed()));
            onView(withId(R.id.editMessage24Hour)).check(matches(isDisplayed()));
            onView(withId(R.id.editMessage1Hour)).check(matches(isDisplayed()));
            onView(withId(R.id.btnSaveReminderSettings)).check(matches(isDisplayed()));
        }
    }
}
