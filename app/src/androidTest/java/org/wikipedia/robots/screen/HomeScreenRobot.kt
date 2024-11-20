package org.wikipedia.robots.screen

import android.app.Activity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import org.hamcrest.Matchers.allOf
import org.wikipedia.R
import org.wikipedia.TestUtil
import org.wikipedia.base.BaseRobot
import org.wikipedia.base.TestConfig

class HomeScreenRobot : BaseRobot() {

    fun clickSearchContainer() = apply {
        // Click the Search box
        clickOnDisplayedView(R.id.search_container)
        delay(TestConfig.DELAY_SHORT)
    }

    fun navigateToNotifications() = apply {
        clickOnDisplayedViewWithIdAnContentDescription(viewId = R.id.menu_notifications, "Notifications")
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
        dismissTooltipIfAny(activity, viewId = R.id.buttonView)
        delay(TestConfig.DELAY_SHORT)
    }

    fun dismissFeedCustomization() = apply {
        clicksOnDisplayedViewWithText(R.id.view_announcement_action_negative, "Got it")
        delay(TestConfig.DELAY_SHORT)
    }
}
