package org.wikipedia.base.actions

import androidx.annotation.IdRes
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.hamcrest.Matchers.allOf

class SwipeActions {
    fun left(@IdRes viewId: Int) {
        onView(withId(viewId)).perform(ViewActions.swipeLeft())
    }

    fun right(@IdRes viewId: Int) {
        onView(withId(viewId)).perform(ViewActions.swipeRight())
    }

    fun up(@IdRes viewId: Int) {
        onView(allOf(withId(viewId))).perform(ViewActions.swipeUp())
    }
}
