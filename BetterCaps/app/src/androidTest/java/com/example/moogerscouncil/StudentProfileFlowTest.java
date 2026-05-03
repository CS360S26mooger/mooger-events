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
import static androidx.test.espresso.matcher.ViewMatchers.withText;

/** UI tests for the counselor-facing student profile screen. */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class StudentProfileFlowTest {

    @Test
    public void studentProfileScreenShowsProfileSection() {
        try (ActivityScenario<StudentProfileActivity> scenario =
                     ActivityScenario.launch(makeIntent())) {
            onView(withText(R.string.student_profile_title)).check(matches(isDisplayed()));
            onView(withId(R.id.textStudentProfileName)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void intakeSectionDisplaysGracefully() {
        try (ActivityScenario<StudentProfileActivity> scenario =
                     ActivityScenario.launch(makeIntent())) {
            scenario.onActivity(activity ->
                    activity.findViewById(R.id.textIntakeEmpty)
                            .setVisibility(android.view.View.VISIBLE));
            onView(withText(R.string.latest_intake_title)).check(matches(isDisplayed()));
            onView(withId(R.id.textIntakeEmpty)).check(matches(isDisplayed()));
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
