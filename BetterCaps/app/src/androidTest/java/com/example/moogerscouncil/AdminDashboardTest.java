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

@RunWith(AndroidJUnit4.class)
@LargeTest
public class AdminDashboardTest {
    @Test
    public void dashboardCoreViewsAreDisplayed() {
        try (ActivityScenario<AdminDashboardActivity> scenario =
                     ActivityScenario.launch(AdminDashboardActivity.class)) {
            onView(withText(R.string.admin_dashboard_title)).check(matches(isDisplayed()));
            onView(withId(R.id.btnReminderSettings)).check(matches(isDisplayed()));
            onView(withId(R.id.btnAdminLogout)).check(matches(isDisplayed()));
        }
    }
}
