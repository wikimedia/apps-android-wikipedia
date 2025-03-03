package org.wikipedia.base.actions

import android.graphics.Rect
import android.view.View
import androidx.annotation.IdRes
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayingAtLeast
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.wikipedia.base.utils.scrollAndClick

class ScrollActions {

    fun toView(@IdRes viewId: Int) {
        onView(withId(viewId)).perform(scrollTo())
    }

    fun toViewAndClick(@IdRes viewId: Int) {
        onView(withId(viewId)).perform(scrollTo(), click())
    }

    fun toTextAndClick(text: String) {
        onView(allOf(withText(text))).perform(scrollTo(), click())
    }

    fun toViewAndMakeVisibleAndClick(@IdRes viewId: Int, @IdRes parentViewId: Int) {
        onView(allOf(withId(viewId), isDescendantOfA(withId(parentViewId))))
            .perform(scrollAndClick())
    }

    fun toViewAndMakeVisibleAndLongClick(@IdRes viewId: Int, @IdRes parentViewId: Int) {
        onView(allOf(withId(viewId), isDescendantOfA(withId(parentViewId))))
            .perform(scrollAndLongClick())
    }

    private fun scrollAndLongClick() = object : ViewAction {
        override fun getConstraints(): Matcher<View> {
            return isDisplayingAtLeast(10)
        }

        override fun getDescription(): String {
            return "Scroll item into view and long click"
        }

        override fun perform(uiController: UiController, view: View) {
            if (!isDisplayingAtLeast(90).matches(view)) {
                view.requestRectangleOnScreen(
                    Rect(0, 0, view.width, view.height),
                    true
                )
                uiController.loopMainThreadForAtLeast(500)
            }

            view.performLongClick()
            uiController.loopMainThreadForAtLeast(1000)
        }
    }
}
