package org.wikipedia.tests

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
import org.wikipedia.tests.ExploreFeedTest.Companion.FEATURED_ARTICLE

@LargeTest
@RunWith(AndroidJUnit4::class)
class WatchListTest : BaseTest<MainActivity>(
    activityClass = MainActivity::class.java,
    isInitialOnboardingEnabled = false
) {

    private val loginRobot = LoginRobot()
    private val systemRobot = SystemRobot()
    private val exploreFeedRobot = ExploreFeedRobot()
    private val bottomNavRobot = BottomNavRobot()

    @Test
    fun startWatchListTest() {
        bottomNavRobot
            .navigateToMoreMenu()
            .clickLoginMenuItem()
        loginRobot
            .clickLoginButton()
            .setLoginUserNameFromBuildConfig()
            .setPasswordFromBuildConfig()
            .loginUser()
        systemRobot
            .clickOnSystemDialogWithText("Allow")
        bottomNavRobot
            .navigateToMoreMenu()
            .gotoWatchList()
            .pressBack()
        exploreFeedRobot
            .scrollToCardWithTitle(FEATURED_ARTICLE)
            .openOverflowMenuItem()
            .addOrRemoveToWatchList()
    }
}
