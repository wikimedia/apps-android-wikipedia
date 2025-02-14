package org.wikipedia.base.actions

import android.os.SystemClock
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.annotation.IdRes
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.doubleClick
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isDisplayingAtLeast
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.wikipedia.base.utils.hasText
import org.wikipedia.base.utils.scrollAndClick
import org.wikipedia.base.utils.waitForAsyncLoading

class ClickActions {
    fun onViewWithIdAndContainsString(@IdRes viewId: Int, text: String) {
        onView(
            allOf(
            withId(viewId),
            hasText(text),
        )
        ).perform(scrollAndClick())
    }

    fun onViewWithId(@IdRes viewId: Int) {
        onView(withId(viewId)).perform(click())
    }

    fun onDisplayedView(@IdRes viewId: Int) {
        onView(allOf(withId(viewId), isDisplayed())).perform(click())
    }

    fun onDisplayedViewWithText(@IdRes viewId: Int, text: String) {
        onView(allOf(withId(viewId), withText(text), isDisplayed())).perform(click())
    }

    fun onDisplayedViewWithContentDescription(description: String) {
        onView(allOf(withContentDescription(description), isDisplayed())).perform(click())
    }

    fun onDisplayedViewWithIdAnContentDescription(
        @IdRes viewId: Int,
        description: String
    ) {
        onView(allOf(withId(viewId), withContentDescription(description), isDisplayed())).perform(
            click()
        )
    }

    fun onViewWithText(text: String) {
        onView(withText(text)).perform(click())
    }

    fun doubleClickOnViewWithId(@IdRes viewId: Int) {
        onView(
            allOf(
                withId(viewId),
                isDisplayed()
            )
        ).perform(doubleClick())
    }

    fun ifVisible(): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return isDisplayingAtLeast(90)
            }

            override fun getDescription(): String {
                return "Click if Visible"
            }

            override fun perform(uiController: UiController, view: View) {
                if (view.isShown && view.isEnabled) {
                    view.performClick()
                    uiController.loopMainThreadForAtLeast(500)
                }
            }
        }
    }

    fun xyPosition(x: Int, y: Int): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return isDisplayed()
            }

            override fun getDescription(): String {
                return "Click at coordinates: $x, $y"
            }

            override fun perform(uiController: UiController, view: View) {
                uiController.injectMotionEvent(
                    MotionEvent.obtain(
                        SystemClock.uptimeMillis(),
                        SystemClock.uptimeMillis(),
                        MotionEvent.ACTION_DOWN,
                        x.toFloat(),
                        y.toFloat(),
                        0
                    ))

                uiController.injectMotionEvent(
                    MotionEvent.obtain(
                        SystemClock.uptimeMillis(),
                        SystemClock.uptimeMillis(),
                        MotionEvent.ACTION_UP,
                        x.toFloat(),
                        y.toFloat(),
                        0
                    ))
            }
        }
    }

    fun ifDialogShown(
        dialogText: String,
        errorString: String
    ) {
        try {
            onView(withText(dialogText))
                .perform(waitForAsyncLoading())
                .inRoot(isDialog())
                .perform(click())
        } catch (e: NoMatchingViewException) {
            Log.e("BaseRobot", "$errorString")
        } catch (e: Exception) {
            Log.e("BaseRobot", "Unexpected Error: ${e.message}")
        }
    }
}
