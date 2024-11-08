package org.wikipedia.robots.screenrobots

import android.app.Activity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import org.hamcrest.Matchers.allOf
import org.wikipedia.R
import org.wikipedia.TestUtil
import org.wikipedia.TestUtil.childAtPosition
import org.wikipedia.base.BaseRobot
import org.wikipedia.base.TestConfig

class HomeScreenRobot : BaseRobot() {

    fun navigateToExploreFeed() = apply {
        onView(allOf(withId(R.id.nav_tab_explore), withContentDescription("Explore"),
            childAtPosition(childAtPosition(withId(R.id.main_nav_tab_layout), 0), 0), isDisplayed())).perform(click())
        delay(TestConfig.DELAY_LARGE)
    }

    fun navigateToSavedPage() = apply {
        // Access the other navigation tabs - `Saved`, `Search` and `Edits`
        onView(
            allOf(
                withId(R.id.nav_tab_reading_lists), withContentDescription("Saved"),
            childAtPosition(childAtPosition(withId(R.id.main_nav_tab_layout), 0), 1), isDisplayed()
            )
        ).perform(click())
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun navigateToSearchPage() = apply {
        onView(allOf(withId(R.id.nav_tab_search), withContentDescription("Search"),
            childAtPosition(childAtPosition(withId(R.id.main_nav_tab_layout), 0), 2), isDisplayed())).perform(click())
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun navigateToEdits() = apply {
        onView(allOf(withId(R.id.nav_tab_edits), withContentDescription("Edits"),
            childAtPosition(childAtPosition(withId(R.id.main_nav_tab_layout), 0), 3), isDisplayed())).perform(click())
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun navigateToNotifications() = apply {
        clickOnDisplayedViewWithIdAnContentDescription(viewId = R.id.menu_notifications, "Notifications")
        delay(TestConfig.DELAY_LARGE)
    }

    fun navigateToMoreMenu() = apply {
        onView(allOf(withId(R.id.nav_tab_more), withContentDescription("More"), isDisplayed())).perform(click())
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun goToSettings() = apply {
        // Click on `Settings` option
        onView(allOf(withId(R.id.main_drawer_settings_container), isDisplayed())).perform(click())
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun pressBack() = apply {
        goBack()
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun assertAllFeedCardsAreHidden() = apply {
        onView(allOf(withId(R.id.empty_container), withParent(withParent(withId(R.id.swipe_refresh_layout))), isDisplayed()))
            .check(matches(isDisplayed()))
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun assertEmptyMessageIsNotVisible() = apply {
        // Ensure that empty message is not shown on explore feed
        onView(allOf(withId(R.id.empty_container), withParent(withParent(withId(R.id.swipe_refresh_layout))),
            TestUtil.isNotVisible())).check(matches(TestUtil.isNotVisible()))
    }

    fun imagesDoesNotShow() = apply {
        // Assert that images arent shown anymore
        onView(allOf(withId(R.id.articleImage), withParent(allOf(withId(R.id.articleImageContainer),
            withParent(withId(R.id.view_wiki_article_card)))), isDisplayed())).check(ViewAssertions.doesNotExist())
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun dismissTooltip(activity: Activity) = apply {
        dismissTooltipIfAny(activity, viewId = R.id.buttonView)
        delay(TestConfig.DELAY_SHORT)
    }

    fun clickLoginMenuItem() = apply {
        clickOnViewWithId(R.id.main_drawer_login_button)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun dismissFeedCustomization() = apply {
        clicksOnDisplayedViewWithText(R.id.view_announcement_action_negative, "Got it")
        delay(TestConfig.DELAY_SHORT)
    }

    fun gotoWatchList() = apply {
        clickOnViewWithId(R.id.main_drawer_watchlist_container)
        delay(TestConfig.DELAY_SHORT)
    }
}
