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
        search(SEARCH_QUERY_WATERMELON)
        readingListRobot
            .saveArticleToReadingList()
            .pressBack()
            .pressBack()
            .pressBack()
        bottomNavRobot
            .navigateToSavedPage()
        readingListRobot
            .clickOnReadingLists(LIST_NAME_SAVED)
            .verifySavedArticleExists(SEARCH_QUERY_WATERMELON)
            .pressBack()
        bottomNavRobot
            .navigateToExploreFeed()
        // 2. article saved to a custom list should be in the custom list in the reading list
        search(SEARCH_QUERY_LEMON)
        readingListRobot
            .saveArticleToReadingList()
            .addToReadingList(context)
            .typeNameOfTheList(LIST_NAME_NEW, context)
            .saveTheList(context)
            .pressBack()
            .pressBack()
            .pressBack()
        dialogRobot
            .dismissPromptLogInToSyncDialog(context)
        bottomNavRobot
            .navigateToSavedPage()
        readingListRobot
            .clickOnReadingLists(LIST_NAME_NEW)
            .verifySavedArticleExists(SEARCH_QUERY_LEMON)
            .pressBack()
         // 3. unsaved article should not be in the "Saved" or "custom list" in the reading list
        bottomNavRobot
            .navigateToSavedPage()
        readingListRobot
            .clickOnReadingLists(LIST_NAME_SAVED)
            .clickOnReadingListItem(1)
            .saveArticleToReadingList()
            .removeArticleList(LIST_NAME_SAVED)
            .pressBack()
            .verifySavedArticleDoesNotExists(SEARCH_QUERY_WATERMELON)
            .pressBack()
            .clickOnReadingLists(LIST_NAME_NEW)
            .clickOnReadingListItem(1)
            .saveArticleToReadingList()
            .removeArticleList(LIST_NAME_NEW)
            .pressBack()
            .verifySavedArticleDoesNotExists(SEARCH_QUERY_LEMON)
            .pressBack()
        // 4. can delete lists
        readingListRobot
            .longClickReadingLists(1)
            .deleteList(context)
        dialogRobot
            .click("OK")
        readingListRobot
            .verifyListDoesNotExist(LIST_NAME_NEW)
    }

    private fun search(title: String) {
        searchRobot
            .tapSearchView()
            .typeTextInView(title)
            .clickOnItemFromSearchList(0)
    }

    companion object {
        private const val LIST_NAME_SAVED = "Saved"
        private const val LIST_NAME_NEW = "new"
        private const val SEARCH_QUERY_WATERMELON = "Watermelon"
        private const val SEARCH_QUERY_LEMON = "Lemon"
    }
}
