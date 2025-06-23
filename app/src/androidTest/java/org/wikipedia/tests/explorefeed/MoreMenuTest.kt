package org.wikipedia.tests.explorefeed

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.base.BaseTest
import org.wikipedia.base.DataInjector
import org.wikipedia.main.MainActivity
import org.wikipedia.robots.SystemRobot
import org.wikipedia.robots.feature.LoginRobot
import org.wikipedia.robots.feature.MoreMenuRobot
import org.wikipedia.robots.feature.SettingsRobot
import org.wikipedia.robots.navigation.BottomNavRobot

@LargeTest
@RunWith(AndroidJUnit4::class)
class MoreMenuTest : BaseTest<MainActivity>(
    activityClass = MainActivity::class.java,
    dataInjector = DataInjector()
) {
    private val systemRobot = SystemRobot()
    private val bottomNavRobot = BottomNavRobot()
    private val moreMenuRobot = MoreMenuRobot()
    private val settingsRobot = SettingsRobot()
    private val loginRobot = LoginRobot()

    @Test
    fun runTest() {
        // sometimes notification dialog may appear
        systemRobot
            .clickOnSystemDialogWithText("Allow")
        // not-login
        bottomNavRobot
            .navigateToMoreMenu()
        moreMenuRobot
            .verifyAllNonLoginItemsExists()
            .clickPlaces()
        systemRobot
            .clickOnSystemDialogWithText("While using the app")
        moreMenuRobot
            .verifyPlacesIsAccessible()
        systemRobot
            .pressBack()
        bottomNavRobot
            .navigateToMoreMenu()
        moreMenuRobot
            .clickSettings()
        settingsRobot
            .verifyTitle()
        systemRobot
            .pressBack()
        bottomNavRobot
            .navigateToMoreMenu()

        // after log in
        moreMenuRobot
            .clickLogin()
        loginRobot
            .logInUser()
        systemRobot
            .clickOnSystemDialogWithText("Allow")
        bottomNavRobot
            .navigateToMoreMenu()
        moreMenuRobot
            .verifyAllLoginItemsExists()
            .clickUserContributions()
            .verifyUserContributionIsAccessible()
        systemRobot
            .pressBack()
        bottomNavRobot
            .navigateToMoreMenu()
        moreMenuRobot
            .clickTalk()
            .verifyUserTalkIsAccessible()
        systemRobot
            .pressBack()
        bottomNavRobot
            .navigateToMoreMenu()
        moreMenuRobot
            .clickWatchList()
            .verifyWatchListIsAccessible()
        settingsRobot
            .pressBack()
        bottomNavRobot
            .navigateToMoreMenu()
        moreMenuRobot
            .clickDonate()
            .verifyDonateFlowIsAccessible(context)
    }
}
