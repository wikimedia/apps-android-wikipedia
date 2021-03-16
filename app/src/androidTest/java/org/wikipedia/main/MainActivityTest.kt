package org.wikipedia.main

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.allOf
import org.hamcrest.TypeSafeMatcher
import org.hamcrest.core.IsInstanceOf
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
        val appCompatImageView = onView(
                allOf(withId(R.id.fragment_onboarding_forward_button), withContentDescription("Continue"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(`is`("android.widget.FrameLayout")),
                                        2),
                                0),
                        isDisplayed()))
        appCompatImageView.perform(click())

        val appCompatImageView2 = onView(
                allOf(withId(R.id.fragment_onboarding_forward_button), withContentDescription("Continue"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(`is`("android.widget.FrameLayout")),
                                        2),
                                0),
                        isDisplayed()))
        appCompatImageView2.perform(click())

        val appCompatImageView3 = onView(
                allOf(withId(R.id.fragment_onboarding_forward_button), withContentDescription("Continue"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(`is`("android.widget.FrameLayout")),
                                        2),
                                0),
                        isDisplayed()))
        appCompatImageView3.perform(click())

        delay(2)

        val switch_ = onView(
                allOf(withId(R.id.switchView), withText("Send usage data"),
                        withParent(allOf(withId(R.id.switchContainer),
                                withParent(IsInstanceOf.instanceOf(android.widget.LinearLayout::class.java)))),
                        isDisplayed()))
        switch_.check(matches(isDisplayed()))

        val materialButton = onView(
                allOf(withId(R.id.fragment_onboarding_done_button), withText("Get started"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(`is`("android.widget.FrameLayout")),
                                        2),
                                1),
                        isDisplayed()))
        materialButton.perform(click())

        delay(3)

        val materialButton2 = onView(
                allOf(withId(R.id.view_announcement_action_negative), withText("Got it"),
                        childAtPosition(
                                allOf(withId(R.id.view_announcement_card_buttons_container),
                                        childAtPosition(
                                                withClassName(`is`("android.widget.LinearLayout")),
                                                2)),
                                1),
                        isDisplayed()))
        materialButton2.perform(click())

        val wikiCardView = onView(
                allOf(withId(R.id.search_container),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.fragment_feed_feed),
                                        0),
                                0),
                        isDisplayed()))
        wikiCardView.perform(click())

        val searchAutoComplete = onView(
                allOf(withId(R.id.search_src_text),
                        childAtPosition(
                                allOf(withId(R.id.search_plate),
                                        childAtPosition(
                                                withId(R.id.search_edit_frame),
                                                1)),
                                0),
                        isDisplayed()))
        searchAutoComplete.perform(replaceText("barack obama"), closeSoftKeyboard())

        delay(5)

        val textView = onView(
                allOf(withId(R.id.page_list_item_title), withText("Barack Obama"),
                        withParent(withParent(withId(R.id.search_results_list))),
                        isDisplayed()))
        textView.check(matches(withText("Barack Obama")))

        val recyclerView = onView(
                allOf(withId(R.id.search_results_list),
                        childAtPosition(
                                withId(R.id.search_results_container),
                                1)))
        recyclerView.perform(actionOnItemAtPosition<ViewHolder>(0, click()))

        delay(5)

        val tabCountsView = onView(
                allOf(withId(R.id.page_toolbar_button_tabs), withContentDescription("Show tabs"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.page_toolbar),
                                        0),
                                1),
                        isDisplayed()))
        tabCountsView.perform(click())

        delay(2)
    }

    private fun childAtPosition(
            parentMatcher: Matcher<View>, position: Int): Matcher<View> {

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


    private fun delay(sec: Long) {
        onView(isRoot()).perform(waitOnId(R.id.fragment_container, TimeUnit.SECONDS.toMillis(sec)))
    }

    companion object {
        fun waitOnId(id: Int, millis: Long): ViewAction {
            return object : ViewAction {
                override fun getConstraints(): Matcher<View> {
                    return isRoot()
                }

                override fun getDescription(): String {
                    return "Wait a specified amount of time on a certain View ID."
                }

                override fun perform(uiController: UiController?, view: View?) {
                    uiController?.loopMainThreadForAtLeast(millis);
                }
            }
        }
    }
}
