package org.wikipedia.main.loggedoutuser

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.Constants
import org.wikipedia.FakeData
import org.wikipedia.base.BaseTest
import org.wikipedia.base.TestConfig.ARTICLE_TITLE
import org.wikipedia.main.MainActivity
import org.wikipedia.readinglist.ReadingListBehaviorsUtil
import org.wikipedia.robots.PageRobot
import org.wikipedia.robots.SystemRobot
import org.wikipedia.robots.screenrobots.HomeScreenRobot
import org.wikipedia.robots.screenrobots.SavedScreenRobot

@LargeTest
@RunWith(AndroidJUnit4::class)
class SavedScreenTest : BaseTest<MainActivity>(
    activityClass = MainActivity::class.java,
    isInitialOnboardingEnabled = false
) {

    private val homeScreenRobot = HomeScreenRobot()
    private val pageRobot = PageRobot()
    private val savedScreenRobot = SavedScreenRobot()
    private val systemRobot = SystemRobot()

    override fun setup() {
        super.setup()
        ReadingListBehaviorsUtil.addToDefaultList(
            activity,
            title = FakeData.title,
            addToDefault = true,
            invokeSource = Constants.InvokeSource.FEED)
    }

    @Test
    fun startSavedScreenTest() {
        homeScreenRobot
            .navigateToSavedPage()
        savedScreenRobot
            .clickOnFirstItemInTheList()
            .dismissTooltip(activity)
            .assertIfListMatchesTheArticleTitle(ARTICLE_TITLE)
        systemRobot
            .turnOnAirplaneMode()
        savedScreenRobot
            .openArticleWithTitle(ARTICLE_TITLE)
        pageRobot
            .clickOnBookmarkIcon()
            .removeArticleFromReadingList()
            .pressBack()
        savedScreenRobot
            .pressBack()
        systemRobot
            .turnOffAirplaneMode()
    }
}
