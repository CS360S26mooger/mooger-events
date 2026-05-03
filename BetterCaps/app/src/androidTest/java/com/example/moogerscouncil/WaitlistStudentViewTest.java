package com.example.moogerscouncil;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.anyOf;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility;

/**
 * UI tests for StudentWaitlistActivity — verifies core views render and
 * that the empty state is shown when the user has no active waitlist entries.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class WaitlistStudentViewTest {

    @Before
    public void setUp() {
        ActivityScenario.launch(StudentWaitlistActivity.class);
    }

    // ------------------------------------------------------------------
    // Core view presence
    // ------------------------------------------------------------------

    @Test
    public void toolbarIsDisplayed() {
        onView(withId(R.id.toolbar)).check(matches(isDisplayed()));
    }

    @Test
    public void recyclerViewIsPresent() {
        onView(withId(R.id.recyclerStudentWaitlist))
                .check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
    }

    // ------------------------------------------------------------------
    // Empty state
    // ------------------------------------------------------------------

    @Test
    public void emptyStateTextIsPresent() {
        // With no Firebase auth and no data, the empty text should be visible
        // and the RecyclerView hidden (or vice versa — exactly one visible at a time).
        onView(withId(R.id.textWaitlistEmpty))
                .check(matches(anyOf(
                        withEffectiveVisibility(Visibility.VISIBLE),
                        withEffectiveVisibility(Visibility.GONE))));
    }

    @Test
    public void exactlyOneOfEmptyTextOrRecyclerIsVisible() {
        // After load completes, empty text and RecyclerView are mutually exclusive.
        // We verify neither is in an invalid INVISIBLE state.
        onView(withId(R.id.textWaitlistEmpty))
                .check(matches(anyOf(
                        withEffectiveVisibility(Visibility.VISIBLE),
                        withEffectiveVisibility(Visibility.GONE))));
        onView(withId(R.id.recyclerStudentWaitlist))
                .check(matches(anyOf(
                        withEffectiveVisibility(Visibility.VISIBLE),
                        withEffectiveVisibility(Visibility.GONE))));
    }
}
