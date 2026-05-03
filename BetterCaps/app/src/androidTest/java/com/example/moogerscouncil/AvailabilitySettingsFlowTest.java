package com.example.moogerscouncil;

import androidx.test.core.app.ActivityScenario;
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

@RunWith(AndroidJUnit4.class)
@LargeTest
public class AvailabilitySettingsFlowTest {

    @Before
    public void setUp() throws Exception {
        Tasks.await(FirebaseAuth.getInstance()
                .signInWithEmailAndPassword("shaaahbaz@lums.edu.pk", "shahbaz"), 10, TimeUnit.SECONDS);
    }

    @After
    public void tearDown() {
        FirebaseAuth.getInstance().signOut();
    }

    @Test
    public void settingsScreenShowsBufferOptions() {
        try (ActivityScenario<AvailabilitySettingsActivity> scenario =
                     ActivityScenario.launch(AvailabilitySettingsActivity.class)) {
            onView(withText(R.string.availability_settings_title)).check(matches(isDisplayed()));
            onView(withId(R.id.radioBuffer15)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void saveButtonIsVisible() {
        try (ActivityScenario<AvailabilitySettingsActivity> scenario =
                     ActivityScenario.launch(AvailabilitySettingsActivity.class)) {
            onView(withId(R.id.buttonSaveAvailabilitySettings)).check(matches(isDisplayed()));
        }
    }
}
