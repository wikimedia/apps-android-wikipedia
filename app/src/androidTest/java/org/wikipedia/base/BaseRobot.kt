package org.wikipedia.base

import androidx.annotation.IdRes
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers.allOf
import org.wikipedia.TestUtil.waitOnId
import java.util.concurrent.TimeUnit

abstract class BaseRobot {

    protected fun clickOnDisplayedView(@IdRes viewId: Int) {
        onView(allOf(withId(viewId), isDisplayed())).perform(click())
    }

    protected fun clicksOnDisplayedViewWithText(@IdRes viewId: Int, text: String) {
        onView(allOf(withId(viewId), withText(text), isDisplayed())).perform(click())
    }

    protected fun typeTextInView(@IdRes viewId: Int, text: String) {
        onView(allOf(withId(viewId), isDisplayed()))
            .perform(replaceText(text), closeSoftKeyboard())
    }

    protected fun clickOnItemInList(@IdRes listId: Int, position: Int) {
        onView(withId(listId))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(position, click()))
    }

    protected fun scrollToView(@IdRes viewId: Int) {
        onView(withId(viewId)).perform(scrollTo())
    }

    protected fun checkViewExists(@IdRes viewId: Int) {
        onView(withId(viewId)).check(matches(isDisplayed()))
    }

    // View Assertions helpers
    protected fun assertViewWithTextDisplayed(text: String) {
        onView(withText(text)).check(matches(isDisplayed()))
    }

    protected fun assertViewWithIdDisplayed(@IdRes viewId: Int) {
        onView(withId(viewId)).check(matches(isDisplayed()))
    }

    protected fun delay(seconds: Long) {
        onView(isRoot()).perform(waitOnId(TimeUnit.SECONDS.toMillis(seconds)))
    }

    protected fun checkWithTextIsDisplayed(@IdRes viewId: Int, text: String) {
        onView(allOf(withId(viewId), withText(text), isDisplayed()))
            .check(matches(withText(text)))
    }

    protected fun goBack() {
        pressBack()
    }
}
