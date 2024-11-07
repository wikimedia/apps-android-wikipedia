package org.wikipedia.main

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.base.BaseTest
import org.wikipedia.robots.OnboardingRobot
import org.wikipedia.robots.SettingsRobot
import org.wikipedia.robots.screenrobots.HomeScreenRobot

@LargeTest
@RunWith(AndroidJUnit4::class)
class HomeScreenTest : BaseTest<MainActivity>(
    activityClass = MainActivity::class.java,
    isInitialOnboardingEnabled = false
) {
    private val onboardingRobot = OnboardingRobot()
    private val homeScreenRobot = HomeScreenRobot()
    private val settingsRobot = SettingsRobot()

    @Test
    fun startHomeScreen() {
        onboardingRobot
            .dismissFeedCustomization()
        homeScreenRobot
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
            .navigateToExploreFeed()
            .assertAllFeedCardsAreHidden()
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
