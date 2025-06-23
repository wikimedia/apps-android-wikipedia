package org.wikipedia.tests.articles

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.Constants
import org.wikipedia.FakeData
import org.wikipedia.base.BaseTest
import org.wikipedia.base.DataInjector
import org.wikipedia.base.TestConfig.ARTICLE_TITLE_ESPANOL
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageActivity.Companion.ACTION_LOAD_IN_CURRENT_TAB
import org.wikipedia.page.PageActivity.Companion.EXTRA_HISTORYENTRY
import org.wikipedia.robots.SystemRobot
import org.wikipedia.robots.feature.PageRobot
import org.wikipedia.robots.feature.SearchRobot

@LargeTest
@RunWith(AndroidJUnit4::class)
class ArticlePageActionItemTest : BaseTest<PageActivity>(
 activityClass = PageActivity::class.java,
    dataInjector = DataInjector(
        intentBuilder = {
            action = ACTION_LOAD_IN_CURRENT_TAB
            putExtra(EXTRA_HISTORYENTRY, FakeData.historyEntry)
            putExtra(Constants.ARG_TITLE, FakeData.historyEntry.title)
        }
    )
) {
    private val FIND_IN_ARTICLE_TEXT = "Hopf"
    private val pageRobot = PageRobot(context)
    private val searchRobot = SearchRobot()
    private val systemRobot = SystemRobot()

    @Test
    fun runTest() {
        systemRobot
            .clickOnSystemDialogWithText("Allow")
        pageRobot
            .saveArticleToReadingList() // test failed here bug: 06-23 16:06:54.637 12375 12448 E EspressoError: No views in hierarchy found matching: view.getId() is <2131297468/org.wikipedia.dev:id/page_save>
            .confirmArticleSaved("Saved")
            .openLanguageSelector()
            .selectSpanishLanguage()
            .verifyArticleTitle(ARTICLE_TITLE_ESPANOL)
            .openFindInArticle()
        searchRobot
            .typeTextInView(FIND_IN_ARTICLE_TEXT)
        pageRobot
            .verifyFindInArticleCount("23")
            .pressBack()
        pageRobot
            .openTableOfContents()
            .swipeTableOfContentsAllTheWayToBottom()
            .pressBack()
            .swipeLeftToShowTableOfContents()
            .swipeTableOfContentsAllTheWayToBottom()
            .pressBack()
            .openThemeSelector()
    }
}
