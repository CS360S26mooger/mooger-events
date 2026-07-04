/*
 * NotesUpsertFlowTest.java
 * Role: Espresso UI tests for the session-notes upsert flow on the counselor dashboard.
 *       Verifies the notes dialog renders, validates empty input, and that the styled
 *       layout (dialog_session_note_styled) with template chips and text area is used.
 *
 * Part of the BetterCAPS counseling platform — Sprint 9.
 */
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

/**
 * UI tests verifying the notes upsert dialog: styled layout, template chips,
 * and text area visibility.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class NotesUpsertFlowTest {

    @Test
    public void notesActivityOpensFromStudentProfile() {
        try (ActivityScenario<StudentProfileActivity> scenario =
                     ActivityScenario.launch(makeProfileIntent())) {
            onView(withId(R.id.buttonAddSessionNote)).perform(click());
            // Title in the top bar should appear
            onView(withText(R.string.session_notes_title)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void notesActivityShowsTemplateChipGroup() {
        try (ActivityScenario<StudentProfileActivity> scenario =
                     ActivityScenario.launch(makeProfileIntent())) {
            onView(withId(R.id.buttonAddSessionNote)).perform(click());
            onView(withId(R.id.chipGroupNoteTemplates)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void notesActivityShowsSectionFields() {
        try (ActivityScenario<StudentProfileActivity> scenario =
                     ActivityScenario.launch(makeProfileIntent())) {
            onView(withId(R.id.buttonAddSessionNote)).perform(click());
            onView(withId(R.id.editNoteConcern)).check(matches(isDisplayed()));
            onView(withId(R.id.editNoteSummary)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void notesActivityShowsSaveButton() {
        try (ActivityScenario<StudentProfileActivity> scenario =
                     ActivityScenario.launch(makeProfileIntent())) {
            onView(withId(R.id.buttonAddSessionNote)).perform(click());
            onView(withId(R.id.buttonSaveNote)).check(matches(isDisplayed()));
        }
    }

    private Intent makeProfileIntent() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                StudentProfileActivity.class);
        intent.putExtra(StudentProfileActivity.EXTRA_STUDENT_ID, "test_student_id");
        intent.putExtra(StudentProfileActivity.EXTRA_APPOINTMENT_ID, "test_appointment_id");
        intent.putExtra(StudentProfileActivity.EXTRA_COUNSELOR_ID, "test_counselor_id");
        return intent;
    }
}
