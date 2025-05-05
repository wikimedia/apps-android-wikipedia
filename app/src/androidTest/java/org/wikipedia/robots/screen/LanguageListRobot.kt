package org.wikipedia.robots.screen

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.core.content.ContextCompat
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers.allOf
import org.wikipedia.R
import org.wikipedia.base.TestConfig
import org.wikipedia.base.TestThemeColorType
import org.wikipedia.base.TestWikipediaColors
import org.wikipedia.base.base.BaseRobot
import org.wikipedia.base.utils.ColorAssertions
import org.wikipedia.base.utils.assertTextColor
import org.wikipedia.theme.Theme

class LanguageListRobot : BaseRobot() {

    fun addNewLanguage() = apply {
        list.scrollToRecyclerView(
            recyclerViewId = R.id.wikipedia_languages_recycler,
            title = "Add language",
            textViewId = R.id.wiki_language_title
        )
        click.onViewWithText("Add language")
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun assertAddLanguageTextColor(theme: Theme) = apply {
        val color = TestWikipediaColors.getGetColor(theme, TestThemeColorType.PROGRESSIVE)
        list.scrollToRecyclerView(
            recyclerViewId = R.id.wikipedia_languages_recycler,
            title = "Add language",
            textViewId = R.id.wiki_language_title
        )
        onView(allOf(
            withId(R.id.wiki_language_title),
            withText("Add language")
        )).check(ColorAssertions.hasColor(color))
    }

    fun openSearchLanguage(context: Context) = apply {
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.search_icon_content_description))
            .performClick()
        delay(TestConfig.DELAY_SHORT)
    }

    fun typeInSearchView(text: String) = apply {
        composeTestRule.onNodeWithTag("search_text_field")
            .performTextInput(text)
        delay(TestConfig.DELAY_LARGE)
    }

    fun scrollToLanguageAndClick(title: String) = apply {
        composeTestRule.onNode(hasTestTag("language_list"))
            .performScrollToNode(hasTestTag(title))

        composeTestRule.onNodeWithTag(title)
            .performClick()
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun assertJapaneseLanguageTextColor(context: Context, theme: Theme) = apply {
        val colorRes = TestWikipediaColors.getGetColor(theme, TestThemeColorType.PRIMARY)
        val color = ContextCompat.getColor(context, colorRes)
        composeTestRule
            .onNodeWithTag("Japanese")
            .assertTextColor(Color(color))
    }

    fun pressBack() = apply {
        goBack()
    }
}
