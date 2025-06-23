package org.wikipedia.tests.explorefeed

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.R
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
            .scrollToItem(title = context.getString(R.string.view_featured_article_card_title))
            .assertFeaturedArticleTitleColor(theme = Theme.LIGHT)
            .clickOnFeaturedArticle()
            .pressBack()
            .scrollToItem(title = context.getString(R.string.view_main_page_card_title), verticalOffset = -100)
            .clickTodayOnWikipedia()
        dialogRobot
            .dismissBigEnglishDialog()
            .dismissContributionDialog()
        exploreFeedRobot
            .pressBack()
        systemRobot
            .enableDarkMode(context)
        exploreFeedRobot
            .scrollToItem(title = context.getString(R.string.view_main_page_card_title), verticalOffset = 400)
            .scrollToItem(title = context.getString(R.string.view_top_read_card_title), verticalOffset = 400)
//            .assertTopReadTitleColor(theme = Theme.DARK)
            .clickTopReadArticle()
//            .scrollToItem(title = context.getString(R.string.view_featured_image_card_title))
//            .clickPictureOfTheDay() // test failed here bug: androidx.test.espresso.PerformException: Error performing 'single click' on view 'Animations or transitions are enabled on the target device. Caused by: java.lang.RuntimeException: Action will not be performed because the target view does not match one or more of the following constraints:
//            .pressBack()
//        systemRobot
//            .enableDarkMode(context)
//        exploreFeedRobot
//            .scrollToItem(title = context.getString(R.string.view_card_news_title))
//            .clickNewsArticle()
//            .pressBack()
//            .scrollToItem(title = context.getString(R.string.on_this_day_card_title))
//            .clickOnThisDayCard()
//            .pressBack()
//            .scrollToItem(title =  context.getString(R.string.view_random_article_card_title))
//            .clickRandomArticle()
//            .pressBack()
    }
}
