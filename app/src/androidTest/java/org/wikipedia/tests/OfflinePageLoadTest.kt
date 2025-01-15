package org.wikipedia.tests

import org.junit.Test
import org.wikipedia.TestConstants.FEATURED_ARTICLE
import org.wikipedia.base.BaseTest
import org.wikipedia.main.MainActivity
import org.wikipedia.robots.SystemRobot
import org.wikipedia.robots.feature.ExploreFeedRobot

class OfflinePageLoadTest : BaseTest<MainActivity>(
    activityClass = MainActivity::class.java
) {

    private val systemRobot = SystemRobot()
    private val exploreFeedRobot = ExploreFeedRobot()

    @Test
    fun runTest() {
        systemRobot
            .clickOnSystemDialogWithText("Allow")
        exploreFeedRobot
            .scrollToItem(title = FEATURED_ARTICLE)
            .clickOnFeaturedArticle()
            .pressBack()
        systemRobot
            .turnOffInternet()
        exploreFeedRobot
            .scrollToItem(title = FEATURED_ARTICLE)
            .clickOnFeaturedArticle()
            .pressBack()
        systemRobot
            .turnOnInternet()
    }
}
