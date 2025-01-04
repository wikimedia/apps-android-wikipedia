package org.wikipedia.robots.navigation

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.hamcrest.Matchers.allOf
import org.wikipedia.R
import org.wikipedia.TestUtil.childAtPosition
import org.wikipedia.base.BaseRobot
import org.wikipedia.base.TestConfig

class BottomNavRobot : BaseRobot() {
    fun navigateToExploreFeed() = apply {
        onView(
            allOf(
                withId(R.id.nav_tab_explore), withContentDescription("Explore"),
            childAtPosition(childAtPosition(withId(R.id.main_nav_tab_layout), 0), 0), isDisplayed()
            )
        ).perform(click())
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
        onView(
            allOf(
                withId(R.id.nav_tab_search), withContentDescription("Search"),
            childAtPosition(childAtPosition(withId(R.id.main_nav_tab_layout), 0), 2), isDisplayed()
            )
        ).perform(click())
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun navigateToSuggestedEdits() = apply {
        onView(
            allOf(
                withId(R.id.nav_tab_edits), withContentDescription(R.string.nav_item_suggested_edits),
            childAtPosition(childAtPosition(withId(R.id.main_nav_tab_layout), 0), 3), isDisplayed()
            )
        ).perform(click())
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

    fun clickLoginMenuItem() = apply {
        clickOnViewWithId(R.id.main_drawer_login_button)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun gotoWatchList() = apply {
        clickOnViewWithId(R.id.main_drawer_watchlist_container)
        delay(TestConfig.DELAY_SHORT)
    }

    fun pressBack() = apply {
        goBack()
        delay(TestConfig.DELAY_SHORT)
    }
}
