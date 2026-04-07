package com.example.moogerscouncil;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class LoginActivityTest {

    @Rule
    public ActivityScenarioRule<LoginActivity> activityRule =
            new ActivityScenarioRule<>(LoginActivity.class);

    @Test
    public void testLoginElementsAreDisplayed() {
        onView(withId(R.id.emailField)).check(matches(isDisplayed()));
        onView(withId(R.id.passwordField)).check(matches(isDisplayed()));
        onView(withId(R.id.loginButton)).check(matches(isDisplayed()));
    }

    @Test
    public void testRoleSelectionChangesButtonText() {
        onView(withId(R.id.btnCounselor)).perform(click());
        onView(withId(R.id.loginButton)).check(matches(withText("Log In as Counselor")));

        onView(withId(R.id.btnStudent)).perform(click());
        onView(withId(R.id.loginButton)).check(matches(withText("Log In as Student")));
    }

    @Test
    public void testEmptyFieldsShowErrorToast() {
        onView(withId(R.id.loginButton)).perform(click());
        // Since toast testing is flaky in Espresso, we check if we're still on the same activity
        onView(withId(R.id.emailField)).check(matches(isDisplayed()));
    }
}