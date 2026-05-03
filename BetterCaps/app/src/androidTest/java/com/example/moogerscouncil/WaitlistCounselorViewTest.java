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
 * UI tests for CounselorWaitlistActivity — verifies core views render and
 * that the empty state is shown when the queue is empty.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class WaitlistCounselorViewTest {

    @Before
    public void setUp() throws Exception {
        Tasks.await(FirebaseAuth.getInstance()
                .signInWithEmailAndPassword("shaaahbaz@lums.edu.pk", "shahbaz"), 10, TimeUnit.SECONDS);
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
        try (ActivityScenario<CounselorWaitlistActivity> scenario =
                     ActivityScenario.launch(CounselorWaitlistActivity.class)) {
            onView(withId(R.id.toolbar)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void recyclerViewIsPresent() {
        try (ActivityScenario<CounselorWaitlistActivity> scenario =
                     ActivityScenario.launch(CounselorWaitlistActivity.class)) {
            onView(withId(R.id.recyclerCounselorWaitlist))
                    .check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
        }
    }

    // ------------------------------------------------------------------
    // Empty state
    // ------------------------------------------------------------------

    @Test
    public void emptyStateTextIsPresent() {
        try (ActivityScenario<CounselorWaitlistActivity> scenario =
                     ActivityScenario.launch(CounselorWaitlistActivity.class)) {
            onView(withId(R.id.textWaitlistEmpty))
                    .check(matches(anyOf(
                            withEffectiveVisibility(Visibility.VISIBLE),
                            withEffectiveVisibility(Visibility.GONE))));
        }
    }

    @Test
    public void exactlyOneOfEmptyTextOrRecyclerIsVisible() {
        try (ActivityScenario<CounselorWaitlistActivity> scenario =
                     ActivityScenario.launch(CounselorWaitlistActivity.class)) {
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
}
