package com.example.moogerscouncil;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
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

/** UI tests for the crisis escalation dialog. */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class CrisisEscalationFlowTest {

    @Test
    public void crisisButtonOpensEscalationDialog() {
        try (ActivityScenario<StudentProfileActivity> scenario =
                     ActivityScenario.launch(makeIntent())) {
            onView(withId(R.id.buttonTriggerCrisis)).perform(click());
            onView(withText(R.string.crisis_escalation_title)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void severityAndActionOptionsAreVisible() {
        try (ActivityScenario<StudentProfileActivity> scenario =
                     ActivityScenario.launch(makeIntent())) {
            onView(withId(R.id.buttonTriggerCrisis)).perform(click());
            onView(withText(R.string.crisis_severity_moderate)).check(matches(isDisplayed()));
            onView(withText(R.string.crisis_action_safety_plan)).check(matches(isDisplayed()));
        }
    }

    private Intent makeIntent() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                StudentProfileActivity.class);
        intent.putExtra(StudentProfileActivity.EXTRA_STUDENT_ID, "test_student_id");
        intent.putExtra(StudentProfileActivity.EXTRA_APPOINTMENT_ID, "test_appointment_id");
        intent.putExtra(StudentProfileActivity.EXTRA_COUNSELOR_ID, "test_counselor_id");
        return intent;
    }
}
