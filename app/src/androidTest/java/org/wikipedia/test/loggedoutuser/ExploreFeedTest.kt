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
            .scrollToCardWithTitle(FEATURED_ARTICLE)
            .longClickFeaturedArticleCardContainer()
            .clickSave()
            .scrollToCardWithTitle(TOP_READ_ARTICLES)
            .topReadCardCanBeSeenAndSaved()
            .clickSave()
            .scrollToCardWithTitleAndClick(PICTURE_OF_DAY)
            .pressBack()
            .scrollToCardWithTitle(FEATURED_ARTICLE)
            .scrollToCardWithTitle(NEWS_CARD)
            .clickNewsArticle()
            .longClickNewsArticleAndSave()
            .pressBack()
            .scrollToCardWithTitle(ON_THIS_DAY_CARD)
            .longClickOnThisDayCardAndSave()
            .scrollToCardWithTitle(FEATURED_ARTICLE)
            .scrollToCardWithTitle(RANDOM_CARD)
            .longClickRandomArticleAndSave()
            .scrollToViewMainPageAndClick()
            .pressBack()
    }

    companion object {
        const val FEATURED_ARTICLE = "Featured article"
        const val TOP_READ_ARTICLES = "Top read"
        const val PLACES_NEARBY = "Places nearby"
        const val PICTURE_OF_DAY = "Picture of the day"
        const val NEWS_CARD = "In the news"
        const val ON_THIS_DAY_CARD = "On this day"
        const val RANDOM_CARD = "Random article"
        const val MAIN_PAGE = "Today on Wikipedia"
    }
}
