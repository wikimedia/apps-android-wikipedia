package org.wikipedia.main.loggedinuser

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.base.BaseTest
import org.wikipedia.main.MainActivity
import org.wikipedia.robots.LoginRobot
import org.wikipedia.robots.SystemRobot
import org.wikipedia.robots.screenrobots.HomeScreenRobot
import org.wikipedia.robots.screenrobots.NotificationScreenRobot

@LargeTest
@RunWith(AndroidJUnit4::class)
class NotificationScreenTest : BaseTest<MainActivity>(
    activityClass = MainActivity::class.java,
    isInitialOnboardingEnabled = false
) {

    private val homeScreenRobot = HomeScreenRobot()
    private val loginRobot = LoginRobot()
    private val notificationScreenRobot = NotificationScreenRobot()
    private val systemRobot = SystemRobot()

    @Test
    fun startNotificationTest() {
        homeScreenRobot
            .navigateToMoreMenu()
            .clickLoginMenuItem()
        loginRobot
            .clickLoginButton()
            .setLoginUserNameFromBuildConfig()
            .setPasswordFromBuildConfig()
            .loginUser()
        systemRobot
            .clickAllowOnSystemDialog()
        homeScreenRobot
            .navigateToNotifications()
        notificationScreenRobot
            .clickSearchBar()
            .pressBack()
            .pressBack()
    }
}
