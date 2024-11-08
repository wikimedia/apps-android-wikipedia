package org.wikipedia.main.loggedinuser

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.base.BaseTest
import org.wikipedia.main.MainActivity
import org.wikipedia.main.loggedoutuser.ExploreFeedTest.Companion.FEATURED_ARTICLE
import org.wikipedia.robots.ExploreFeedRobot
import org.wikipedia.robots.LoginRobot
import org.wikipedia.robots.SystemRobot
import org.wikipedia.robots.screenrobots.HomeScreenRobot

@LargeTest
@RunWith(AndroidJUnit4::class)
class WatchListTest : BaseTest<MainActivity>(
    activityClass = MainActivity::class.java,
    isInitialOnboardingEnabled = false
) {

    private val loginRobot = LoginRobot()
    private val homeScreenRobot = HomeScreenRobot()
    private val systemRobot = SystemRobot()
    private val exploreFeedRobot = ExploreFeedRobot()

    @Test
    fun startWatchListTest() {
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
            .navigateToMoreMenu()
            .gotoWatchList()
            .pressBack()
        exploreFeedRobot
            .scrollToPositionOnFeedAndClick(FEATURED_ARTICLE)
            .openOverflowMenuItem()
            .addOrRemoveToWatchList()
    }
}
