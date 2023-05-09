package org.wikipedia.main

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItem
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.hamcrest.Matchers
import org.hamcrest.Matchers.allOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.R
import org.wikipedia.TestUtil
import org.wikipedia.TestUtil.childAtPosition

@LargeTest
@RunWith(AndroidJUnit4::class)
class ExploreFeedTests {

    private val cardNames = listOf("Top read", "Picture of the day", "In the news", "On this day", "Random article", "Today on Wikipedia")

    @Rule
    @JvmField
    var mActivityTestRule = ActivityScenarioRule(MainActivity::class.java)
    private lateinit var activity: MainActivity

    @Before
    fun setActivity() {
        mActivityTestRule.scenario.onActivity {
            activity = it
        }
    }
    @Test
    fun exploreFeedTest() {

        // Skip the initial onboarding screens...
        onView(allOf(withId(R.id.fragment_onboarding_skip_button), isDisplayed()))
            .perform(click())

        TestUtil.delay(2)

/*
        cardNames.forEach{
            onView(withId(R.id.feed_view))
                .perform(
                    actionOnItem<RecyclerView.ViewHolder>(
                        hasDescendant(withText(it)),
                        scrollTo()
                    ), click()
                )
            TestUtil.delay(4)
            pressBack()
            TestUtil.delay(4)

        }
*/

        // Featured article card
        onView(allOf(withId(R.id.view_featured_article_card_content_container), isDisplayed()))
            .perform(scrollTo(), click())

        TestUtil.delay(2)

        pressBack()

        TestUtil.delay(2)

        // Top read card
        onView(allOf(withId(R.id.feed_view), isNotFocused())).perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(4))

        TestUtil.delay(1)

        onView(allOf(withId(R.id.view_list_card_list_container), isDisplayed())).perform(scrollTo(), click())

        TestUtil.delay(2)

        pressBack()

        TestUtil.delay(1)

        // Picture of the day card
        onView(allOf(withId(R.id.feed_view), isNotFocused())).perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(5))

        TestUtil.delay(1)

        onView(allOf(withId(R.id.view_featured_image_card_image), isDisplayed())).perform(scrollTo(), click())

        pressBack()

        TestUtil.delay(1)

        // On this day card
        onView(allOf(withId(R.id.feed_view), isNotFocused())).perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(7))

        TestUtil.delay(1)

        onView(allOf(withId(R.id.event_layout), isDisplayed())).perform(scrollTo(), click())

        TestUtil.delay(2)

        pressBack()

        TestUtil.delay(1)

        // Random article card
        onView(allOf(withId(R.id.feed_view), isNotFocused())).perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(7))

        TestUtil.delay(1)

        onView(allOf(withId(R.id.view_featured_article_card_content_container), isDisplayed())).perform(scrollTo(), click())

        TestUtil.delay(2)

        pressBack()

        TestUtil.delay(1)

        // In the news card
        onView(withId(R.id.feed_view)).perform(actionOnItem<RecyclerView.ViewHolder>(hasDescendant(withText("In the news")), scrollTo()), click())

        TestUtil.delay(2)

        pressBack()

        TestUtil.delay(1)

        // Main page card
        onView(withId(R.id.feed_view)).perform(actionOnItem<RecyclerView.ViewHolder>(hasDescendant(withText("Today on Wikipedia")), scrollTo()), click())

        TestUtil.delay(2)

        pressBack()

        TestUtil.delay(2)

        onView(allOf(withId(R.id.nav_tab_reading_lists), withContentDescription("Saved"),
            childAtPosition(childAtPosition(withId(R.id.main_nav_tab_layout), 0), 1), isDisplayed())).perform(click())

        TestUtil.delay(2)

        onView(allOf(withId(R.id.nav_tab_search), withContentDescription("Search"),
             childAtPosition(childAtPosition(withId(R.id.main_nav_tab_layout), 0), 2), isDisplayed())).perform(click())

        TestUtil.delay(2)

        onView(allOf(withId(R.id.nav_tab_edits), withContentDescription("Edits"),
          childAtPosition(childAtPosition(withId(R.id.main_nav_tab_layout), 0), 3), isDisplayed())).perform(click())

        TestUtil.delay(2)

        onView(allOf(withId(R.id.nav_more_container), withContentDescription("More"),
            childAtPosition(allOf(withId(R.id.main_nav_tab_container), childAtPosition(withClassName(Matchers.`is`("android.widget.LinearLayout")), 1)), 1), isDisplayed())).perform(click())

        TestUtil.delay(2)
    }

    private fun goToTop() {
        onView(allOf(withId(R.id.feed_view))).perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(0))
        TestUtil.delay(2)
    }

    companion object {
    }
}
