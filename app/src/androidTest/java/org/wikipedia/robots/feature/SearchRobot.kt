package org.wikipedia.robots.feature

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers.allOf
import org.wikipedia.R
import org.wikipedia.base.BaseRobot
import org.wikipedia.base.TestConfig
import org.wikipedia.base.TestThemeColorType
import org.wikipedia.base.TestWikipediaColors
import org.wikipedia.theme.Theme

class SearchRobot : BaseRobot() {
    fun tapSearchView() = apply {
        // Click the Search box
        clickOnViewWithText("Search Wikipedia")
        delay(TestConfig.DELAY_SHORT)
    }

    fun clickSearchContainer() = apply {
        // Click the Search box
        clickOnDisplayedView(R.id.search_container)
        delay(TestConfig.DELAY_SHORT)
    }

    fun typeTextInView(searchTerm: String) = apply {
        // Type in our search term
        typeTextInView(androidx.appcompat.R.id.search_src_text, searchTerm)

        // Give the API plenty of time to return results
        delay(TestConfig.DELAY_LARGE)
    }

    fun verifySearchResult(expectedTitle: String) = apply {
        // Make sure one of the results matches the title that we expect
        checkWithTextIsDisplayed(R.id.page_list_item_title, expectedTitle)
    }

    fun verifyHistoryArticle(articleTitle: String) = apply {
        checkWithTextIsDisplayed(R.id.page_list_item_title, articleTitle)
    }

    fun clickFilterHistoryButton() = apply {
        clickOnViewWithId(R.id.history_filter)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun removeTextByTappingTrashIcon() = apply {
        onView(withId(androidx.appcompat.R.id.search_close_btn))
            .check(matches(isDisplayed()))
            .perform(click())
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun verifySearchTermIsCleared() = apply {
        checkViewWithIdAndText(viewId = androidx.appcompat.R.id.search_src_text, text = "")
    }

    fun clickOnItemFromSearchList(position: Int) = apply {
        clickOnItemInList(R.id.search_results_list, position)
        delay(TestConfig.DELAY_LARGE)
    }

    fun verifyRecentSearchesAppears() = apply {
        checkViewWithTextDisplayed("Recent searches:")
    }

    fun navigateUp() = apply {
        clickOnDisplayedViewWithContentDescription("Navigate up")
    }

    fun checkLanguageAvailability(language: String) = apply {
        checkViewWithIdAndText(viewId = R.id.language_label, text = language)
    }

    fun clickLanguage(language: String) = apply {
        clicksOnDisplayedViewWithText(viewId = R.id.language_label, text = language)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun checkSearchListItemHasRTLDirection() = apply {
        checkRTLDirectionOfRecyclerViewItem(R.id.search_results_list)
    }

    fun pressBack() = apply {
        goBack()
        delay(TestConfig.DELAY_SHORT)
    }

    fun goBackToSearchScreen() = apply {
        pressBack()
        pressBack()
    }

    fun dismissDialogIfShown() = apply {
        performIfDialogShown(dialogText = "No, thanks", action = {
            clickOnViewWithText("No, thanks")
        })
    }

    fun backToHistoryScreen() = apply {
        pressBack()
        pressBack()
        pressBack()
    }

    fun swipeToDelete(position: Int, title: String) = apply {
        onView(withId(R.id.history_list))
            .perform(
                RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                    position,
                    ViewActions.swipeLeft()
                )
            )
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun verifyArticleRemoved(title: String) = apply {
        onView(allOf(withId(R.id.page_list_item_title), withText(title)))
            .check(doesNotExist())
    }

    fun clickOnItemFromHistoryList(position: Int) = apply {
        clickOnItemInList(R.id.history_list, position)
        delay(TestConfig.DELAY_LARGE)
    }

    fun longClickOnItemFromHistoryList(position: Int) = apply {
        longClickOnItemInList(R.id.history_list, position)
        delay(TestConfig.DELAY_LARGE)
    }

    fun assertColorOfTitleInTheSearchList(position: Int, theme: Theme) = apply {
        val color = TestWikipediaColors.getGetColor(theme, TestThemeColorType.PRIMARY)
        assertColorForChildItemInAList(
            listId = R.id.search_results_list,
            childItemId = R.id.page_list_item_title,
            position = position,
            colorResId = color
        )
    }

    fun assertColorOfTitleInTheHistoryList(position: Int, theme: Theme) = apply {
        val color = TestWikipediaColors.getGetColor(theme, TestThemeColorType.PRIMARY)
        assertColorForChildItemInAList(
            listId = R.id.history_list,
            childItemId = R.id.page_list_item_title,
            position = position,
            colorResId = color
        )
    }
}
