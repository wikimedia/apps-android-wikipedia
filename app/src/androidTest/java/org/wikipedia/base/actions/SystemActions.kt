package org.wikipedia.base.actions

import android.app.Activity
import android.util.Log
import androidx.annotation.IdRes
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not

class SystemActions {
    fun dismissTooltipIfAny(activity: Activity, @IdRes viewId: Int) = apply {
        onView(allOf(withId(viewId))).inRoot(withDecorView(not(Matchers.`is`(activity.window.decorView))))
            .perform(click())
    }

    fun performActionIfSnackbarVisible(
        text: String,
        action: () -> Unit
    ) = apply {
        try {
            onView(
                allOf(
                    withId(com.google.android.material.R.id.snackbar_text),
                    withText(text)
                )
            ).check(matches(isDisplayed()))
            action.invoke()
        } catch (e: NoMatchingViewException) {
            Log.e("BaseRobot", "No snackbar visible, skipping action")
        } catch (e: Exception) {
            Log.e("BaseRobot", "Unexpected error: ${e.message}")
        }
    }
}
