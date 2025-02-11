package org.wikipedia.robots.screen

import android.app.Activity
import android.util.Log
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import org.hamcrest.Matchers.allOf
import org.wikipedia.R
import org.wikipedia.TestUtil
import org.wikipedia.base.TestConfig
import org.wikipedia.base.base.BaseRobot

class HomeScreenRobot : BaseRobot() {

    fun clickSearchContainer() = apply {
        // Click the Search box
        click.onDisplayedView(R.id.search_container)
        delay(TestConfig.DELAY_SHORT)
    }

    fun navigateToNotifications() = apply {
        click.onDisplayedViewWithIdAnContentDescription(viewId = R.id.menu_notifications, "Notifications")
        delay(TestConfig.DELAY_LARGE)
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
        system.dismissTooltipIfAny(activity, viewId = R.id.buttonView)
        delay(TestConfig.DELAY_SHORT)
    }

    fun dismissFeedCustomization() = apply {
        try {
            click.onDisplayedViewWithText(R.id.view_announcement_action_negative, "Got it")
            delay(TestConfig.DELAY_SHORT)
        } catch (e: Exception) {
            Log.d("HomeScreenRobot", "no view because the device has no internet")
        }
    }

    fun verifyIfSnackBarAppears() = apply {
        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(isDisplayed()))
        delay(TestConfig.DELAY_SHORT)
    }
}
