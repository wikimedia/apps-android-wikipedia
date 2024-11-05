package org.wikipedia.main

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.base.BaseTest
import org.wikipedia.robots.OnboardingRobot

@LargeTest
@RunWith(AndroidJUnit4::class)
class OnboardingTest : BaseTest<MainActivity>(
    activityClass = MainActivity::class.java
) {
    private val onboardingRobot = OnboardingRobot()

    @Test
    fun startOnboardingTest() {
        onboardingRobot
            .checkWelcomeScreenViewsForVisibility()
            .verifyAppLanguageMatchesDeviceLanguage()
            .swipeAllTheWayToEnd()
            .swipeBackToWelcomeScreen()
            .moveAllTheWayToEndUsingTapButton()
            .swipeBackToWelcomeScreen()
            .checkWelcomeScreenViewsForVisibility()
            .skipWelcomeScreen()
    }
}
