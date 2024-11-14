package org.wikipedia.robots.feature

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers.allOf
import org.wikipedia.R
import org.wikipedia.TestUtil.childAtPosition
import org.wikipedia.base.BaseRobot
import org.wikipedia.base.TestConfig

class SettingsRobot : BaseRobot() {

    fun clickExploreFeedSettingItem() = apply {
        // Click on `Explore feed` option
        onView(
            allOf(
                withId(R.id.recycler_view),
            childAtPosition(withId(android.R.id.list_container), 0)
            )
        )
            .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(2, click()))

        delay(TestConfig.DELAY_MEDIUM)
    }

    fun openMoreOptionsToolbar() = apply {
        onView(allOf(
            withContentDescription("More options"),
            childAtPosition(childAtPosition(withId(R.id.toolbar), 2), 0), isDisplayed()
        ))
            .perform(click())
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun hideAllExploreFeeds() = apply {
        // Choose the option to hide all explore feed cards
        onView(allOf(withId(R.id.title), withText("Hide all"),
            childAtPosition(childAtPosition(withId(androidx.appcompat.R.id.content), 0), 0), isDisplayed()))
            .perform(click())
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun showAllExploreFeeds() = apply {
        onView(allOf(withId(R.id.title), withText("Show all"),
            childAtPosition(childAtPosition(withId(androidx.appcompat.R.id.content), 0), 0), isDisplayed()))
            .perform(click())
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun clickAboutWikipediaAppOptionItem() = apply {
        // Click `About the wikipedia app` option
        onView(allOf(withId(R.id.recycler_view), childAtPosition(withId(android.R.id.list_container), 0)))
            .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(14, click()))
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun activateDeveloperMode() = apply {
        // Click 7 times to activate developer mode
        for (i in 1 until 8) {
            onView(allOf(withId(R.id.about_logo_image),
                childAtPosition(childAtPosition(withId(R.id.about_container), 0), 0)))
                .perform(scrollTo(), click())
            delay(TestConfig.DELAY_MEDIUM)
        }
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun clickDeveloperMode() = apply {
        // Assert that developer mode is activated
        onView(allOf(withId(R.id.developer_settings), withContentDescription("Developer settings"),
            childAtPosition(childAtPosition(withId(androidx.appcompat.R.id.action_bar), 2), 0), isDisplayed()))
            .perform(click())
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun assertWeAreInDeveloperSettings() = apply {
        onView(allOf(withText("Developer settings"),
            withParent(allOf(withId(androidx.appcompat.R.id.action_bar),
                withParent(withId(androidx.appcompat.R.id.action_bar_container))
            )), isDisplayed()))
            .check(matches(withText("Developer settings")))
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun scrollToShowImagesOnSettings() = apply {
        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(
                RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>
                (hasDescendant(withText(R.string.preference_title_show_images)), click()))
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun pressBack() = apply {
        goBack()
        delay(TestConfig.DELAY_MEDIUM)
    }
}
