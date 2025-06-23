package org.wikipedia.tests.settings

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.base.BaseTest
import org.wikipedia.main.MainActivity
import org.wikipedia.robots.SystemRobot
import org.wikipedia.robots.feature.ExploreFeedRobot
import org.wikipedia.robots.feature.SettingsRobot
import org.wikipedia.robots.navigation.BottomNavRobot
import org.wikipedia.robots.screen.HomeScreenRobot

@LargeTest
@RunWith(AndroidJUnit4::class)
class CustomizeExploreFeedTest : BaseTest<MainActivity>(
 activityClass = MainActivity::class.java
) {
    private val homeScreenRobot = HomeScreenRobot()
    private val bottomNavRobot = BottomNavRobot()
    private val settingsRobot = SettingsRobot()
    private val systemRobot = SystemRobot()
    private val exploreFeedRobot = ExploreFeedRobot()

    @Test
    fun runTest() {
        systemRobot
            .clickOnSystemDialogWithText("Allow")
        systemRobot
            .disableDarkMode(context)
        homeScreenRobot
            .dismissFeedCustomization()
        bottomNavRobot
            .navigateToMoreMenu()
            .goToSettings()
        settingsRobot
            .clickExploreFeed()
            .openMoreOptionsToolbar()
            .hideAllExploreFeeds()
            .pressBack()
            .pressBack()
        exploreFeedRobot
            .verifyFeedViewSize(1) // test failed here bug: 06-23 15:47:52.012  2079  2186 E EspressoError: expected:<1> but was:<2>
        bottomNavRobot
            .navigateToMoreMenu()
            .goToSettings()
        settingsRobot
            .clickExploreFeed()
            .openMoreOptionsToolbar()
            .showAllExploreFeeds()
            .pressBack()
            .pressBack()
            .verifyExploreFeedIsNotEmpty(context)
    }
}
