package org.wikipedia.main

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.allOf
import org.hamcrest.TypeSafeMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.R
import java.util.concurrent.TimeUnit

@LargeTest
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityTestRule(MainActivity::class.java)

    @Test
    fun mainActivityTest() {

        IdlingPolicies.setMasterPolicyTimeout(20, TimeUnit.SECONDS)

        onView(allOf(withId(R.id.fragment_onboarding_forward_button), isDisplayed()))
                .perform(click())

        delay(1)

        onView(allOf(withId(R.id.fragment_onboarding_forward_button), isDisplayed()))
                .perform(click())

        delay(1)

        onView(allOf(withId(R.id.fragment_onboarding_forward_button), isDisplayed()))
                .perform(click())

        delay(1)

        onView(allOf(withId(R.id.switchView), withText("Send usage data"), isDisplayed()))
                .check(matches(isDisplayed()))

        onView(allOf(withId(R.id.fragment_onboarding_done_button), withText("Get started"), isDisplayed()))
                .perform(click())

        delay(3)

        onView(allOf(withId(R.id.view_announcement_action_negative), withText("Got it"), isDisplayed()))
                .perform(click())

        delay(1)

        onView(allOf(withId(R.id.search_container), isDisplayed()))
                .perform(click())

        delay(2)

        onView(allOf(withId(R.id.search_src_text), isDisplayed()))
                .perform(replaceText("barack obama"), closeSoftKeyboard())

        delay(5)

        onView(allOf(withId(R.id.page_list_item_title), withText("Barack Obama"), isDisplayed()))
                .check(matches(withText("Barack Obama")))

        onView(withId(R.id.search_results_list))
                .perform(actionOnItemAtPosition<ViewHolder>(0, click()))

        delay(5)

        onView(allOf(withId(R.id.page_toolbar_button_tabs), isDisplayed()))
                .perform(click())

        delay(2)
    }

    @Test
    fun mainActivityTest2() {

        // Skip over onboarding screens
        onView(allOf(withId(R.id.fragment_onboarding_skip_button), isDisplayed()))
                .perform(click())

        delay(2)

        // Click the More menu
        onView(allOf(withId(R.id.nav_more_container), isDisplayed()))
                .perform(click())

        delay(1)

        // Click the Login menu item
        onView(allOf(withId(R.id.main_drawer_login_button), isDisplayed()))
                .perform(click())

        delay(2)

        // Click the login button
        onView(allOf(withId(R.id.create_account_login_button), withText("Log in"), isDisplayed()))
                .perform(click())

        delay(2)

        // Type in an incorrect username and password
        onView(allOf(withGrandparent(withId(R.id.login_username_text)), withClassName(`is`("org.wikipedia.views.PlainPasteEditText"))))
                .perform(replaceText("Foo"), closeSoftKeyboard())

        onView(allOf(withGrandparent(withId(R.id.login_password_input)), withClassName(`is`("org.wikipedia.views.PlainPasteEditText"))))
                .perform(replaceText("Bar"), closeSoftKeyboard())

        // Click the login button
        onView(withId(R.id.login_button))
                .perform(scrollTo(), click())

        delay(3)

        // Verify that a snackbar appears (because the login failed.)
        onView(withId(R.id.snackbar_text))
                .check(matches(isDisplayed()))
    }


    private fun childAtPosition(parentMatcher: Matcher<View>, position: Int): Matcher<View> {
        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("Child at position $position in parent ")
                parentMatcher.describeTo(description)
            }

            public override fun matchesSafely(view: View): Boolean {
                val parent = view.parent
                return parent is ViewGroup && parentMatcher.matches(parent)
                        && view == parent.getChildAt(position)
            }
        }
    }

    fun nthChildOf(parentMatcher: Matcher<View>, childPosition: Int): Matcher<View> {
        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("position $childPosition of parent ")
                parentMatcher.describeTo(description)
            }

            override fun matchesSafely(view: View): Boolean {
                if (view.parent !is ViewGroup) return false
                val parent = view.parent as ViewGroup
                return (parentMatcher.matches(parent)
                        && parent.childCount > childPosition && parent.getChildAt(childPosition) == view)
            }
        }
    }

    fun withGrandparent(grandparentMatcher: Matcher<View>): Matcher<View> {
        return WithGrandparentMatcher(grandparentMatcher)
    }

    internal class WithGrandparentMatcher constructor(private val grandparentMatcher: Matcher<View>) : TypeSafeMatcher<View>() {
        override fun describeTo(description: Description) {
            description.appendText("has grandparent matching: ")
            grandparentMatcher.describeTo(description)
        }

        public override fun matchesSafely(view: View): Boolean {
            return grandparentMatcher.matches(view.parent.parent)
        }
    }

    private fun withPositionInParent(parentViewId: Int, position: Int): Matcher<View> {
        return allOf(withParent(withId(parentViewId)), withParentIndex(position))
    }

    private fun delay(sec: Long) {
        onView(isRoot()).perform(waitOnId(TimeUnit.SECONDS.toMillis(sec)))
    }

    companion object {
        fun waitOnId(millis: Long): ViewAction {
            return object : ViewAction {
                override fun getConstraints(): Matcher<View> {
                    return isRoot()
                }

                override fun getDescription(): String {
                    return "Wait a specified amount of time."
                }

                override fun perform(uiController: UiController?, view: View?) {
                    uiController?.loopMainThreadForAtLeast(millis);
                }
            }
        }
    }
}
