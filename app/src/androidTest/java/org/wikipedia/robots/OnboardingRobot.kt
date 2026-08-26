package org.wikipedia.robots

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import org.wikipedia.R
import org.wikipedia.base.utils.assertTextColor

class OnboardingRobot(
    private val composeTestRule: ComposeTestRule,
    private val context: Context
) {
    fun assertIntroScreenIsDisplayed() = apply {
        composeTestRule
            .onNodeWithText(context.getString(R.string.onboarding_fresh_install_knowledge_title))
            .assertIsDisplayed()
    }

    fun assertDataPrivacyScreenIsDisplayed() = apply {
        composeTestRule
            .onNodeWithText(context.getString(R.string.onboarding_data_privacy_title))
            .assertIsDisplayed()
    }

    fun assertDataPrivacyTitleColor(color: Color) = apply {
        composeTestRule
            .onNodeWithText(context.getString(R.string.onboarding_data_privacy_title))
            .assertTextColor(color)
    }

    fun assertLanguagesScreenIsDisplayed() = apply {
        composeTestRule
            .onNodeWithText(context.getString(R.string.onboarding_app_languages_title))
            .assertIsDisplayed()
    }

    fun tapForward() = apply {
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.nav_item_forward))
            .performClick()
    }

    fun tapAddOrEditLanguages() = apply {
        composeTestRule
            .onNodeWithText(context.getString(R.string.onboarding_app_languages_add_button))
            .performScrollTo()
            .performClick()
    }

    fun assertLanguageIsNotDisplayed(languageName: String) = apply {
        composeTestRule
            .onNodeWithText(languageName)
            .assertIsNotDisplayed()
    }

    fun assertLanguageIsDisplayed(languageName: String) = apply {
        composeTestRule
            .onNodeWithTag(ONBOARDING_LANGUAGE_LIST_TAG)
            .performScrollToNode(hasText(languageName))
        composeTestRule
            .onNodeWithText(languageName)
            .assertIsDisplayed()
    }

    private companion object {
        const val ONBOARDING_LANGUAGE_LIST_TAG = "onboarding_language_list"
    }
}
