package org.wikipedia.main.loggedinuser

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.base.BaseTest
import org.wikipedia.main.MainActivity
import org.wikipedia.robots.ExploreFeedRobot
import org.wikipedia.robots.LoginRobot
import org.wikipedia.robots.SystemRobot
import org.wikipedia.robots.screenrobots.HomeScreenRobot

@LargeTest
@RunWith(AndroidJUnit4::class)
class SuggestedEditCardTest : BaseTest<MainActivity>(
    activityClass = MainActivity::class.java,
    isInitialOnboardingEnabled = false
) {

    private val exploreFeedRobot = ExploreFeedRobot()
    private val homeScreenRobot = HomeScreenRobot()
    private val loginRobot = LoginRobot()
    private val systemRobot = SystemRobot()

    @Test
    fun startSuggestedEditCardTest() {
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
        exploreFeedRobot
            .scrollToPositionOnTheFeed(SUGGESTED_EDIT_CARD)
            .clickAddArticleDescription()
            .pressBack()
    }

    companion object {
        const val SUGGESTED_EDIT_CARD = 10
    }
}
