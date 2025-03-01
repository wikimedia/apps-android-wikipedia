package org.wikipedia.base.utils

import android.graphics.Rect
import android.view.View
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isDisplayingAtLeast
import org.hamcrest.Description
import org.hamcrest.Matcher

fun waitForAsyncLoading(): ViewAction {
    return object : ViewAction {
        override fun getConstraints(): Matcher<View> {
            return isDisplayed()
        }

        override fun getDescription(): String {
            return "wait for async loading"
        }

        override fun perform(uiController: UiController, view: View?) {
            uiController.loopMainThreadForAtLeast(2000)
        }
    }
}

fun hasText(text: String) = object : BoundedMatcher<View, TextView>(TextView::class.java) {
    override fun describeTo(description: Description?) {
        description?.appendText("contains text $text")
    }

    override fun matchesSafely(item: TextView): Boolean {
        return item.text.toString().contains(text, ignoreCase = true)
    }
}

fun scrollAndClick() = object : ViewAction {
    override fun getConstraints(): Matcher<View> {
        return isDisplayingAtLeast(10)
    }

    override fun getDescription(): String {
        return "Scroll item into view and click"
    }

    override fun perform(uiController: UiController, view: View) {
        if (!isDisplayingAtLeast(90).matches(view)) {
            view.requestRectangleOnScreen(
                Rect(0, 0, view.width, view.height),
                true
            )
            uiController.loopMainThreadForAtLeast(500)
        }

        view.performClick()
        uiController.loopMainThreadForAtLeast(1000)
    }
}

fun matchesAtPosition(position: Int, @IdRes targetViewId: Int, assertion: (View) -> Unit): ViewAssertion {
    return ViewAssertion { view, noViewFoundException ->
        if (view !is RecyclerView) {
            throw IllegalStateException("The asserted view is not RecyclerView")
        }

        val itemView = view.findViewHolderForAdapterPosition(position)?.itemView
            ?: throw IllegalStateException("No view with id: $targetViewId")
        val targetView = itemView.findViewById<View>(targetViewId)
            ?: throw IllegalStateException("No view with id: $targetViewId")
        assertion(targetView)
    }
}
