package org.wikipedia.tests.articles

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.TestConstants
import org.wikipedia.TestConstants.FEATURED_ARTICLE
import org.wikipedia.base.BaseTest
import org.wikipedia.main.MainActivity
import org.wikipedia.robots.SystemRobot
import org.wikipedia.robots.feature.ExploreFeedRobot
import org.wikipedia.robots.feature.SearchRobot
import org.wikipedia.robots.navigation.BottomNavRobot
import org.wikipedia.robots.screen.SavedScreenRobot

@LargeTest
@RunWith(AndroidJUnit4::class)
class SavedArticleTest : BaseTest<MainActivity>(
 activityClass = MainActivity::class.java
) {
    private val searchRobot = SearchRobot()
    private val systemRobot = SystemRobot()
    private val exploreFeedRobot = ExploreFeedRobot()
    private val bottomNavRobot = BottomNavRobot()
    private val savedScreenRobot = SavedScreenRobot()

    @Test
    fun runTest() {
        setDeviceOrientation(isLandscape = false)
        systemRobot
            .clickOnSystemDialogWithText("Allow")
        searchRobot
            .tapSearchView()
            .typeTextInView(TestConstants.SEARCH_TERM)
            .longClickOnItemFromSearchList(0)
            .clickSave(action = { isSaved ->
                if (isSaved) {
                    searchRobot
                        .pressBack()
                        .pressBack()
                } else {
                    searchRobot
                        .pressBack()
                        .pressBack()
                        .pressBack()
                }
            })
        exploreFeedRobot
            .scrollToItem(title = FEATURED_ARTICLE) // test failed here bug: 06-23 16:06:45.221 11706 11997 E EspressoError: No views in hierarchy found matching: view.getId() is <2131296743/org.wikipedia.dev:id/feed_view>
            .longClickFeaturedArticleCardContainer()
            .clickSave()
        setDeviceOrientation(isLandscape = true)
        bottomNavRobot
            .navigateToSavedPage()
        savedScreenRobot
            .clickFilterList()
        searchRobot
            .typeTextInView(TestConstants.SEARCH_TERM)
            .pressBack()
            .pressBack()
        setDeviceOrientation(isLandscape = false)
        savedScreenRobot
            .clickItemOnTheList(0)
        savedScreenRobot
            .clickFilterList()
        searchRobot
            .typeTextInView(TestConstants.SEARCH_TERM)
        savedScreenRobot
            .clickItemOnReadingList(0)
            .pressBack()
            .pressBack()
            .pressBack()
            .swipeToDelete(2)
            .verifySavedArticleIsRemoved(TestConstants.SEARCH_TERM)
    }
}
