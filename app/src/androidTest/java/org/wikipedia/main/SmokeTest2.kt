package org.wikipedia.main

import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.base.BaseTest
import org.wikipedia.robots.ArticleRobot
import org.wikipedia.robots.OnboardingRobot
import org.wikipedia.robots.SearchRobot

@LargeTest
@RunWith(AndroidJUnit4::class)
class SmokeTest2 : BaseTest<MainActivity>(
    MainActivity::class.java
) {
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

        articleRobot
            .dismissTooltip(activity)
            .clickLink("3-sphere")
            .previewArticle()
            .clickLink("Sphere")
            .openInNewTab()
            .verifyTabCount("2")
            .goBackToOriginalArticle()
            .verifyHeaderViewWithLeadImage()
            .clickLeadImage()
            .swipeLeft()
            .clickOverflowMenu("More options")
            .visitImagePage()
            .goBackToGalleryView()
            .goBackToOriginalArticle()
            .enableJavaScript()
            .verifyArticleTitle(ARTICLE_TITLE)
    }

    companion object {
        const val SEARCH_TERM = "hopf fibration"
        const val ARTICLE_TITLE = "Hopf fibration"
        const val ARTICLE_TITLE_ESPANOL = "Fibración de Hopf"
    }
}

