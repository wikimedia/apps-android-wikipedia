package org.wikipedia.base.actions

import android.view.View
import android.widget.EditText
import androidx.annotation.IdRes
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf

class InputActions {
    fun replaceTextInView(@IdRes viewId: Int, text: String) {
        onView(allOf(withId(viewId), isDisplayed()))
            .perform(replaceText(text), closeSoftKeyboard())
    }

    fun typeInEditText(@IdRes viewId: Int, text: String) {
        onView(withId(viewId))
            .perform(typeText(text))
    }

    fun selectText(@IdRes viewId: Int, start: Int, end: Int) {
        onView(withId(viewId))
            .perform(object : ViewAction {
                override fun getConstraints(): Matcher<View> {
                    return allOf(isDisplayed(), isAssignableFrom(EditText::class.java))
                }

                override fun getDescription(): String {
                    return "Select text from $start to $end"
                }

                override fun perform(uiController: UiController, view: View) {
                    val editText = view as EditText
                    editText.setSelection(start, end)
                }
            })
    }

    fun getCurrentText(@IdRes viewId: Int): String {
        var currentText = ""
        onView(withId(viewId))
            .perform(object : ViewAction {
                override fun getConstraints(): Matcher<View> {
                    return allOf(isDisplayed(), isAssignableFrom(EditText::class.java))
                }

                override fun getDescription(): String {
                    return "Get current text from EditText"
                }

                override fun perform(uiController: UiController, view: View) {
                    val editText = view as EditText
                    currentText = editText.text.toString()
                }
            })
        return currentText
    }

    fun closeKeyboard(@IdRes viewId: Int) {
        onView(withId(viewId))
            .perform(closeSoftKeyboard())
    }
}
