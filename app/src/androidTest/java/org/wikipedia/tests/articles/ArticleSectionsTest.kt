package org.wikipedia.tests.articles

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.TestConstants.SEARCH_TERM
import org.wikipedia.base.BaseTest
import org.wikipedia.main.MainActivity
import org.wikipedia.robots.feature.PageRobot
import org.wikipedia.robots.feature.SearchRobot

@LargeTest
@RunWith(AndroidJUnit4::class)
class ArticleSectionsTest : BaseTest<MainActivity>(
 activityClass = MainActivity::class.java,
) {

    private val searchRobot = SearchRobot()
    private val pageRobot = PageRobot(context)

    @Test
    fun runTest() {
        searchRobot
            .tapSearchView()
            .typeTextInView(SEARCH_TERM)
            .clickOnItemFromSearchList(0)
        pageRobot
            .dismissTooltip(activity)
        setDeviceOrientation(isLandscape = true)
        pageRobot
            .scrollToCollapsingTables()
            .clickToExpandQuickFactsTable()
            .scrollToAboutThisArticle()
        setDeviceOrientation(isLandscape = false)
        pageRobot
            .goToViewEditHistory()
            .pressBack()
            .goToTalkPage()
            .pressBack()
            .scrollToLegalSection()
    }
}
