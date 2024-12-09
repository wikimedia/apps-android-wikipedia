package org.wikipedia.tests.explorefeed

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.base.BaseTest
import org.wikipedia.main.MainActivity
import org.wikipedia.robots.SystemRobot
import org.wikipedia.robots.feature.ExploreFeedRobot
import org.wikipedia.robots.feature.LoginRobot
import org.wikipedia.robots.navigation.BottomNavRobot
import org.wikipedia.robots.screen.HomeScreenRobot

@LargeTest
@RunWith(AndroidJUnit4::class)
class FeedScreenSuggestedEditTest : BaseTest<MainActivity>(
 activityClass = MainActivity::class.java
) {
    private val bottomNavRobot = BottomNavRobot()
    private val loginRobot = LoginRobot()
    private val systemRobot = SystemRobot()
    private val homeScreenRobot = HomeScreenRobot()
    private val exploreFeedRobot = ExploreFeedRobot()

    @Test
    fun runTest() {
        // Following test requires login
        // 1. Notification click
        // 2. Suggested Edit Visibility
        systemRobot
            .clickOnSystemDialogWithText("Allow")

        // Logging user
        bottomNavRobot
            .navigateToMoreMenu()
            .clickLoginMenuItem()
        loginRobot
            .clickLoginButton()
            .setLoginUserNameFromBuildConfig()
            .setPasswordFromBuildConfig()
            .loginUser()
        // After log in, notification dialog appears
        systemRobot
            .clickOnSystemDialogWithText(text = "Allow")

        homeScreenRobot
            .navigateToNotifications()
            .pressBack()

        // Final Feed View Test which appears after user logs in and user has to be online
        exploreFeedRobot
            .scrollToSuggestedEditsIfVisible()
    }
}
