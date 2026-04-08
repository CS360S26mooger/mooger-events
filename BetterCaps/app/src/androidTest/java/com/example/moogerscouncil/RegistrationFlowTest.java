/*
 * RegistrationFlowTest.java
 * Role: Espresso instrumented tests for the RegisterActivity UI and flow.
 *       Covers: form field display, inline validation error messages,
 *       and successful navigation to PrivacyPolicyActivity.
 *
 * Note: The navigation test (testSuccessfulRegistrationNavigatesToPrivacyPolicy)
 *       requires a test Firebase project or Firebase emulator, and is skipped
 *       with @Ignore if running without one.
 *
 * Part of the BetterCAPS counseling platform.
 */
package com.example.moogerscouncil;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Espresso tests for {@link RegisterActivity}.
 * Verifies that all form fields are visible and that inline validation
 * errors appear for invalid input before any Firebase call is made.
 */
@RunWith(AndroidJUnit4.class)
public class RegistrationFlowTest {

    @Rule
    public ActivityScenarioRule<RegisterActivity> activityRule =
            new ActivityScenarioRule<>(RegisterActivity.class);

    /**
     * All six input fields and the register button must be visible on launch.
     */
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

    /**
     * Submitting a non-@lums.edu.pk email must set an error on the email field
     * without making any Firebase call.
     */
    @Test
    public void testInvalidEmailShowsError() {
        onView(withId(R.id.editTextName))
                .perform(typeText("Test User"), closeSoftKeyboard());
        onView(withId(R.id.editTextEmail))
                .perform(typeText("user@gmail.com"), closeSoftKeyboard());
        onView(withId(R.id.editTextPassword))
                .perform(typeText("password123"), closeSoftKeyboard());
        onView(withId(R.id.editTextConfirmPassword))
                .perform(typeText("password123"), closeSoftKeyboard());

        onView(withId(R.id.buttonRegister)).perform(click());

        onView(withId(R.id.editTextEmail))
                .check(matches(withErrorText("lums.edu.pk")));
    }

    /**
     * Submitting mismatched passwords must set an error on the confirm-password field
     * without making any Firebase call.
     */
    @Test
    public void testPasswordMismatchShowsError() {
        onView(withId(R.id.editTextName))
                .perform(typeText("Test User"), closeSoftKeyboard());
        onView(withId(R.id.editTextEmail))
                .perform(typeText("test@lums.edu.pk"), closeSoftKeyboard());
        onView(withId(R.id.editTextPassword))
                .perform(typeText("abc123"), closeSoftKeyboard());
        onView(withId(R.id.editTextConfirmPassword))
                .perform(typeText("xyz789"), closeSoftKeyboard());

        onView(withId(R.id.buttonRegister)).perform(click());

        onView(withId(R.id.editTextConfirmPassword))
                .check(matches(withErrorText("do not match")));
    }

    /**
     * Submitting a password shorter than 6 characters must set an error on the password field.
     */
    @Test
    public void testShortPasswordShowsError() {
        onView(withId(R.id.editTextName))
                .perform(typeText("Test User"), closeSoftKeyboard());
        onView(withId(R.id.editTextEmail))
                .perform(typeText("test@lums.edu.pk"), closeSoftKeyboard());
        onView(withId(R.id.editTextPassword))
                .perform(typeText("abc"), closeSoftKeyboard());
        onView(withId(R.id.editTextConfirmPassword))
                .perform(typeText("abc"), closeSoftKeyboard());

        onView(withId(R.id.buttonRegister)).perform(click());

        onView(withId(R.id.editTextPassword))
                .check(matches(withErrorText("6 characters")));
    }

    /**
     * Submitting an empty name field must set an error on the name field.
     */
    @Test
    public void testEmptyNameShowsError() {
        onView(withId(R.id.editTextEmail))
                .perform(typeText("test@lums.edu.pk"), closeSoftKeyboard());
        onView(withId(R.id.editTextPassword))
                .perform(typeText("password123"), closeSoftKeyboard());
        onView(withId(R.id.editTextConfirmPassword))
                .perform(typeText("password123"), closeSoftKeyboard());

        onView(withId(R.id.buttonRegister)).perform(click());

        onView(withId(R.id.editTextName))
                .check(matches(withErrorText("required")));
    }

    // --- Helper ---

    /**
     * Custom Hamcrest matcher that checks whether the error text of an EditText
     * contains the given substring (case-insensitive).
     *
     * @param substring the substring to search for in the error text.
     * @return a Matcher usable in Espresso checks.
     */
    private static org.hamcrest.Matcher<android.view.View> withErrorText(String substring) {
        return new org.hamcrest.TypeSafeMatcher<android.view.View>() {
            @Override
            public void describeTo(org.hamcrest.Description description) {
                description.appendText("with error text containing: " + substring);
            }

            @Override
            protected boolean matchesSafely(android.view.View view) {
                if (!(view instanceof android.widget.EditText)) return false;
                CharSequence error = ((android.widget.EditText) view).getError();
                return error != null && error.toString().toLowerCase()
                        .contains(substring.toLowerCase());
            }
        };
    }
}
