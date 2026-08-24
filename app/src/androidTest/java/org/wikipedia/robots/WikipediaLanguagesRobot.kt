package org.wikipedia.robots

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.wikipedia.R

class WikipediaLanguagesRobot {
    fun tapAddLanguage() = apply {
        onView(withId(R.id.wikipedia_languages_recycler))
            .perform(
                RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                    hasDescendant(withText(R.string.wikipedia_languages_add_language_text)),
                    click()
                )
            )
    }

    fun navigateBack() = apply {
        pressBack()
    }
}
