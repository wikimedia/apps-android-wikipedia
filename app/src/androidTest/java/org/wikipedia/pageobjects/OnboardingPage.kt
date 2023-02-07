package org.wikipedia.pageobjects

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.action.ViewActions.*
import org.hamcrest.CoreMatchers.allOf
import org.wikipedia.R

class OnboardingPage {

    private val skipButton = withId(R.id.fragment_onboarding_skip_button)

    fun tapOnSkipButton(){
       onView(allOf(skipButton)).perform(click())
    }
}