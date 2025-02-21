package org.wikipedia.tests.explorefeed

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.TestConstants.FEATURED_ARTICLE
import org.wikipedia.TestConstants.NEWS_CARD
import org.wikipedia.TestConstants.ON_THIS_DAY_CARD
import org.wikipedia.TestConstants.PICTURE_OF_DAY
import org.wikipedia.TestConstants.RANDOM_CARD
import org.wikipedia.TestConstants.TODAY_ON_WIKIPEDIA_MAIN_PAGE
import org.wikipedia.TestConstants.TOP_READ_ARTICLES
import org.wikipedia.base.BaseTest
import org.wikipedia.main.MainActivity
import org.wikipedia.robots.DialogRobot
import org.wikipedia.robots.SystemRobot
import org.wikipedia.robots.feature.ExploreFeedRobot
import org.wikipedia.robots.screen.HomeScreenRobot
import org.wikipedia.theme.Theme

@LargeTest
@RunWith(AndroidJUnit4::class)
class FeedScreenTest : BaseTest<MainActivity>(
 activityClass = MainActivity::class.java
) {

    private val exploreFeedRobot = ExploreFeedRobot()
    private val systemRobot = SystemRobot()
    private val homeScreenRobot = HomeScreenRobot()
    private val dialogRobot = DialogRobot()

    @Test
    fun runTest() {
        // sometimes notification dialog may appear
        systemRobot
            .clickOnSystemDialogWithText("Allow")
            .disableDarkMode(context)

        // dismisses the onboarding card
        homeScreenRobot
            .dismissFeedCustomization()

        // Feed Test flow
        exploreFeedRobot
            .scrollToItem(title = FEATURED_ARTICLE)
            .assertFeaturedArticleTitleColor(theme = Theme.LIGHT)
            .clickOnFeaturedArticle()
            .pressBack()
            .scrollToItem(title = TODAY_ON_WIKIPEDIA_MAIN_PAGE, verticalOffset = -100)
            .clickTodayOnWikipedia()
        dialogRobot
            .dismissBigEnglishDialog()
            .dismissContributionDialog()
        exploreFeedRobot
            .pressBack()
        systemRobot
            .enableDarkMode(context)
        exploreFeedRobot
            .scrollToItem(title = TODAY_ON_WIKIPEDIA_MAIN_PAGE, verticalOffset = 400)
            .scrollToItem(title = TOP_READ_ARTICLES, verticalOffset = 400)
            .assertTopReadTitleColor(theme = Theme.DARK)
            .clickTopReadArticle()
            .scrollToItem(title = PICTURE_OF_DAY)
            .clickPictureOfTheDay()
            .pressBack()
        systemRobot
            .enableDarkMode(context)
        exploreFeedRobot
            .scrollToItem(title = NEWS_CARD)
            .clickNewsArticle()
            .pressBack()
            .scrollToItem(title = ON_THIS_DAY_CARD)
            .clickOnThisDayCard()
            .pressBack()
            .scrollToItem(title = RANDOM_CARD)
            .clickRandomArticle()
            .pressBack()
    }
}
