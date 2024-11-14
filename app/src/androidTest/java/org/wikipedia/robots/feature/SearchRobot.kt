package org.wikipedia.robots.feature

import org.wikipedia.R
import org.wikipedia.base.BaseRobot
import org.wikipedia.base.TestConfig

class SearchRobot : BaseRobot() {
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

    fun clickOnItemFromSearchList(position: Int) = apply {
        clickOnItemInList(R.id.search_results_list, 0)
        delay(TestConfig.DELAY_LARGE)
    }

    fun navigateUp() = apply {
        clickOnDisplayedViewWithContentDescription("Navigate up")
    }

    fun pressBack() = apply {
        goBack()
        delay(TestConfig.DELAY_SHORT)
    }
}
