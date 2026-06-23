package org.wikipedia.tests.articles

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.base.BaseTest
import org.wikipedia.main.MainActivity
import org.wikipedia.robots.SystemRobot
import org.wikipedia.robots.feature.PageRobot

@LargeTest
@RunWith(AndroidJUnit4::class)
class ArticleSectionsTest : BaseTest<MainActivity>(
 activityClass = MainActivity::class.java,
) {
    private val pageRobot = PageRobot(context)
    private val systemRobot = SystemRobot()

    @Test
    fun runTest() {
        systemRobot
            .clickOnSystemDialogWithText("Allow")
        // TODO: update the test steps
        // 1. Search for an article and open it
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
