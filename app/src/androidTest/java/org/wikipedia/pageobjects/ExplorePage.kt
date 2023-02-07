package org.wikipedia.pageobjects

import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.Matcher
import org.wikipedia.R
class ExplorePage: BasePage()  {

    private val wikiLogo = withId(R.id.main_toolbar_wordmark)
    private val searchbar = withId(R.id.search_container)

    fun tapOnSearchBar(){
        onView(searchbar).perform(ViewActions.click())
    }
}