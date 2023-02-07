package org.wikipedia.pageobjects
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers

open class NavbarPage:BasePage(){
    // navigation bar elements
    private val searchBtn = ViewMatchers.withContentDescription("Search")
    private val moreBtn = ViewMatchers.withContentDescription("More")
    private val settingsBtn = ViewMatchers.withText("Settings")
    private val donateBtn = ViewMatchers.withText("Donate")

    fun tapOnDonateBtn(){
        onView(donateBtn).perform(ViewActions.click())
    }
    fun tapOnNavSearchBtn() {
        onView(searchBtn).perform(ViewActions.click())
    }
    fun tapOnMoreBtn() {
        onView(moreBtn).perform(ViewActions.click())
    }
    fun tapOnSettingsBtn() {
        onView(settingsBtn).perform(ViewActions.click())
    }
}