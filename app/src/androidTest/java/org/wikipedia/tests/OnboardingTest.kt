package org.wikipedia.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.base.BaseTest
import org.wikipedia.base.DataInjector
import org.wikipedia.main.MainActivity
import org.wikipedia.robots.SystemRobot
import org.wikipedia.robots.feature.OnboardingRobot
import org.wikipedia.theme.Theme

@LargeTest
@RunWith(AndroidJUnit4::class)
class OnboardingTest : BaseTest<MainActivity>(
    activityClass = MainActivity::class.java,
    dataInjector = DataInjector(isInitialOnboardingEnabled = true)
) {
    private val onboardingRobot = OnboardingRobot()
    private val systemRobot = SystemRobot()

    @Test
    fun startOnboardingTest() {
        systemRobot
            .disableDarkMode(context)
        onboardingRobot
            .checkWelcomeScreenViewsForVisibility()
            .checkPrimaryTextViewColor(Theme.LIGHT)
            .checkSecondaryTextViewColor(Theme.LIGHT)
            .verifyAppLanguageMatchesDeviceLanguage()
            .swipeAllTheWayToEnd()
            .swipeBackToWelcomeScreen()
            .moveAllTheWayToEndUsingTapButton()
            .swipeBackToWelcomeScreen()
    systemRobot
        .enableDarkMode(context)
    onboardingRobot
        .checkWelcomeScreenViewsForVisibility()
        .checkPrimaryTextViewColor(Theme.DARK)
        .checkSecondaryTextViewColor(Theme.DARK)
        .skipWelcomeScreen()
    systemRobot
        .clickOnSystemDialogWithText("Allow")
    }
}
