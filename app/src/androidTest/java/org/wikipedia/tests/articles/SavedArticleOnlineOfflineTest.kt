package org.wikipedia.tests.articles

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.TestConstants
import org.wikipedia.base.BaseTest
import org.wikipedia.main.MainActivity
import org.wikipedia.robots.DialogRobot
import org.wikipedia.robots.SystemRobot
import org.wikipedia.robots.feature.SearchRobot
import org.wikipedia.robots.navigation.BottomNavRobot
import org.wikipedia.robots.screen.SavedScreenRobot

@LargeTest
@RunWith(AndroidJUnit4::class)
class SavedArticleOnlineOfflineTest : BaseTest<MainActivity>(
 activityClass = MainActivity::class.java
) {
    private val searchRobot = SearchRobot()
    private val systemRobot = SystemRobot()
    private val bottomNavRobot = BottomNavRobot()
    private val savedScreenRobot = SavedScreenRobot()
    private val dialogRobot = DialogRobot()

    @Test
    fun runTest() {
        systemRobot
            .clickOnSystemDialogWithText("Allow")
        searchRobot
            .tapSearchView()
            .typeTextInView(TestConstants.SEARCH_TERM)
            .longClickOnItemFromSearchList(0)
            .clickSave(action = { isSaved ->
                if (!isSaved) {
                    searchRobot
                        .pressBack()
                }
            })
            .clickSearchInsideSearchFragment()
            .typeTextInView(TestConstants.SEARCH_TERM2)
            .longClickOnItemFromSearchList(0)
            .clickSave(action = { isSaved ->
                if (!isSaved) {
                    searchRobot
                        .pressBack()
                }
            })
            .pressBack()
            .pressBack()
        bottomNavRobot
            .navigateToSavedPage()
        savedScreenRobot
            .clickItemOnTheList(0)
        savedScreenRobot
            .verifySavedArticle("Apple")
            .verifySavedArticle("Orange")
            .clickItemOnReadingList(1)
        systemRobot
            .turnOnAirplaneMode()
        savedScreenRobot
            .pressBack()
            .pressBack()
        bottomNavRobot
            .navigateToExploreFeed()
            .navigateToSavedPage()
        savedScreenRobot
            .clickItemOnTheList(0)
            .verifyImageIsVisible(1)
            .verifyImageIsVisible(2)
            .clickItemOnReadingList(1)
        dialogRobot
            .dismissBigEnglishDialog()
        savedScreenRobot
            .verifyPageIsOffline(context)
        systemRobot
            .turnOffAirplaneMode()
    }
}
