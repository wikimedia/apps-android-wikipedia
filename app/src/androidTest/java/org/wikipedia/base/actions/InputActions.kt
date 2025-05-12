package org.wikipedia.base.actions

import androidx.annotation.IdRes
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.hamcrest.Matchers.allOf

class InputActions {
    fun typeTextInView(@IdRes viewId: Int, text: String) {
        onView(allOf(withId(viewId), isDisplayed()))
            .perform(replaceText(text), closeSoftKeyboard())
    }
}
