package org.wikipedia.robots.screen

import BaseRobot
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.wikipedia.R
import org.wikipedia.base.TestConfig

class HomeScreenRobot : BaseRobot() {

    fun clickSearchContainer() = apply {
        // Click the Search box
        click.onDisplayedView(R.id.search_container)
        delay(TestConfig.DELAY_SHORT)
    }

    fun navigateToNotifications() = apply {
        click.onDisplayedViewWithIdAndContentDescription(viewId = R.id.menu_notifications, "Notifications")
        delay(TestConfig.DELAY_LARGE)
    }

    fun pressBack() = apply {
        goBack()
        delay(TestConfig.DELAY_SHORT)
    }

    fun verifyIfSnackBarAppears() = apply {
        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(isDisplayed()))
        delay(TestConfig.DELAY_SHORT)
    }
}
