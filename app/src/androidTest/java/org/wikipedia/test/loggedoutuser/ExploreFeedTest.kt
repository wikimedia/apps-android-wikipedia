package org.wikipedia.test.loggedoutuser

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.base.BaseTest
import org.wikipedia.main.MainActivity
import org.wikipedia.robots.feature.ExploreFeedRobot
import org.wikipedia.robots.screen.HomeScreenRobot

@LargeTest
@RunWith(AndroidJUnit4::class)
class ExploreFeedTest : BaseTest<MainActivity>(
    activityClass = MainActivity::class.java,
    isInitialOnboardingEnabled = false
) {
    private val exploreFeedRobot = ExploreFeedRobot()
    private val homeScreenRobot = HomeScreenRobot()

    @Test
    fun startExploreFeedTest() {
        homeScreenRobot
            .dismissFeedCustomization()
        exploreFeedRobot
            .scrollToPositionOnTheFeed(FEATURED_ARTICLE)
            .longClickFeaturedArticleCardContainer()
            .clickSave()
            .scrollToPositionOnTheFeed(TOP_READ_ARTICLES)
            .topReadCardCanBeSeenAndSaved()
            .clickSave()
            .scrollToPositionOnFeedAndClick(PICTURE_OF_DAY)
            .pressBack()
            .scrollToPositionOnTheFeed(FEATURED_ARTICLE)
            .scrollToPositionOnTheFeed(NEWS_CARD)
            .clickNewsArticle()
            .longClickNewsArticleAndSave()
            .pressBack()
            .scrollToPositionOnTheFeed(ON_THIS_DAY_CARD)
            .longClickOnThisDayCardAndSave()
            .scrollToPositionOnTheFeed(FEATURED_ARTICLE)
            .scrollToPositionOnTheFeed(RANDOM_CARD)
            .longClickRandomArticleAndSave()
            .scrollToPositionOnTheFeed(MAIN_PAGE)
            .clickMainPageCard()
            .pressBack()
    }

    companion object {
        const val FEATURED_ARTICLE = 2
        const val TOP_READ_ARTICLES = 4
        const val PICTURE_OF_DAY = 6
        const val NEWS_CARD = 7
        const val ON_THIS_DAY_CARD = 8
        const val RANDOM_CARD = 9
        const val MAIN_PAGE = 10
    }
}
