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
            .scrollAndPerform(title = FEATURED_ARTICLE) { position ->
                assertFeaturedArticleTitleColor(theme = Theme.LIGHT)
                clickOnFeaturedArticle(position)
                pressBack()
            }
            .scrollAndPerform(title = TODAY_ON_WIKIPEDIA_MAIN_PAGE) { position ->
                clickTodayOnWikipedia(position)
                dialogRobot
                    .dismissBigEnglishDialog()
                    .dismissContributionDialog()
                pressBack()
            }
        systemRobot
            .enableDarkMode(context)
        exploreFeedRobot
            .scrollAndPerform(title = TOP_READ_ARTICLES) { position ->
                assertTopReadTitleColor(theme = Theme.DARK)
                clickTopReadArticle(position)
                pressBack()
            }
            .scrollAndPerform(title = PICTURE_OF_DAY) { position ->
                clickPictureOfTheDay(position)
                pressBack()
            }
        systemRobot
            .disableDarkMode(context)
        exploreFeedRobot
            .scrollAndPerform(title = NEWS_CARD) { position ->
                clickNewsArticle(position)
                pressBack()
            }
            .scrollAndPerform(title = ON_THIS_DAY_CARD) { position ->
                clickOnThisDayCard(position)
                pressBack()
            }
            .scrollAndPerform(title = RANDOM_CARD) { position ->
                clickRandomArticle(position)
                pressBack()
            }
    }
}
