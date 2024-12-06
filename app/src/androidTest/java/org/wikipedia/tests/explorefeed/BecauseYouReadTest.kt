package org.wikipedia.tests.explorefeed

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.Constants.BECAUSE_YOU_READ
import org.wikipedia.Constants.FEATURED_ARTICLE
import org.wikipedia.base.BaseTest
import org.wikipedia.main.MainActivity
import org.wikipedia.robots.SystemRobot
import org.wikipedia.robots.feature.ExploreFeedRobot
import org.wikipedia.robots.screen.HomeScreenRobot

@LargeTest
@RunWith(AndroidJUnit4::class)
class BecauseYouReadTest : BaseTest<MainActivity>(
 activityClass = MainActivity::class.java
) {
    private val exploreFeedRobot = ExploreFeedRobot()
    private val systemRobot = SystemRobot()
    private val homeScreenRobot = HomeScreenRobot()

    @Test
    fun runTest() {
        // sometimes notification dialog may appear
        systemRobot
            .clickOnSystemDialogWithText("Allow")
        // dismisses the onboarding card
        homeScreenRobot
            .dismissFeedCustomization()

        // Because you read, requires users to read some article for 30 seconds
        exploreFeedRobot
            .scrollToItem(title = FEATURED_ARTICLE)
            .stayOnFeaturedArticleFor(milliseconds = 30000)
            .pressBack()
            .swipeToRefresh()
            .scrollToItem(title = BECAUSE_YOU_READ)
            .clickBecauseYouReadArticle()
            .pressBack()
    }
}
