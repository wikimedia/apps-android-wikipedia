package org.wikipedia.test.loggedoutuser

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.Constants
import org.wikipedia.FakeData
import org.wikipedia.base.BaseTest
import org.wikipedia.base.TestConfig.ARTICLE_TITLE
import org.wikipedia.readinglist.ReadingListBehaviorsUtil
import org.wikipedia.robots.SystemRobot
import org.wikipedia.robots.feature.PageRobot
import org.wikipedia.robots.navigation.BottomNavRobot
import org.wikipedia.robots.screen.SavedScreenRobot
import org.wikipedia.test.MainActivity

@LargeTest
@RunWith(AndroidJUnit4::class)
class SavedScreenTest : BaseTest<MainActivity>(
    activityClass = MainActivity::class.java,
    isInitialOnboardingEnabled = false
) {
    private val pageRobot = PageRobot()
    private val savedScreenRobot = SavedScreenRobot()
    private val systemRobot = SystemRobot()
    private val bottomNavRobot = BottomNavRobot()

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
        bottomNavRobot
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
