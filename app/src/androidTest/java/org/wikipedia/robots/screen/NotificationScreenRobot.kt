package org.wikipedia.robots.screen

import BaseRobot
import org.wikipedia.R
import org.wikipedia.base.TestConfig

class NotificationScreenRobot : BaseRobot() {

    fun clickSearchBar() = apply {
        list.clickRecyclerViewItemAtPosition(viewId = R.id.notifications_recycler_view, position = 0)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun pressBack() = apply {
        goBack()
        delay(TestConfig.DELAY_SHORT)
    }
}
