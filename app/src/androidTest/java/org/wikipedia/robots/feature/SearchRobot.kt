package org.wikipedia.robots.feature

import BaseRobot
import android.util.Log
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
import org.wikipedia.WikipediaApp
import org.wikipedia.base.TestConfig
import org.wikipedia.base.TestThemeColorType
import org.wikipedia.base.TestWikipediaColors
import org.wikipedia.theme.Theme

class SearchRobot : BaseRobot() {
    fun tapSearchView() = apply {
        // Click the Search box
        click.onViewWithText("Search Wikipedia")
        delay(TestConfig.DELAY_SHORT)
    }

    fun clickSearchFromPageView() = apply {
        click.onViewWithId(viewId = R.id.page_toolbar_button_search)
        delay(TestConfig.DELAY_SHORT)
    }

    fun clickSearchContainer() = apply {
        // Click the Search box
        click.onDisplayedView(R.id.search_container)
        delay(TestConfig.DELAY_SHORT)
    }

    fun clickSearchInsideSearchFragment() = apply {
        click.onViewWithId(R.id.search_cab_view)
        delay(TestConfig.DELAY_SHORT)
    }

    fun typeTextInView(searchTerm: String) = apply {
        // Type in our search term
        input.replaceTextInView(androidx.appcompat.R.id.search_src_text, searchTerm)

        // Give the API plenty of time to return results
        delay(TestConfig.DELAY_LARGE)
    }

    fun verifySearchResult(expectedTitle: String) = apply {
        // Make sure one of the results matches the title that we expect
        verify.withTextIsDisplayed(R.id.page_list_item_title, expectedTitle)
    }

    fun verifyHistoryArticle(articleTitle: String) = apply {
        verify.withTextIsDisplayed(R.id.page_list_item_title, articleTitle)
    }

    fun clickFilterHistoryButton() = apply {
        click.onViewWithId(R.id.history_filter)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun removeTextByTappingTrashIcon() = apply {
        onView(withId(androidx.appcompat.R.id.search_close_btn))
            .check(matches(isDisplayed()))
            .perform(click())
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun verifySearchTermIsCleared() = apply {
        verify.viewWithIdAndText(viewId = androidx.appcompat.R.id.search_src_text, text = "")
    }

    fun clickOnItemFromSearchList(position: Int) = apply {
        list.clickOnItemInList(R.id.search_results_list, position)
        delay(TestConfig.DELAY_LARGE)
    }

    fun longClickOnItemFromSearchList(position: Int) = apply {
        list.longClickOnItemInList(R.id.search_results_list, position)
        delay(TestConfig.DELAY_SHORT)
    }

    fun verifyRecentSearchesAppears() = apply {
        verify.viewWithTextDisplayed("Recent searches:")
    }

    fun navigateUp() = apply {
        click.onDisplayedViewWithContentDescription("Navigate up")
    }

    fun checkLanguageAvailability(languageCode: String) = apply {
        val language = WikipediaApp.instance.languageState.getAppLanguageLocalizedName(languageCode) ?: ""
        verify.viewWithIdAndText(viewId = R.id.language_label, text = language)
        delay(TestConfig.DELAY_SHORT)
    }

    fun clickLanguage(languageCode: String) = apply {
        val language = WikipediaApp.instance.languageState.getAppLanguageLocalizedName(languageCode) ?: ""
        click.onDisplayedViewWithText(viewId = R.id.language_label, text = language)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun checkSearchListItemHasRTLDirection() = apply {
        verify.rTLDirectionOfRecyclerViewItem(R.id.search_results_list)
    }

    fun clickSave(action: ((isSaved: Boolean) -> Unit)? = null) = apply {
        try {
            click.onViewWithText("Save")
            delay(TestConfig.DELAY_SHORT)
            action?.invoke(true)
        } catch (e: Exception) {
            Log.e("SearchRobotError:", "Already saved.")
            action?.invoke(false)
        }
    }

    fun pressBack() = apply {
        goBack()
        delay(TestConfig.DELAY_SHORT)
    }

    fun goBackToSearchScreen() = apply {
        pressBack()
        pressBack()
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
        list.clickOnItemInList(R.id.history_list, position)
        delay(TestConfig.DELAY_LARGE)
    }

    fun longClickOnItemFromHistoryList(position: Int) = apply {
        list.longClickOnItemInList(R.id.history_list, position)
        delay(TestConfig.DELAY_LARGE)
    }

    fun assertColorOfTitleInTheSearchList(position: Int, theme: Theme) = apply {
        val color = TestWikipediaColors.getGetColor(theme, TestThemeColorType.PRIMARY)
        verify.assertColorForChildItemInAList(
            listId = R.id.search_results_list,
            childItemId = R.id.page_list_item_title,
            position = position,
            colorResId = color
        )
    }

    fun assertColorOfTitleInTheHistoryList(position: Int, theme: Theme) = apply {
        val color = TestWikipediaColors.getGetColor(theme, TestThemeColorType.PRIMARY)
        verify.assertColorForChildItemInAList(
            listId = R.id.history_list,
            childItemId = R.id.page_list_item_title,
            position = position,
            colorResId = color
        )
    }
}
