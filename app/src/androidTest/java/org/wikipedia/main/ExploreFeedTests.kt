package org.wikipedia.main

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.contrib.RecyclerViewActions
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
import org.wikipedia.TestUtil.isNotVisible
import org.wikipedia.auth.AccountUtil

@LargeTest
@RunWith(AndroidJUnit4::class)
class ExploreFeedTests {

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

        // Dismiss the Feed customization onboarding card in the feed
        onView(allOf(withId(R.id.view_announcement_action_negative), withText("Got it"), isDisplayed()))
            .perform(click())

        TestUtil.delay(1)

        // Featured article card seen and saved to reading lists
        onView(allOf(withId(R.id.view_featured_article_card_content_container),
            childAtPosition(childAtPosition(withClassName(Matchers.`is`("org.wikipedia.feed.featured.FeaturedArticleCardView")), 0), 1), isDisplayed()))
        .perform(scrollTo(), longClick())

        onView(allOf(withId(R.id.title), withText("Save"),
            childAtPosition(childAtPosition(withId(androidx.appcompat.R.id.content), 0), 0), isDisplayed()))
        .perform(click())

        TestUtil.delay(2)

        onView(allOf(withId(R.id.feed_view), isNotFocused())).perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(3))
        TestUtil.delay(2)

        // Top read card seen and saved to reading lists
        onView(allOf(withId(R.id.view_list_card_list), childAtPosition(withId(R.id.view_list_card_list_container), 0)))
        .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, longClick()))

        onView(allOf(withId(R.id.title), withText("Save"),
            childAtPosition(childAtPosition(withId(androidx.appcompat.R.id.content), 0), 0), isDisplayed()))
        .perform(click())

        TestUtil.delay(2)

        onView(allOf(withId(R.id.feed_view), isNotFocused())).perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(4))

        TestUtil.delay(2)

        // Picture of the day card seen and clicked
        onView(allOf(withId(R.id.view_featured_image_card_content_container),
            childAtPosition(childAtPosition(withClassName(Matchers.`is`("org.wikipedia.feed.image.FeaturedImageCardView")), 0), 1), isDisplayed()))
        .perform(click())

        TestUtil.delay(2)

        pressBack()

        TestUtil.delay(2)

        onView(allOf(withId(R.id.feed_view), isNotFocused())).perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(5), click())

        TestUtil.delay(2)

        // News card seen and news item saved to reading lists
        onView(allOf(withId(R.id.news_story_items_recyclerview),
            childAtPosition(withClassName(Matchers.`is`("android.widget.LinearLayout")), 1)))
        .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, longClick()))

        onView(allOf(withId(R.id.title), withText("Save"),
            childAtPosition(childAtPosition(withId(androidx.appcompat.R.id.content), 0), 0), isDisplayed()))
        .perform(click())

        TestUtil.delay(2)

        pressBack()

        TestUtil.delay(2)

        onView(allOf(withId(R.id.feed_view), isNotFocused())).perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(6))

        TestUtil.delay(2)

        // On this day card seen and saved to reading lists
        onView(allOf(withId(R.id.on_this_day_page),
            childAtPosition(allOf(withId(R.id.event_layout), childAtPosition(withId(R.id.on_this_day_card_view_click_container), 0)), 3), isDisplayed()))
        .perform(longClick())

        onView(allOf(withId(R.id.title), withText("Save"),
            childAtPosition(childAtPosition(withId(androidx.appcompat.R.id.content), 0), 0), isDisplayed()))
        .perform(click())

        TestUtil.delay(2)

        goToTop()

        TestUtil.delay(2)

        onView(allOf(withId(R.id.feed_view), isDisplayed())).perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(7))

        TestUtil.delay(2)

        // Random article card seen and saved to reading lists
        onView(allOf(withId(R.id.view_featured_article_card_content_container),
            childAtPosition(childAtPosition(withClassName(Matchers.`is`("org.wikipedia.feed.random.RandomCardView")), 0), 1), isDisplayed()))
        .perform(scrollTo(), longClick())

        onView(allOf(withId(R.id.title), withText("Save"),
            childAtPosition(childAtPosition(withId(androidx.appcompat.R.id.content), 0), 0), isDisplayed()))
        .perform(click())

        TestUtil.delay(2)

        if (AccountUtil.isLoggedIn) {
            // Access Suggested edits card
            onView(allOf(withId(R.id.feed_view), isDisplayed())).perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(9))

            TestUtil.delay(2)

            onView(allOf(withId(R.id.callToActionButton), withText("Add article description"),
                childAtPosition(allOf(withId(R.id.viewArticleContainer), childAtPosition(withId(R.id.cardItemContainer), 1)), 6), isDisplayed()))
                .perform(click())

            TestUtil.delay(2)

            pressBack()

            TestUtil.delay(2)

            pressBack()

            TestUtil.delay(2)
        }

        onView(allOf(withId(R.id.feed_view), isDisplayed())).perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(8))

        TestUtil.delay(2)

        // Main page card seen clicked
        onView(allOf(withId(R.id.footerActionButton), withText("View main page  "),
            childAtPosition(allOf(withId(R.id.card_footer), childAtPosition(withClassName(Matchers.`is`("android.widget.LinearLayout")), 1)), 0), isDisplayed()))
        .perform(click())

        TestUtil.delay(2)

        pressBack()

        TestUtil.delay(2)

        // Access the other navigation tabs - `Saved`, `Search` and `Edits`
        onView(allOf(withId(R.id.nav_tab_reading_lists), withContentDescription("Saved"),
            childAtPosition(childAtPosition(withId(R.id.main_nav_tab_layout), 0), 1), isDisplayed())).perform(click())

        TestUtil.delay(2)

        onView(allOf(withId(R.id.nav_tab_search), withContentDescription("Search"),
             childAtPosition(childAtPosition(withId(R.id.main_nav_tab_layout), 0), 2), isDisplayed())).perform(click())

        TestUtil.delay(2)

        onView(allOf(withId(R.id.nav_tab_edits), withContentDescription("Edits"),
          childAtPosition(childAtPosition(withId(R.id.main_nav_tab_layout), 0), 3), isDisplayed())).perform(click())

        TestUtil.delay(2)

        if (AccountUtil.isLoggedIn) {
            // Click through `Edits` screen stats onboarding
            for (i in 1 until 5) {
                onView(allOf(withId(R.id.buttonView), withText("Got it"),
                    childAtPosition(childAtPosition(withClassName(Matchers.`is`("android.widget.LinearLayout")), 1), 0), isDisplayed()))
                    .perform(click())
                TestUtil.delay(2)
            }
        }

        // Click on `More` menu
        onView(allOf(withId(R.id.nav_more_container), withContentDescription("More"), isDisplayed())).perform(click())

        TestUtil.delay(2)

        // Click on `Settings` option
        onView(allOf(withId(R.id.main_drawer_settings_container), isDisplayed())).perform(click())

        TestUtil.delay(2)

        // Click on `Explore feed` option
        onView(allOf(withId(R.id.recycler_view),
            childAtPosition(withId(android.R.id.list_container), 0)))
        .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(2, click()))

        TestUtil.delay(2)

        onView(allOf(withContentDescription("More options"),
            childAtPosition(childAtPosition(withId(androidx.appcompat.R.id.action_bar), 2), 0), isDisplayed()))
        .perform(click())

        TestUtil.delay(2)

        onView(allOf(withId(R.id.title), withText("Hide all"),
            childAtPosition(childAtPosition(withId(androidx.appcompat.R.id.content), 0), 0), isDisplayed()))
        .perform(click())

        TestUtil.delay(2)

        pressBack()

        TestUtil.delay(2)

        pressBack()

        TestUtil.delay(2)

        onView(allOf(withId(R.id.nav_tab_explore), withContentDescription("Explore"),
            childAtPosition(childAtPosition(withId(R.id.main_nav_tab_layout), 0), 0), isDisplayed())).perform(click())

        TestUtil.delay(2)

        onView(allOf(withId(R.id.empty_container), withParent(withParent(withId(R.id.swipe_refresh_layout))), isDisplayed()))
        .check(ViewAssertions.matches(isDisplayed()))

        TestUtil.delay(2)

        // Click on `More` menu
        onView(allOf(withId(R.id.nav_more_container), withContentDescription("More"), isDisplayed())).perform(click())

        TestUtil.delay(2)

        // Click on `Settings` option
        onView(allOf(withId(R.id.main_drawer_settings_container), isDisplayed())).perform(click())

        TestUtil.delay(2)

        // Click on `Explore feed` option
        onView(allOf(withId(R.id.recycler_view),
            childAtPosition(withId(android.R.id.list_container), 0)))
        .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(2, click()))
        TestUtil.delay(2)

        onView(allOf(withContentDescription("More options"),
            childAtPosition(childAtPosition(withId(androidx.appcompat.R.id.action_bar), 2), 0), isDisplayed()))
        .perform(click())

        TestUtil.delay(2)

        onView(allOf(withId(R.id.title), withText("Show all"),
            childAtPosition(childAtPosition(withId(androidx.appcompat.R.id.content), 0), 0), isDisplayed()))
        .perform(click())

        TestUtil.delay(2)

        pressBack()

        TestUtil.delay(2)

        pressBack()

        TestUtil.delay(2)

        onView(allOf(withId(R.id.empty_container), withParent(withParent(withId(R.id.swipe_refresh_layout))), isNotVisible()))
        .check(ViewAssertions.matches(isNotVisible()))

        TestUtil.delay(2)

        // Test `Developer settings activation process via `Settings` screen
        onView(allOf(withId(R.id.nav_more_container), withContentDescription("More"), isDisplayed())).perform(click())

        TestUtil.delay(2)

        onView(allOf(withId(R.id.main_drawer_settings_container), isDisplayed())).perform(click())

        TestUtil.delay(2)

        onView(allOf(withId(R.id.recycler_view), childAtPosition(withId(android.R.id.list_container), 0)))
        .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(16, click()))

        TestUtil.delay(2)

       for (i in 1 until 8) {
           onView(allOf(withId(R.id.about_logo_image),
               childAtPosition(childAtPosition(withId(R.id.about_container), 0), 0)))
           .perform(scrollTo(), click())
           TestUtil.delay(2)
       }

        TestUtil.delay(2)

        pressBack()

        TestUtil.delay(2)

        onView(allOf(withId(R.id.developer_settings), withContentDescription("Developer settings"),
            childAtPosition(childAtPosition(withId(androidx.appcompat.R.id.action_bar), 2), 0), isDisplayed()))
        .perform(click())

        TestUtil.delay(2)

        onView(allOf(withText("Developer settings"),
            withParent(allOf(withId(androidx.appcompat.R.id.action_bar),
                withParent(withId(androidx.appcompat.R.id.action_bar_container)))), isDisplayed()))
        .check(ViewAssertions.matches(withText("Developer settings")))

        TestUtil.delay(2)
    }

    private fun goToTop() {
        onView(allOf(withId(R.id.feed_view))).perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(0))
        TestUtil.delay(2)
    }
}
