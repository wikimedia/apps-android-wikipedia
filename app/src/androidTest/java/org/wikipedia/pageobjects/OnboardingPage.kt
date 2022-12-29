package org.wikipedia.pageobjects

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.CoreMatchers.allOf
import org.wikipedia.R

class OnboardingPage {

    //onboarding page elements
    private val skipButton = withId(R.id.fragment_onboarding_skip_button)

    fun tapOnSkipButton(){
       onView(allOf(skipButton,
            ViewMatchers.isDisplayed())).perform(click())
        return
    }
}