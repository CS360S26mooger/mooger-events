package com.example.moogerscouncil;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

/**
 * UI tests for {@link PastSessionsActivity}.
 */
@RunWith(AndroidJUnit4.class)
public class PastSessionsFlowTest {

    @Test
    public void pastSessionsScreen_displaysBackButton() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                PastSessionsActivity.class);
        try (ActivityScenario<PastSessionsActivity> scenario =
                     ActivityScenario.launch(intent)) {
            onView(withId(R.id.buttonBack)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void pastSessionsScreen_displaysRecyclerView() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                PastSessionsActivity.class);
        try (ActivityScenario<PastSessionsActivity> scenario =
                     ActivityScenario.launch(intent)) {
            onView(withId(R.id.recyclerPastSessions)).check(matches(isDisplayed()));
        }
    }
}
