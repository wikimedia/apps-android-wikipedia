package org.wikipedia.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.base.BaseTest
import org.wikipedia.base.DataInjector
import org.wikipedia.main.MainActivity
import org.wikipedia.robots.SystemRobot
import org.wikipedia.robots.feature.LoginRobot
import org.wikipedia.robots.navigation.BottomNavRobot
import org.wikipedia.robots.screen.SuggestedEditsScreenRobot

@LargeTest
@RunWith(AndroidJUnit4::class)
class SuggestedEditScreenTest : BaseTest<MainActivity>(
    activityClass = MainActivity::class.java,
    dataInjector = DataInjector(
        overrideEditsContribution = 10
    )
) {

    private val navRobot = BottomNavRobot()
    private val loginRobot = LoginRobot()
    private val systemRobot = SystemRobot()
    private val suggestedEditsScreenRobot = SuggestedEditsScreenRobot()

    @Test
    fun runTest() {
        systemRobot
            .clickOnSystemDialogWithText("Allow")
        navRobot
            .navigateToMoreMenu()
            .clickLoginMenuItem()
        loginRobot
            .clickLoginButton()
            .setLoginUserNameFromBuildConfig()
            .setPasswordFromBuildConfig()
            .loginUser()
        systemRobot
            .clickOnSystemDialogWithText("Allow")
        navRobot
            .navigateToSuggestedEdits()
        suggestedEditsScreenRobot
            .verifyEditsIsVisible()
            .verifyViewsIsVisible()
            .verifyLastEditedIsVisible()
            .verifyEditQualityIsVisible()
            .verifyLastDonatedIsVisible()
            .enterContributionScreen()
            .pressBack()
            .clickArticleDescriptions()
            .pressBack()
            .clickImageCaptions()
            .pressBack()
            .clickImageTags()
            .pressBack()
            .clickSuggestedEdits()
    }
}
