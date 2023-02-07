package org.wikipedia.pageobjects
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.CoreMatchers.allOf
import org.wikipedia.R
import androidx.test.espresso.intent.matcher.IntentMatchers.*
import androidx.test.espresso.intent.matcher.UriMatchers.hasHost

open class SettingsPage:BasePage(){
    private val recyclerView = withId(R.id.recycler_view)

        fun tapSelectedSection(selectedSection: String) {
        onView(recyclerView).perform(ViewActions.swipeUp())
            onView(withText(selectedSection)).perform(click())
        }

        fun isCorrectIntentSendForSettings (): Boolean {
            val termsOfUseUrl = "https://foundation.wikimedia.org/wiki/Terms_of_Use"
            val termsTabHost = "foundation.wikimedia.org"

           return  try {
                Intents.intended(allOf(hasData(termsOfUseUrl),hasData(hasHost(termsTabHost))))
                true
            } catch (e: AssertionError) {
                false
            }
        }
}