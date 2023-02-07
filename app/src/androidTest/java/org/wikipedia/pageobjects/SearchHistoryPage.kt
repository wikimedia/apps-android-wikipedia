package org.wikipedia.pageobjects

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItem
import org.hamcrest.CoreMatchers.*
import org.wikipedia.R

class SearchHistoryPage : BasePage() {

    //onboarding page elements
    private val searchCard = withId(R.id.search_card)
    private val searchTextInput = withId(R.id.search_src_text)
    private val resultsItem = withId(R.id.page_list_item_title)
    private val findingsList = withId(R.id.search_results_list)
    private val noResultsLable = withId(R.id.results_text)

    fun tapOnSearchBar() {
        onView(searchCard).perform(click())
    }

    fun typeTextSearch(itemText: String) {
        onView(searchTextInput).perform(typeText(itemText))
    }

    fun tapOnSearchResultItem(itemText: String) {
        waitForResult(itemText)
        onView(findingsList)
            .perform(
                actionOnItem<RecyclerView.ViewHolder>(
                    hasDescendant(withText(itemText)),
                    click()
                )
            );
    }
    fun verifyNoResultFound(): String {
        waitForResult("No results")
        return getText(onView(noResultsLable))
    }

    private fun waitForResult(foundPhrase: String) {
        waitUntilElementsAreDisplayed(
            allOf(
                hasDescendant(withText(foundPhrase)),
                withParent(findingsList)
            )
        )
    }
}
