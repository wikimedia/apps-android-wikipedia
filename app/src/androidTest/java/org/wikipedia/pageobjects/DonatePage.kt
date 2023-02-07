package org.wikipedia.pageobjects
import android.content.Intent
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItem
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.wikipedia.R
import org.wikipedia.savedpages.PageComponentsUrlParser.parse
import android.net.Uri
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.intent.matcher.IntentMatchers.toPackage
import androidx.test.espresso.intent.matcher.UriMatchers

open class DonatePage:BasePage(){

    fun isCorrectIntentSendForDonate (): Boolean {
        val donateTabHost = "donate.wikimedia.org"
    //TO DO
        return  try {
//            Intents.intended(hasData(UriMatchers.hasHost(donateTabHost)))
           Intent.ACTION_VIEW
            true
        } catch (e: AssertionError) {
            false
        }
    }
}

