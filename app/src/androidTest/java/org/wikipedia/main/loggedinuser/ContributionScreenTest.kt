package org.wikipedia.main.loggedinuser

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.base.BaseTest
import org.wikipedia.main.MainActivity
import org.wikipedia.robots.LoginRobot
import org.wikipedia.robots.SystemRobot
import org.wikipedia.robots.screenrobots.ContributionScreenScreenRobot
import org.wikipedia.robots.screenrobots.HomeScreenRobot

@LargeTest
@RunWith(AndroidJUnit4::class)
class ContributionScreenTest : BaseTest<MainActivity>(
    activityClass = MainActivity::class.java,
    isInitialOnboardingEnabled = false
) {

    private val loginRobot = LoginRobot()
    private val homeScreenRobot = HomeScreenRobot()
    private val systemRobot = SystemRobot()
    private val contributionScreenScreenRobot = ContributionScreenScreenRobot()

    @Test
    fun startEditsScreenTest() {
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
            .navigateToEdits()

        if (contributionScreenScreenRobot.isDisabled().not()) {
            contributionScreenScreenRobot
                .clickThroughScreenStatsOnboarding()
                .enterContributionScreen()
                .clickFilterButton()
                .assertThePresenceOfAllFilters()
                .pressBack()
                .clickAddDescriptionTask()
                .assertThePresenceOfCorrectActionButton()
                .pressBack()
                .assertTranslateButtonLeadingToAddLanguageScreen()
                .clickAddLanguageButton()
                .selectLanguage()
                .pressBack()
                .assertTranslateButtonLeadingToTranslateDescriptionScreen()
                .assertThePresenceOfCorrectActionButtonText()
                .assertImageCaptionTranslationTaskAndSubsequentActionText()
                .pressBack()
                .clickOnImageTask()
                .assertThePresenceOfAddCaptionButton()
                .pressBack()
                .scrollToImageTagsTask()
                .scrollToImageTagsTask()
                .clickGetStartedButton()
                .assertThePresenceOfAddTagButton()
                .pressBack()
                .assertThePresenceOfTutorialButton()
        }
    }
}
