package com.example.moogerscouncil;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

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

/**
 * UI tests for {@link PastSessionsActivity}.
 */
@RunWith(AndroidJUnit4.class)
public class PastSessionsFlowTest {

    @Before
    public void setUp() throws Exception {
        // Sign in with counselor credentials to avoid redirects to LoginActivity
        Tasks.await(FirebaseAuth.getInstance()
                .signInWithEmailAndPassword("shaaahbaz@lums.edu.pk", "shahbaz"), 10, TimeUnit.SECONDS);
    }

    @After
    public void tearDown() {
        FirebaseAuth.getInstance().signOut();
    }

    @Test
    public void pastSessionsScreen_displaysBackButton() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                PastSessionsActivity.class);
        try (ActivityScenario<PastSessionsActivity> scenario =
                     ActivityScenario.launch(intent)) {
            onView(withId(R.id.buttonBack)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void pastSessionsScreen_displaysRecyclerView() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                PastSessionsActivity.class);
        try (ActivityScenario<PastSessionsActivity> scenario =
                     ActivityScenario.launch(intent)) {
            onView(withId(R.id.recyclerPastSessions)).check(matches(isDisplayed()));
        }
    }
}
