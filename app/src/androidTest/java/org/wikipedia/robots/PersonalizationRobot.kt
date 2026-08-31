package org.wikipedia.robots

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.wikipedia.R
import org.wikipedia.extensions.getString
import org.wikipedia.feed.personalization.homepreference.HomePreferenceType
import org.wikipedia.topics.ArticleTopic

class PersonalizationRobot(
    private val composeTestRule: ComposeTestRule,
    private val context: Context
) {
    fun assertCuriosityScreenIsDisplayed() =
        assertTextIsDisplayed(R.string.explore_feed_onboarding_curiosity_title)

    fun assertInterestsScreenIsDisplayed() =
        assertTextIsDisplayed(R.string.recommended_reading_list_interest_pick_title)

    fun assertHomePreferenceScreenIsDisplayed() =
        assertTextIsDisplayed(R.string.explore_feed_preference_selection_screen_title)

    fun assertArticleIsDisplayed(title: String) = assertTextIsDisplayed(title)

    fun assertCommunityContentIsDisplayed(description: String) = assertTextIsDisplayed(description)

    fun assertSelectedCountIsDisplayed(count: Int) =
        assertTextIsDisplayed(context.getString(R.string.multi_select_items_selected, count))

    fun assertPersonalizedPreferenceIsEnabled() = apply {
        personalizedPreferenceRadioButton().assertIsEnabled()
    }

    fun tapNext() = apply {
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.onboarding_next))
            .performClick()
    }

    fun tapTopic(topic: ArticleTopic, languageCode: String = DEFAULT_LANGUAGE_CODE) = apply {
        composeTestRule
            .onNodeWithText(context.getString(languageCode, topic.msgKey))
            .performClick()
    }

    fun tapArticle(title: String) = apply {
        waitForText(title)
        composeTestRule
            .onNodeWithText(title)
            .performClick()
    }

    fun tapPersonalizedPreference() = apply {
        personalizedPreferenceRadioButton().performClick()
    }

    private fun personalizedPreferenceRadioButton() = composeTestRule.onNode(
        isSelectable() and hasAnyAncestor(
            hasText(context.getString(HomePreferenceType.PERSONALIZED.titleRes))
        )
    )

    private fun assertTextIsDisplayed(@StringRes stringRes: Int) =
        assertTextIsDisplayed(context.getString(stringRes))

    private fun assertTextIsDisplayed(text: String) = apply {
        waitForText(text)
        composeTestRule
            .onNodeWithText(text)
            .assertIsDisplayed()
    }

    private fun waitForText(text: String) {
        composeTestRule.waitUntil(timeoutMillis = TEXT_TIMEOUT_MILLIS) {
            composeTestRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private companion object {
        const val TEXT_TIMEOUT_MILLIS = 10_000L
        const val DEFAULT_LANGUAGE_CODE = "en"
    }
}
