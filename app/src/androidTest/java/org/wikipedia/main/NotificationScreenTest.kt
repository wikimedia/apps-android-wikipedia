package org.wikipedia.main

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.base.BaseTest
import org.wikipedia.notifications.NotificationActivity
import org.wikipedia.robots.screenrobots.NotificationScreenRobot

@LargeTest
@RunWith(AndroidJUnit4::class)
class NotificationScreenTest : BaseTest<NotificationActivity>(
    activityClass = NotificationActivity::class.java,
    isInitialOnboardingEnabled = false
) {

    private val notificationScreenRobot = NotificationScreenRobot()

    @Test
    fun startNotificationTest() {
        notificationScreenRobot
            .clickSearchBar()
    }
}
