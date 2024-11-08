package org.wikipedia.main.loggedoutuser

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.base.BaseTest
import org.wikipedia.base.TestConfig.ARTICLE_TITLE
import org.wikipedia.base.TestConfig.SEARCH_TERM
import org.wikipedia.main.MainActivity
import org.wikipedia.robots.SearchRobot
import org.wikipedia.robots.screenrobots.HistoryScreenRobot
import org.wikipedia.robots.screenrobots.HomeScreenRobot

@LargeTest
@RunWith(AndroidJUnit4::class)
class HistoryScreenTest : BaseTest<MainActivity>(
    activityClass = MainActivity::class.java,
    isInitialOnboardingEnabled = false
) {

    private val homeScreenRobot = HomeScreenRobot()
    private val searchRobot = SearchRobot()
    private val historyScreenRobot = HistoryScreenRobot()

    @Test
    fun startSearchTest() {
        homeScreenRobot
            .navigateToSearchPage()
            .navigateToSearchPage()
        searchRobot
            .typeTextInView(SEARCH_TERM)
            .verifySearchResult(ARTICLE_TITLE)
            .clickOnItemFromSearchList(0)
            .pressBack()
            .pressBack()
            .pressBack() // press back three times to go back to history screen
        historyScreenRobot
            .clearHistory()
            .assertDeletionMessage()
            .clickNoOnAlertDialog()
    }
}
