package org.wikipedia.pageobjects
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers


open class NavbarPage {
    // navigation bar elements
    private val navSearchBtn = ViewMatchers.withContentDescription("Search")
    private val navMoreBtn = ViewMatchers.withContentDescription("More")

    fun tapOnNavSearchButton() {
        onView(navSearchBtn).perform(ViewActions.click())
        return
    }

    fun tapOnNavMoreButton() {
        onView(navSearchBtn).perform(ViewActions.click())
        return
    }
}