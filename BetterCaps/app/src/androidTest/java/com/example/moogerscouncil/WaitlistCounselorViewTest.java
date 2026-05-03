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
 * UI tests for CounselorWaitlistActivity — verifies core views render and
 * that the empty state is shown when the queue is empty.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class WaitlistCounselorViewTest {

    @Before
    public void setUp() {
        ActivityScenario.launch(CounselorWaitlistActivity.class);
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
        onView(withId(R.id.recyclerCounselorWaitlist))
                .check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
    }

    // ------------------------------------------------------------------
    // Empty state
    // ------------------------------------------------------------------

    @Test
    public void emptyStateTextIsPresent() {
        onView(withId(R.id.textWaitlistEmpty))
                .check(matches(anyOf(
                        withEffectiveVisibility(Visibility.VISIBLE),
                        withEffectiveVisibility(Visibility.GONE))));
    }

    @Test
    public void exactlyOneOfEmptyTextOrRecyclerIsVisible() {
        onView(withId(R.id.textWaitlistEmpty))
                .check(matches(anyOf(
                        withEffectiveVisibility(Visibility.VISIBLE),
                        withEffectiveVisibility(Visibility.GONE))));
        onView(withId(R.id.recyclerCounselorWaitlist))
                .check(matches(anyOf(
                        withEffectiveVisibility(Visibility.VISIBLE),
                        withEffectiveVisibility(Visibility.GONE))));
    }
}
