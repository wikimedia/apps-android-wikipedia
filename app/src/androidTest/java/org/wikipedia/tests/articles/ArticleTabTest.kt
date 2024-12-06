package org.wikipedia.tests.articles

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.base.BaseTest
import org.wikipedia.main.MainActivity
import org.wikipedia.robots.DialogRobot
import org.wikipedia.robots.SystemRobot
import org.wikipedia.robots.feature.ExploreFeedRobot
import org.wikipedia.robots.feature.PageRobot
import org.wikipedia.robots.feature.SearchRobot
import org.wikipedia.robots.feature.TabsRobot

@LargeTest
@RunWith(AndroidJUnit4::class)
class ArticleTabTest : BaseTest<MainActivity>(
 activityClass = MainActivity::class.java
) {

    private val pageRobot = PageRobot()
    private val tabsRobot = TabsRobot()
    private val systemRobot = SystemRobot()
    private val exploreFeedRobot = ExploreFeedRobot()
    private val dialogRobot = DialogRobot()
    private val searchRobot = SearchRobot()

    @Test
    fun runTest() {
        systemRobot
            .clickOnSystemDialogWithText("Allow")
        exploreFeedRobot
            .scrollToItem(title = Constants.FEATURED_ARTICLE)
            .clickOnFeaturedArticle()
        pageRobot
            .dismissTooltip(activity)
            .navigateUp()
        exploreFeedRobot
            .scrollToItem(title = Constants.FEATURED_ARTICLE)
            .clickOnFeaturedArticle()
        dialogRobot
            .dismissBigEnglishDialog()
            .dismissContributionDialog()
        tabsRobot
            .launchTabsScreen()
            .assertColorOfTabsTitle(0)
            .createNewTabWithContentDescription(context.getString(R.string.menu_new_tab))

        searchRobot
            .tapSearchView()
            .typeTextInView(Constants.SEARCH_TERM)
            .clickOnItemFromSearchList(0)
        tabsRobot
            .launchTabsScreen()
            .assertColorOfTabsTitle(1)
            .createNewTabWithContentDescription(context.getString(R.string.menu_new_tab))
        searchRobot
            .tapSearchView()
            .typeTextInView(Constants.SEARCH_TERM2)
            .clickOnItemFromSearchList(0)
        tabsRobot
            .launchTabsScreen()
            .assertColorOfTabsTitle(2)
            .verifyTabCount(3)
            .removeTab(0)
            .verifyTabCount(2)
    }
}
