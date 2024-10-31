package org.wikipedia.main

import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.base.BaseTest
import org.wikipedia.base.TestConfig.Articles.ARTICLE_TITLE
import org.wikipedia.base.TestConfig.Articles.SEARCH_TERM
import org.wikipedia.robots.ArticleRobot
import org.wikipedia.robots.OnboardingRobot
import org.wikipedia.robots.SearchRobot

@LargeTest
@RunWith(AndroidJUnit4::class)
class SmokeTest2 : BaseTest() {
    private val onboardingRobot = OnboardingRobot()
    private val searchRobot = SearchRobot()
    private val articleRobot = ArticleRobot()

    @Test
    fun smokeTest2() {
        onboardingRobot
            .completeOnboarding()
            .dismissFeedCustomization()

        searchRobot
            .performSearch(SEARCH_TERM)
            .verifySearchResult(ARTICLE_TITLE)

        setDeviceOrientation(true)
        pressBack()

        // Make sure the same title appears in the new screen orientation
        searchRobot.verifySearchResult(ARTICLE_TITLE)

        // Rotate the device back to the original orientation
        setDeviceOrientation(false)

        searchRobot.clickOnItemFromSearchList(0)

        articleRobot.dismissTooltip(activity)
        articleRobot.clickLink("3-sphere")
    }
}
