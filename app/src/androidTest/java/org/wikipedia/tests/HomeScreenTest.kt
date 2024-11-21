package org.wikipedia.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.base.BaseTest
import org.wikipedia.main.MainActivity
import org.wikipedia.robots.feature.SettingsRobot
import org.wikipedia.robots.navigation.BottomNavRobot
import org.wikipedia.robots.screen.HomeScreenRobot

@LargeTest
@RunWith(AndroidJUnit4::class)
class HomeScreenTest : BaseTest<MainActivity>(
    activityClass = MainActivity::class.java,
    isInitialOnboardingEnabled = false
) {
    private val bottomNavRobot = BottomNavRobot()
    private val homeScreenRobot = HomeScreenRobot()
    private val settingsRobot = SettingsRobot()

    @Test
    fun startHomeScreen() {
        homeScreenRobot
            .dismissFeedCustomization()
        bottomNavRobot
            .navigateToSavedPage()
            .navigateToSearchPage()
            .navigateToEdits()
            .navigateToMoreMenu()
            .goToSettings()
        settingsRobot
            .clickExploreFeedSettingItem()
            .openMoreOptionsToolbar()
            .hideAllExploreFeeds()
        homeScreenRobot
            .pressBack()
            .pressBack()
        bottomNavRobot
            .navigateToExploreFeed()
        homeScreenRobot
            .assertAllFeedCardsAreHidden()
        bottomNavRobot
            .navigateToMoreMenu()
            .goToSettings()
        settingsRobot
            .clickExploreFeedSettingItem()
            .openMoreOptionsToolbar()
            .showAllExploreFeeds()
            .pressBack()
            .pressBack()
        homeScreenRobot
            .assertEmptyMessageIsNotVisible()
    }
}
