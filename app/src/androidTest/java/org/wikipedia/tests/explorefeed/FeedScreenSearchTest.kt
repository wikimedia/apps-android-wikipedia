package org.wikipedia.tests.explorefeed

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
import org.wikipedia.robots.feature.SearchRobot
import org.wikipedia.robots.screen.HomeScreenRobot

@LargeTest
@RunWith(AndroidJUnit4::class)
class FeedScreenSearchTest : BaseTest<MainActivity>(
    activityClass = MainActivity::class.java,
    dataInjector = DataInjector()
) {
    private val homeScreenRobot = HomeScreenRobot()
    private val searchRobot = SearchRobot()
    private val systemRobot = SystemRobot()

    @Test
    fun startExploreFeedTest() {
        systemRobot
            .clickOnSystemDialogWithText("Allow")
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
    }
}
