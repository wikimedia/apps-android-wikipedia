package org.wikipedia.base

import androidx.annotation.IdRes
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.web.assertion.WebViewAssertions
import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.espresso.web.webdriver.DriverAtoms
import androidx.test.espresso.web.webdriver.DriverAtoms.findElement
import androidx.test.espresso.web.webdriver.DriverAtoms.webClick
import androidx.test.espresso.web.webdriver.Locator
import org.hamcrest.Matchers
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import org.wikipedia.TestUtil.waitOnId
import java.util.concurrent.TimeUnit

abstract class BaseRobot {

    protected fun clickWithId(@IdRes viewId: Int) {
        onView(withId(viewId)).perform(click())
    }

    protected fun clickOnDisplayedView(@IdRes viewId: Int) {
        onView(allOf(withId(viewId), isDisplayed())).perform(click())
    }

    protected fun clicksOnDisplayedViewWithText(@IdRes viewId: Int, text: String) {
        onView(allOf(withId(viewId), withText(text), isDisplayed())).perform(click())
    }

    protected fun clickOnDisplayedViewWithContentDescription(description: String) {
        onView(allOf(withContentDescription(description), isDisplayed())).perform(click())
    }

    protected fun clickWithText(text: String) {
        onView(withText(text)).perform(click())
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

    protected fun scrollToViewAndClick(@IdRes viewId: Int) {
        onView(withId(viewId)).perform(scrollTo(), click())
    }

    protected fun scrollToTextAndClick(text: String) {
        onView(allOf(withText(text))).perform(scrollTo(), click())
    }

    protected fun checkViewExists(@IdRes viewId: Int) {
        onView(withId(viewId)).check(matches(isDisplayed()))
    }

    protected fun checkViewDoesNotExist(@IdRes viewId: Int) {
        onView(withId(viewId)).check(matches(not(isDisplayed())))
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

    protected fun swipeLeft(@IdRes viewId: Int) {
        onView(withId(viewId)).perform(swipeLeft())
    }

    protected fun goBack() {
        pressBack()
    }

    protected fun clickWebLink(linkTitle: String) = apply {
        onWebView()
            .withElement(findElement(Locator.CSS_SELECTOR, "a[title='$linkTitle']"))
            .perform(webClick())
    }

    protected fun verifyH1Title(expectedTitle: String) = apply {
        onWebView()
            .withElement(findElement(Locator.CSS_SELECTOR, "h1"))
            .check(WebViewAssertions.webMatches(DriverAtoms.getText(), Matchers.`is`(expectedTitle)))
    }

    protected fun isViewDisplayed(@IdRes viewId: Int): Boolean {
        var isDisplayed = false
        onView(withId(viewId)).check { view, noViewFoundException ->
            isDisplayed = noViewFoundException == null && view.isShown
        }
        return isDisplayed
    }

    protected fun performIfDialogShown(
        dialogText: String,
        action: () -> Unit
    ) {
        try {
            onView(withText(dialogText))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))
            action()
        } catch (e: Exception) {
            // Dialog not shown or text not found
        }
    }
}
