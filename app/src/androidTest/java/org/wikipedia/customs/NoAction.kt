package org.wikipedia.customs

import android.view.View
import androidx.test.espresso.UiController
import org.hamcrest.CoreMatchers
import org.hamcrest.Matcher

import androidx.test.espresso.ViewAction
import org.hamcrest.CoreMatchers.any


class NoAction {
    fun perform(uiController: UiController?, view: View?) { }

    fun getConstraints(): Matcher<View> {
        return CoreMatchers.any(View::class.java)
    }

    fun getDescription(): String {
        return "Dummy action. No real interaction with UI performed."
    }
}