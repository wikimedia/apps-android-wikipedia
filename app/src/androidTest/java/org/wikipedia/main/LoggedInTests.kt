package org.wikipedia.main

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.hamcrest.Matchers.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.BuildConfig
import org.wikipedia.R
import org.wikipedia.TestUtil

@LargeTest
@RunWith(AndroidJUnit4::class)
class LoggedInTests {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun loggedInTest() {

        // Skip over onboarding screens
        onView(allOf(withId(R.id.fragment_onboarding_skip_button), isDisplayed()))
                .perform(click())

        TestUtil.delay(2)

        // Click the More menu
        onView(allOf(withId(R.id.nav_more_container), isDisplayed()))
                .perform(click())

        TestUtil.delay(1)

        // Click the Login menu item
        onView(allOf(withId(R.id.main_drawer_login_button), isDisplayed()))
                .perform(click())

        TestUtil.delay(2)

        // Click the login button
        onView(allOf(withId(R.id.create_account_login_button), withText("Log in"), isDisplayed()))
                .perform(click())

        TestUtil.delay(2)

        // Type in an incorrect username and password
        onView(allOf(TestUtil.withGrandparent(withId(R.id.login_username_text)), withClassName(`is`("org.wikipedia.views.PlainPasteEditText"))))
                .perform(replaceText(BuildConfig.TEST_LOGIN_USERNAME), closeSoftKeyboard())

        onView(allOf(TestUtil.withGrandparent(withId(R.id.login_password_input)), withClassName(`is`("org.wikipedia.views.PlainPasteEditText"))))
                .perform(replaceText(BuildConfig.TEST_LOGIN_PASSWORD), closeSoftKeyboard())

        // Click the login button
        onView(withId(R.id.login_button))
                .perform(scrollTo(), click())

        TestUtil.delay(5)

        // Verify that a snackbar appears (because the login failed.)
        onView(withId(R.id.snackbar_text))
                .check(matches(isDisplayed()))
    }
}
