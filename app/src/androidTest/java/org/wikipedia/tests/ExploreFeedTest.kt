package org.wikipedia.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.base.BaseTest
import org.wikipedia.base.DataInjector
import org.wikipedia.base.TestConfig.ARTICLE_TITLE
import org.wikipedia.base.TestConfig.SEARCH_TERM
import org.wikipedia.main.MainActivity
import org.wikipedia.robots.SystemRobot
import org.wikipedia.robots.feature.ExploreFeedRobot
import org.wikipedia.robots.feature.LoginRobot
import org.wikipedia.robots.feature.SearchRobot
import org.wikipedia.robots.navigation.BottomNavRobot
import org.wikipedia.robots.screen.HomeScreenRobot

@LargeTest
@RunWith(AndroidJUnit4::class)
class ExploreFeedTest : BaseTest<MainActivity>(
    activityClass = MainActivity::class.java,
    dataInjector = DataInjector()
) {
    private val exploreFeedRobot = ExploreFeedRobot()
    private val systemRobot = SystemRobot()
    private val homeScreenRobot = HomeScreenRobot()
    private val navRobot = BottomNavRobot()
    private val loginRobot = LoginRobot()
    private val searchRobot = SearchRobot()

    @Test
    fun startExploreFeedTest() {
        // sometimes notification dialog may appear
        systemRobot
            .clickOnSystemDialogWithText("Allow")

        // dismisses the onboarding card
        homeScreenRobot
            .dismissFeedCustomization()

        // Feed Test flow
        exploreFeedRobot
            .scrollToItem(title = FEATURED_ARTICLE)
            .clickOnFeaturedArticle()
            .pressBack()
            .scrollToItem(title = TODAY_ON_WIKIPEDIA_MAIN_PAGE, verticalOffset = -100)
            .clickTodayOnWikipedia()
            .dismissBigEnglishCampaignDialog()
            .dismissContributionDialog()
            .pressBack()
            .scrollToItem(title = TODAY_ON_WIKIPEDIA_MAIN_PAGE, verticalOffset = 400)
            .scrollToItem(title = TOP_READ_ARTICLES, verticalOffset = 400)
            .clickTopReadArticle()
            .scrollToItem(title = PICTURE_OF_DAY)
            .clickPictureOfTheDay()
            .pressBack()
            .scrollToItem(title = NEWS_CARD)
            .clickNewsArticle()
            .pressBack()
            .scrollToItem(title = ON_THIS_DAY_CARD)
            .clickOnThisDayCard()
            .pressBack()
            .scrollToItem(title = RANDOM_CARD)
            .clickRandomArticle()
            .pressBack()

        // Because you read, requires users to read some article for 30 seconds
        exploreFeedRobot
            .scrollToItem(title = FEATURED_ARTICLE)
            .stayOnFeaturedArticleFor(milliseconds = 30000)
            .pressBack()
            .swipeToRefresh()
            .scrollToItem(title = BECAUSE_YOU_READ)
            .clickBecauseYouReadArticle()
            .pressBack()

        // checking if notification icon and search icon can be tapped
        navRobot
            .navigateToExploreFeed()
        homeScreenRobot
            .clickSearchContainer()

        // Search Test
        searchRobot
            .typeTextInView(SEARCH_TERM)
            .verifySearchResult(ARTICLE_TITLE)
            .removeTextByTappingTrashIcon()
            .verifySearchTermIsCleared()

        setDeviceOrientation(isLandscape = true)

        searchRobot
            .typeTextInView(SEARCH_TERM)
            .verifySearchResult(ARTICLE_TITLE)

        setDeviceOrientation(isLandscape = false)

        searchRobot
            .clickOnItemFromSearchList(0)
            .goBackToSearchScreen()

        searchRobot
            .removeTextByTappingTrashIcon()
            .verifyRecentSearchesAppears()
            .pressBack()
            .pressBack()

        // Checking the navigation menu items
        navRobot
            .navigateToSavedPage()
            .navigateToSearchPage()
            .navigateToEdits()
            .navigateToMoreMenu()
            .pressBack()
            .navigateToExploreFeed()

        // Following test requires login
        // 1. Notification click
        // 2. Suggested Edit Visibility

        // Logging user
        navRobot
            .navigateToMoreMenu()
            .clickLoginMenuItem()
        loginRobot
            .clickLoginButton()
            .setLoginUserNameFromBuildConfig()
            .setPasswordFromBuildConfig()
            .loginUser()
        // After log in, notification dialog appears
        systemRobot
            .clickOnSystemDialogWithText(text = "Allow")

        homeScreenRobot
            .navigateToNotifications()
            .pressBack()

        // Final Feed View Test which appears after user logs in and user has to be online
        exploreFeedRobot
            .scrollToSuggestedEditsIfVisible()
    }

    companion object {
        const val FEATURED_ARTICLE = "Featured article"
        const val TODAY_ON_WIKIPEDIA_MAIN_PAGE = "Today on Wikipedia"
        const val TOP_READ_ARTICLES = "Top read"
        const val PICTURE_OF_DAY = "Picture of the day"
        const val BECAUSE_YOU_READ = "Because you read"
        const val NEWS_CARD = "In the news"
        const val ON_THIS_DAY_CARD = "On this day"
        const val RANDOM_CARD = "Random article"
        const val SUGGESTED_EDITS = "Suggested edits"
    }
}
