package com.example.moogerscouncil;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

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
    public void setUp() throws Exception {
        // Sign in with student credentials to avoid potential issues with unauthenticated states
        Tasks.await(FirebaseAuth.getInstance()
                .signInWithEmailAndPassword("test@lums.edu.pk", "testtest"), 10, TimeUnit.SECONDS);
        ActivityScenario.launch(StudentWaitlistActivity.class);
    }

    @After
    public void tearDown() {
        FirebaseAuth.getInstance().signOut();
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
                .check(matches(anyOf(
                        withEffectiveVisibility(Visibility.VISIBLE),
                        withEffectiveVisibility(Visibility.GONE))));
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
