package org.wikipedia.tests.articles

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.TestConstants
import org.wikipedia.base.BaseTest
import org.wikipedia.main.MainActivity
import org.wikipedia.robots.SystemRobot
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
    private val bottomNavRobot = BottomNavRobot()
    private val savedScreenRobot = SavedScreenRobot()

    @Test
    fun runTest() {
        setDeviceOrientation(isLandscape = false)
        systemRobot
            .clickOnSystemDialogWithText("Allow")
        // TODO: update steps
        // 1. Go to search screen and search for an article
        // 2. Long click on the first article and click on save button
        // 3. Go to explore feed and long click on the featured article card and click on save button
        setDeviceOrientation(isLandscape = true)
        bottomNavRobot
            .navigateToSavedPage()
        savedScreenRobot
            .clickFilterList()
        searchRobot
            .typeTextInView(TestConstants.SEARCH_TERM)
        savedScreenRobot
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
            .closeFilterList()
            .swipeToDelete(2)
            .verifySavedArticleIsRemoved(TestConstants.SEARCH_TERM)
    }
}
