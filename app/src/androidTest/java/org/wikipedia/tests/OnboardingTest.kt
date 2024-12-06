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
        onboardingRobot
            .checkWelcomeScreenViewsForVisibility()
            .checkPrimaryTextViewColor()
            .checkSecondaryTextViewColor()
            .verifyAppLanguageMatchesDeviceLanguage()
            .swipeAllTheWayToEnd()
            .swipeBackToWelcomeScreen()
            .moveAllTheWayToEndUsingTapButton()
            .swipeBackToWelcomeScreen()
    systemRobot
        .enableDarkMode(context)
    onboardingRobot
        .checkWelcomeScreenViewsForVisibility()
        .checkPrimaryTextViewColor()
        .checkSecondaryTextViewColor()
        .skipWelcomeScreen()
    systemRobot
        .clickOnSystemDialogWithText("Allow")
    }
}
