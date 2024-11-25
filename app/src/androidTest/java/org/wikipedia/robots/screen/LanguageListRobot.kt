package org.wikipedia.robots.screen

import org.wikipedia.R
import org.wikipedia.base.BaseRobot
import org.wikipedia.base.TestConfig

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

    fun pressBack() = apply {
        goBack()
    }
}
