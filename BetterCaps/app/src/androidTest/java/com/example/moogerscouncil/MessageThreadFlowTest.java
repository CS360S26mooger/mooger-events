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

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MessageThreadFlowTest {
    @Test
    public void messageThreadShowsInputAndSendButton() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                MessageThreadActivity.class);
        intent.putExtra(MessageThreadActivity.EXTRA_APPOINTMENT_ID, "test-appointment");
        intent.putExtra(MessageThreadActivity.EXTRA_STUDENT_ID, "student");
        intent.putExtra(MessageThreadActivity.EXTRA_COUNSELOR_ID, "counselor");

        try (ActivityScenario<MessageThreadActivity> scenario = ActivityScenario.launch(intent)) {
            onView(withText(R.string.message_thread_title)).check(matches(isDisplayed()));
            onView(withId(R.id.editMessage)).check(matches(isDisplayed()));
            onView(withId(R.id.btnSendMessage)).check(matches(isDisplayed()));
        }
    }
}
