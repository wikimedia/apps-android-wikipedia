package org.wikipedia.robots

import android.content.Context
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import org.wikipedia.R

class AddLanguagesRobot(
    private val composeTestRule: ComposeTestRule,
    private val context: Context
) {
    fun searchForLanguage(languageName: String) = apply {
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.search_icon_content_description))
            .performClick()
        composeTestRule
            .onNodeWithTag(SEARCH_TEXT_FIELD_TAG)
            .performTextInput(languageName)
    }

    fun selectLanguage(languageName: String) = apply {
        composeTestRule
            .onNodeWithTag(languageName)
            .performScrollTo()
            .performClick()
    }

    private companion object {
        const val SEARCH_TEXT_FIELD_TAG = "search_text_field"
    }
}
