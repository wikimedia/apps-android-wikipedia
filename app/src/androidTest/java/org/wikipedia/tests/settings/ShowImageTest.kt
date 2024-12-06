package org.wikipedia.tests.settings

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.base.BaseTest
import org.wikipedia.main.MainActivity
import org.wikipedia.robots.feature.ExploreFeedRobot
import org.wikipedia.robots.feature.SettingsRobot
import org.wikipedia.robots.navigation.BottomNavRobot
import org.wikipedia.robots.screen.HomeScreenRobot

@LargeTest
@RunWith(AndroidJUnit4::class)
class ShowImageTest : BaseTest<MainActivity>(
 activityClass = MainActivity::class.java
) {
    private val bottomNavRobot = BottomNavRobot()
    private val settingsRobot = SettingsRobot()
    private val exploreFeedRobot = ExploreFeedRobot()
    private val homeScreenRobot = HomeScreenRobot()

    @Test
    fun runTest() {
        homeScreenRobot
            .dismissFeedCustomization()
        bottomNavRobot
            .navigateToMoreMenu()
            .goToSettings()
        settingsRobot
            .toggleShowImages()
        exploreFeedRobot
            .pressBack()
            .scrollToItem(title = "Featured article")
            .verifyFeaturedArticleImageIsNotVisible()
            .scrollToItem(title = "Top read", verticalOffset = 200)
            .verifyTopReadArticleIsGreyedOut()
    }
}
