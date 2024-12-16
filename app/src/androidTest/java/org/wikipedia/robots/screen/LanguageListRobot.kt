package org.wikipedia.robots.screen

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers.allOf
import org.wikipedia.R
import org.wikipedia.base.BaseRobot
import org.wikipedia.base.ColorAssertions
import org.wikipedia.base.TestConfig
import org.wikipedia.base.TestThemeColorType
import org.wikipedia.base.TestWikipediaColors
import org.wikipedia.theme.Theme

class LanguageListRobot : BaseRobot() {

    fun addNewLanguage() = apply {
        scrollToRecyclerView(
            recyclerViewId = R.id.wikipedia_languages_recycler,
            title = "Add language",
            textViewId = R.id.wiki_language_title
        )
        clickOnViewWithText("Add language")
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun assertAddLanguageTextColor(theme: Theme) = apply {
        val color = TestWikipediaColors.getGetColor(theme, TestThemeColorType.PROGRESSIVE)
        scrollToRecyclerView(
            recyclerViewId = R.id.wikipedia_languages_recycler,
            title = "Add language",
            textViewId = R.id.wiki_language_title
        )
        onView(allOf(
            withId(R.id.wiki_language_title),
            withText("Add language")
        )).check(ColorAssertions.hasColor(color))
    }

    fun openSearchLanguage() = apply {
        clickOnViewWithId(R.id.menu_search_language)
        delay(TestConfig.DELAY_SHORT)
    }

    fun scrollToLanguageAndClick(title: String) = apply {
        scrollToRecyclerView(
            recyclerViewId = R.id.languages_list_recycler,
            title = title,
            textViewId = R.id.language_subtitle,
        )
        clicksOnDisplayedViewWithText(viewId = R.id.language_subtitle, title)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun assertJapaneseLanguageTextColor(theme: Theme) = apply {
        val color = TestWikipediaColors.getGetColor(theme, TestThemeColorType.SECONDARY)
        onView(
            allOf(
                withId(R.id.language_subtitle),
                withText("Japanese")
            )
        ).check(ColorAssertions.hasColor(color))
    }

    fun pressBack() = apply {
        goBack()
    }
}
