package org.wikipedia.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.base.BaseTest
import org.wikipedia.base.DataInjector
import org.wikipedia.base.TestConfig.ARTICLE_TITLE
import org.wikipedia.base.TestConfig.ARTICLE_TITLE_WORLD_CUP
import org.wikipedia.base.TestConfig.SEARCH_TERM
import org.wikipedia.base.TestConfig.SEARCH_TERM2
import org.wikipedia.main.MainActivity
import org.wikipedia.robots.DialogRobot
import org.wikipedia.robots.feature.SearchRobot
import org.wikipedia.robots.navigation.BottomNavRobot
import org.wikipedia.robots.screen.HomeScreenRobot
import org.wikipedia.theme.Theme

@LargeTest
@RunWith(AndroidJUnit4::class)
class SearchTest : BaseTest<MainActivity>(
    activityClass = MainActivity::class.java,
    dataInjector = DataInjector()
) {

    private val homeScreenRobot = HomeScreenRobot()
    private val searchRobot = SearchRobot()
    private val bottomNavRobot = BottomNavRobot()
    private val dialogRobot = DialogRobot()

    @Test
    fun startSearchTest() {
        homeScreenRobot
            .clickSearchContainer()
        searchRobot
            .typeTextInView(SEARCH_TERM)
            .verifySearchResult(ARTICLE_TITLE)
            .assertColorOfTitleInTheSearchList(position = 0, theme = Theme.LIGHT)
            .assertColorOfTitleInTheSearchList(position = 1, theme = Theme.LIGHT)
            .clickOnItemFromSearchList(0)
            .pressBack()
            .navigateUp()
        setDeviceOrientation(isLandscape = true)
        bottomNavRobot
            .navigateToSearchPage()
        searchRobot
            .tapSearchView()
            .typeTextInView(SEARCH_TERM2)
            .verifySearchResult(ARTICLE_TITLE_WORLD_CUP)
            .clickOnItemFromSearchList(0)
        setDeviceOrientation(isLandscape = false)
        dialogRobot
            .dismissBigEnglishDialog()
            .dismissContributionDialog()
        searchRobot
            .backToHistoryScreen()
            .verifyHistoryArticle(ARTICLE_TITLE_WORLD_CUP)
            .clickFilterHistoryButton()
            .typeTextInView(SEARCH_TERM2)
            .verifyHistoryArticle(ARTICLE_TITLE_WORLD_CUP)
            .assertColorOfTitleInTheHistoryList(position = 1, theme = Theme.LIGHT)
            .pressBack()
            .pressBack()
            .clickOnItemFromHistoryList(2)
            .pressBack()
            .longClickOnItemFromHistoryList(2)
            .swipeToDelete(2, ARTICLE_TITLE)
            .verifyArticleRemoved(ARTICLE_TITLE_WORLD_CUP)
    }
}
