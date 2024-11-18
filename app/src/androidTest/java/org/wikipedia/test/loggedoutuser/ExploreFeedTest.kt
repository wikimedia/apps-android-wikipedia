package org.wikipedia.test.loggedoutuser

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.base.BaseTest
import org.wikipedia.main.MainActivity
import org.wikipedia.robots.SystemRobot
import org.wikipedia.robots.feature.ExploreFeedRobot
import org.wikipedia.robots.feature.LoginRobot
import org.wikipedia.robots.navigation.BottomNavRobot
import org.wikipedia.robots.screen.HomeScreenRobot

@LargeTest
@RunWith(AndroidJUnit4::class)
class ExploreFeedTest : BaseTest<MainActivity>(
    activityClass = MainActivity::class.java,
    isInitialOnboardingEnabled = false
) {
    private val exploreFeedRobot = ExploreFeedRobot()
    private val systemRobot = SystemRobot()
    private val homeScreenRobot = HomeScreenRobot()
    private val navRobot = BottomNavRobot()
    private val loginRobot = LoginRobot()

    @Test
    fun startExploreFeedTest() {
        systemRobot
            .clickOnSystemDialogWithText("Allow")
        homeScreenRobot
            .dismissFeedCustomization()
        exploreFeedRobot
            .scrollToCardWithTitle(FEATURED_ARTICLE)
            .longClickFeaturedArticleCardContainer()
            .clickSave()
            .clickTodayOnWikipedia()
            .pressBack()
            .scrollToCardWithTitle(TOP_READ_ARTICLES)
            .topReadCardCanBeSeenAndSaved()
            .clickSave()
            .scrollToCardWithTitle(PICTURE_OF_DAY)
            .clickPictureOfTheDay()
            .pressBack()
            .scrollToCardWithTitle(NEWS_CARD)
            .clickNewsArticle()
            .longClickNewsArticleAndSave()
            .pressBack()
            .scrollToCardWithTitle(ON_THIS_DAY_CARD)
            .longClickOnThisDayCardAndSave()
            .scrollToCardWithTitle(RANDOM_CARD)
            .longClickRandomArticleAndSave()

        // Because you read, requires users to read some article for some time
        exploreFeedRobot
            .scrollToCardWithTitle(RANDOM_CARD)
            .longClickRandomArticleAndSave()
            .scrollToCardWithTitle(FEATURED_ARTICLE)
            .clickOnFeaturedArticle()
            .pressBack()
            .swipeToRefresh()
            .scrollToCardWithTitle(BECAUSE_YOU_READ)
            .clickBecauseYouReadArticle()
            .pressBack()
            .pressBack()

        // Suggested Edits requires log in
        navRobot
            .navigateToMoreMenu()
            .clickLoginMenuItem()
        loginRobot
            .clickLoginButton()
            .setLoginUserNameFromBuildConfig()
            .setPasswordFromBuildConfig()
            .loginUser()
        systemRobot
            .clickOnSystemDialogWithText(text = "Allow")
        exploreFeedRobot
            .scrollToCardWithTitle(SUGGESTED_EDITS)
            .clickAddArticleDescription()
            .pressBack()
    }

    companion object {
        const val FEATURED_ARTICLE = "Featured article"
        const val TODAY_ON_WIKIPEDIA_MAIN_PAGE = "Today on Wikipedia"
        const val TOP_READ_ARTICLES = "Top read"
        const val PICTURE_OF_DAY = "Picture of the day"
        const val BECAUSE_YOU_READ = "Because you read"
        const val PLACES_NEARBY = "Places nearby"
        const val NEWS_CARD = "In the news"
        const val ON_THIS_DAY_CARD = "On this day"
        const val RANDOM_CARD = "Random article"
        const val SUGGESTED_EDITS = "Suggested edits"
    }
}
