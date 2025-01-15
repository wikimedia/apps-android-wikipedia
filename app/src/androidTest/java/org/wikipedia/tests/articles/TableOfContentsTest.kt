package org.wikipedia.tests.articles

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.Constants
import org.wikipedia.FakeData
import org.wikipedia.base.BaseTest
import org.wikipedia.base.DataInjector
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageActivity.Companion.ACTION_LOAD_IN_CURRENT_TAB
import org.wikipedia.page.PageActivity.Companion.EXTRA_HISTORYENTRY
import org.wikipedia.robots.DialogRobot
import org.wikipedia.robots.SystemRobot
import org.wikipedia.robots.feature.PageRobot
import org.wikipedia.robots.screen.HomeScreenRobot

@LargeTest
@RunWith(AndroidJUnit4::class)
class TableOfContentsTest : BaseTest<PageActivity>(
    activityClass = PageActivity::class.java,
    dataInjector = DataInjector(
        intentBuilder = {
            action = ACTION_LOAD_IN_CURRENT_TAB
            putExtra(EXTRA_HISTORYENTRY, FakeData.historyEntry)
            putExtra(Constants.ARG_TITLE, FakeData.historyEntry.title)
        }
    )
) {
    private val pageRobot = PageRobot(context)
    private val homeScreenRobot = HomeScreenRobot()
    private val dialogRobot = DialogRobot()
    private val systemRobot = SystemRobot()

    @Test
    fun runTest() {
        systemRobot
            .clickOnSystemDialogWithText("Allow")
        dialogRobot
            .dismissBigEnglishDialog()
        setDeviceOrientation(isLandscape = false)
        tocTest()
        setDeviceOrientation(isLandscape = true)
        tocTest()
    }

    private fun tocTest() {
        pageRobot
            .openTableOfContents()
            .clickOutside()
            .openTableOfContents()
            .clickOnTOCItem(5)
            .openTableOfContents()
            .swipeTableOfContentsAllTheWayToBottom()
            .clickAboutThisArticleTextInTOC()
    }
}
