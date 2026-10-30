package org.wikipedia.robots

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.wikipedia.search.SEARCH_LIST_TAG

class SearchRobot(
    private val composeTestRule: ComposeTestRule,
    private val resultsTimeoutMillis: Long = DEFAULT_RESULTS_TIMEOUT_MILLIS
) {
    fun typeQuery(query: String) = apply {
        onView(withId(androidx.appcompat.R.id.search_src_text)).perform(replaceText(query))
    }

    fun waitForResults() = apply {
        composeTestRule.waitUntil(timeoutMillis = resultsTimeoutMillis) {
            composeTestRule.onAllNodesWithTag(FIRST_RESULT_TAG).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /**
     * The title of the top result, read off the screen so the test never has to assume what
     * the live search returns for a query.
     */
    fun firstResultTitle(): String {
        val node = composeTestRule.onNodeWithTag(FIRST_RESULT_TAG).fetchSemanticsNode()
        val texts = node.config.getOrNull(SemanticsProperties.Text).orEmpty()
        return checkNotNull(texts.firstOrNull()?.text) {
            "The first search result has no text to read a title from."
        }
    }

    fun tapFirstResult() = apply {
        composeTestRule.onNodeWithTag(FIRST_RESULT_TAG).performClick()
    }

    private companion object {
        const val DEFAULT_RESULTS_TIMEOUT_MILLIS = 30_000L
        const val FIRST_RESULT_TAG = SEARCH_LIST_TAG + "0"
    }
}
