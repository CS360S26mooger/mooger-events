package com.example.moogerscouncil;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.firebase.auth.FirebaseAuth;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class RegistrationFlowTest {

    @Rule
    public ActivityScenarioRule<RegisterActivity> activityRule =
            new ActivityScenarioRule<>(RegisterActivity.class);

    @Before
    public void setUp() {
        FirebaseAuth.getInstance().signOut();
    }

    @Test
    public void testRegistrationFormDisplaysAllFields() {
        onView(withId(R.id.editTextName)).check(matches(isDisplayed()));
        onView(withId(R.id.editTextEmail)).check(matches(isDisplayed()));
        onView(withId(R.id.editTextPreferredName)).check(matches(isDisplayed()));
        onView(withId(R.id.editTextPronouns)).check(matches(isDisplayed()));
        onView(withId(R.id.editTextPassword)).check(matches(isDisplayed()));
        onView(withId(R.id.editTextConfirmPassword)).check(matches(isDisplayed()));
        onView(withId(R.id.buttonRegister)).check(matches(isDisplayed()));
    }

    @Test
    public void testInvalidEmailShowsError() {
        onView(withId(R.id.editTextName)).perform(typeText("Test User"), closeSoftKeyboard());
        onView(withId(R.id.editTextEmail)).perform(typeText("user@gmail.com"), closeSoftKeyboard());
        onView(withId(R.id.editTextPassword)).perform(typeText("password123"), closeSoftKeyboard());
        onView(withId(R.id.editTextConfirmPassword)).perform(typeText("password123"), closeSoftKeyboard());

        onView(withId(R.id.buttonRegister)).perform(click());
        onView(withId(R.id.layoutEmail)).check(matches(withErrorText("lums.edu.pk")));
    }

    @Test
    public void testPasswordMismatchShowsError() {
        onView(withId(R.id.editTextName)).perform(typeText("Test User"), closeSoftKeyboard());
        onView(withId(R.id.editTextEmail)).perform(typeText("test@lums.edu.pk"), closeSoftKeyboard());
        onView(withId(R.id.editTextPassword)).perform(typeText("abc123"), closeSoftKeyboard());
        onView(withId(R.id.editTextConfirmPassword)).perform(typeText("xyz789"), closeSoftKeyboard());

        onView(withId(R.id.buttonRegister)).perform(click());
        onView(withId(R.id.layoutConfirmPassword)).check(matches(withErrorText("do not match")));
    }

    @Test
    public void testShortPasswordShowsError() {
        onView(withId(R.id.editTextName)).perform(typeText("Test User"), closeSoftKeyboard());
        onView(withId(R.id.editTextEmail)).perform(typeText("test@lums.edu.pk"), closeSoftKeyboard());
        onView(withId(R.id.editTextPassword)).perform(typeText("abc"), closeSoftKeyboard());
        onView(withId(R.id.editTextConfirmPassword)).perform(typeText("abc"), closeSoftKeyboard());

        onView(withId(R.id.buttonRegister)).perform(click());
        onView(withId(R.id.layoutPassword)).check(matches(withErrorText("6 characters")));
    }

    @Test
    public void testEmptyNameShowsError() {
        onView(withId(R.id.editTextEmail)).perform(typeText("test@lums.edu.pk"), closeSoftKeyboard());
        onView(withId(R.id.editTextPassword)).perform(typeText("password123"), closeSoftKeyboard());
        onView(withId(R.id.editTextConfirmPassword)).perform(typeText("password123"), closeSoftKeyboard());

        onView(withId(R.id.buttonRegister)).perform(click());
        onView(withId(R.id.layoutName)).check(matches(withErrorText("required")));
    }

    private static org.hamcrest.Matcher<android.view.View> withErrorText(String substring) {
        return new org.hamcrest.TypeSafeMatcher<android.view.View>() {
            @Override
            public void describeTo(org.hamcrest.Description description) {
                description.appendText("with error text containing: " + substring);
            }

            @Override
            protected boolean matchesSafely(android.view.View view) {
                if (!(view instanceof com.google.android.material.textfield.TextInputLayout)) return false;
                CharSequence error = ((com.google.android.material.textfield.TextInputLayout) view).getError();
                return error != null && error.toString().toLowerCase().contains(substring.toLowerCase());
            }
        };
    }
}
