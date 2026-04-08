/*
 * CounselorDirectoryTest.java
 * Role: Espresso UI tests for the counselor directory (US-23).
 *       Verifies that the directory screen renders correctly and that
 *       search and chip filter interactions produce the expected UI state.
 *
 * Part of the BetterCAPS counseling platform.
 */
package com.example.moogerscouncil;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

/**
 * Espresso instrumentation tests for {@link CounselorListActivity}.
 *
 * <p>These tests verify UI structure and interaction. They do not assert
 * specific Firestore data, since the directory content depends on the
 * live Firestore instance.</p>
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class CounselorDirectoryTest {

    /**
     * Launches CounselorListActivity and verifies that the core UI elements —
     * the search field, chip group, and RecyclerView — are displayed.
     */
    @Test
    public void testDirectoryScreenDisplaysCoreElements() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                CounselorListActivity.class);
        try (ActivityScenario<CounselorListActivity> scenario =
                     ActivityScenario.launch(intent)) {

            onView(withId(R.id.searchField))
                    .check(matches(isDisplayed()));

            onView(withId(R.id.chipGroupSpecializations))
                    .check(matches(isDisplayed()));

            onView(withId(R.id.counselorRecyclerView))
                    .check(matches(isDisplayed()));
        }
    }

    /**
     * Verifies that the language dropdown is present and visible.
     */
    @Test
    public void testLanguageDropdownIsDisplayed() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                CounselorListActivity.class);
        try (ActivityScenario<CounselorListActivity> scenario =
                     ActivityScenario.launch(intent)) {

            onView(withId(R.id.dropdownLanguage))
                    .check(matches(isDisplayed()));
        }
    }

    /**
     * Verifies that the gender dropdown is present and visible.
     */
    @Test
    public void testGenderDropdownIsDisplayed() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                CounselorListActivity.class);
        try (ActivityScenario<CounselorListActivity> scenario =
                     ActivityScenario.launch(intent)) {

            onView(withId(R.id.dropdownGender))
                    .check(matches(isDisplayed()));
        }
    }

    /**
     * Types a query that is unlikely to match any counselor name and verifies
     * that the empty-state message becomes visible.
     */
    @Test
    public void testSearchWithNoMatchShowsEmptyState() throws InterruptedException {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                CounselorListActivity.class);
        try (ActivityScenario<CounselorListActivity> scenario =
                     ActivityScenario.launch(intent)) {

            // Allow Firestore to load (or timeout gracefully)
            Thread.sleep(3000);

            onView(withId(R.id.searchField))
                    .perform(typeText("zzz_no_match_xyz"), closeSoftKeyboard());

            // Empty state should be visible after the filter runs
            onView(withId(R.id.textEmptyState))
                    .check(matches(isDisplayed()));
        }
    }

    /**
     * Clears the search field after typing and verifies the RecyclerView
     * returns to visibility (empty state hidden).
     */
    @Test
    public void testClearingSearchRestoresList() throws InterruptedException {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                CounselorListActivity.class);
        try (ActivityScenario<CounselorListActivity> scenario =
                     ActivityScenario.launch(intent)) {

            Thread.sleep(3000);

            // Type a no-match query then clear it
            onView(withId(R.id.searchField))
                    .perform(typeText("zzz_no_match_xyz"), closeSoftKeyboard());

            onView(withId(R.id.searchField))
                    .perform(clearText(), closeSoftKeyboard());

            // RecyclerView should be visible again
            onView(withId(R.id.counselorRecyclerView))
                    .check(matches(isDisplayed()));
        }
    }
}
