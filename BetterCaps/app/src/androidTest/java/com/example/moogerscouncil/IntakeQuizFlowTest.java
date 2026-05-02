package com.example.moogerscouncil;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

/** Espresso tests for the intake quiz UI shell. */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class IntakeQuizFlowTest {

    @Test
    public void quizQuestionsDisplayInOrder() {
        try (ActivityScenario<QuizActivity> scenario = ActivityScenario.launch(QuizActivity.class)) {
            onView(withText("What's been on your mind lately?"))
                    .check(matches(isDisplayed()));

            onView(withId(R.id.btnOption1)).perform(click());
            onView(withText("How long have you been dealing with this?"))
                    .check(matches(isDisplayed()));
        }
    }

    @Test
    public void resultScreenHasViewProfileButton() {
        try (ActivityScenario<QuizActivity> scenario = ActivityScenario.launch(QuizActivity.class)) {
            scenario.onActivity(activity -> {
                activity.findViewById(R.id.layoutQuestion).setVisibility(android.view.View.GONE);
                activity.findViewById(R.id.layoutResult).setVisibility(android.view.View.VISIBLE);
            });

            onView(withId(R.id.btnViewProfile)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void browseFallbackButtonIsDisplayed() {
        try (ActivityScenario<QuizActivity> scenario = ActivityScenario.launch(QuizActivity.class)) {
            scenario.onActivity(activity -> {
                activity.findViewById(R.id.layoutQuestion).setVisibility(android.view.View.GONE);
                activity.findViewById(R.id.layoutResult).setVisibility(android.view.View.VISIBLE);
            });

            onView(withId(R.id.btnBrowseAll)).check(matches(isDisplayed()));
        }
    }
}
