package org.wikipedia.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.base.BaseTest
import org.wikipedia.main.MainActivity
import org.wikipedia.robots.DialogRobot
import org.wikipedia.robots.SystemRobot
import org.wikipedia.robots.feature.LoginRobot
import org.wikipedia.robots.feature.ReadingListRobot
import org.wikipedia.robots.feature.SearchRobot
import org.wikipedia.robots.navigation.BottomNavRobot

@LargeTest
@RunWith(AndroidJUnit4::class)
class ReadingListsTest : BaseTest<MainActivity>(
 activityClass = MainActivity::class.java
) {
    private val systemRobot = SystemRobot()
    private val bottomNavRobot = BottomNavRobot()
    private val loginRobot = LoginRobot()
    private val searchRobot = SearchRobot()
    private val readingListRobot = ReadingListRobot()
    private val dialogRobot = DialogRobot()

    @Test
    fun runTest() {
        systemRobot
            .clickOnSystemDialogWithText("Allow")
        // 1. saved article should be in the "Saved" reading list
        search("watermelon")
        readingListRobot
            .saveArticleToReadingList()
            .pressBack()
            .pressBack()
            .pressBack()
        bottomNavRobot
            .navigateToSavedPage()
        readingListRobot
            .clickOnList("Saved")
            .verifySavedArticleExists("Watermelon")
            .pressBack()
        bottomNavRobot
            .navigateToExploreFeed()
        // 2. article saved to a custom list should be in the custom list in the reading list
        search("lemon")
        readingListRobot
            .saveArticleToReadingList()
            .addToReadingList(context)
            .typeNameOfTheList("new", context)
            .saveTheList(context)
            .pressBack()
            .pressBack()
            .pressBack()
        dialogRobot
            .dismissPromptLogInToSyncDialog(context)
        bottomNavRobot
            .navigateToSavedPage()
        readingListRobot
            .clickOnList("new")
            .verifySavedArticleExists("lemon")

        // 3. can delete lists
        // 4. unsaved article should not be in the "Saved" or "custom list" in the reading list
    }

    private fun search(title: String) {
        searchRobot
            .tapSearchView()
            .typeTextInView(title)
            .clickOnItemFromSearchList(0)
    }
}
