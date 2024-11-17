package org.wikipedia.test.loggedoutuser

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
class SettingsTest : BaseTest<MainActivity>(
    activityClass = MainActivity::class.java,
    isInitialOnboardingEnabled = false
) {
    private val homeScreenRobot = HomeScreenRobot()
    private val settingsRobot = SettingsRobot()
    private val bottomNavRobot = BottomNavRobot()

    @Test
    fun testSettings() {
        bottomNavRobot
            .navigateToMoreMenu()
            .goToSettings()
        settingsRobot
            .clickAboutWikipediaAppOptionItem()
            .activateDeveloperMode()
            .pressBack()
            .clickDeveloperMode()
            .assertWeAreInDeveloperSettings()
            .pressBack()
            .scrollToShowImagesOnSettings()
            .pressBack()
        homeScreenRobot
            .imagesDoesNotShow()
    }
}
