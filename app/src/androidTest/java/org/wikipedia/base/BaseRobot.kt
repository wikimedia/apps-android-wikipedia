package org.wikipedia.base

import android.app.Activity
import android.graphics.Rect
import android.util.Log
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ListView
import android.widget.ScrollView
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.action.ViewActions.swipeRight
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.contrib.RecyclerViewActions.scrollToPosition
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isDisplayingAtLeast
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
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.not
import org.hamcrest.TypeSafeMatcher
import org.wikipedia.R
import org.wikipedia.TestUtil
import org.wikipedia.TestUtil.waitOnId
import java.util.concurrent.TimeUnit

abstract class BaseRobot {

    protected fun clickOnViewWithIdAndContainsString(@IdRes viewId: Int, text: String) {
        onView(allOf(
            withId(viewId),
            hasText(text),
        )).perform(scrollAndClick())
    }

    protected fun clickOnViewWithId(@IdRes viewId: Int) {
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

    protected fun clickOnDisplayedViewWithIdAnContentDescription(
        @IdRes viewId: Int,
        description: String
    ) {
        onView(allOf(withId(viewId), withContentDescription(description), isDisplayed())).perform(
            click()
        )
    }

    protected fun clickOnViewWithText(text: String) {
        onView(withText(text)).perform(click())
    }

    protected fun typeTextInView(@IdRes viewId: Int, text: String) {
        onView(allOf(withId(viewId), isDisplayed()))
            .perform(replaceText(text), closeSoftKeyboard())
    }

    protected fun clickOnItemInList(@IdRes listId: Int, position: Int) {
        onView(withId(listId))
            .perform(
                RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                    position,
                    click()
                )
            )
    }

    protected fun clickOnSpecificItemInList(@IdRes listId: Int, @IdRes itemId: Int, position: Int) {
        onView(withId(listId))
            .perform(
                RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(position),
                RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                    position,
                    clickChildViewWithId(itemId)
                )
            )
    }

    protected fun assertColorForChildItemInAList(
        @IdRes listId: Int,
        @IdRes childItemId: Int,
        colorResOrAttr: Int,
        position: Int,
        isAttr: Boolean = true,
        colorType: ColorAssertions.ColorType = ColorAssertions.ColorType.TextColor
    ) {
        onView(withId(listId))
            .perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(position))
            .check(matchesAtPosition(position, targetViewId = childItemId, assertion = { view ->
                ColorAssertions.hasColor(colorResOrAttr, isAttr, colorType)
                    .check(view, null)
            }))
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

    protected fun checkViewWithTextDisplayed(text: String) {
        onView(withText(text)).check(matches(isDisplayed()))
    }

    protected fun checkTextDoesNotExist(text: String) {
        onView(withText(text)).check(matches(not(isDisplayed())))
    }

    protected fun checkViewWithIdDisplayed(@IdRes viewId: Int) {
        onView(withId(viewId)).check(matches(isDisplayed()))
    }

    protected fun checkPartialString(text: String) {
        onView(withText(containsString(text)))
            .check(matches(isDisplayed()))
    }

    protected fun isViewWithTextVisible(text: String): Boolean {
        var isDisplayed = false
        onView(withText(text)).check { view, noViewFoundException ->
            isDisplayed = noViewFoundException == null && view.isShown
        }
        return isDisplayed
    }

    protected fun isViewWithIdDisplayed(@IdRes viewId: Int): Boolean {
        var isDisplayed = false
        onView(withId(viewId)).check { view, noViewFoundException ->
            isDisplayed = noViewFoundException == null && view.isShown
        }
        return isDisplayed
    }

    protected fun checkViewWithIdAndText(@IdRes viewId: Int, text: String) {
        onView(allOf(withId(viewId), withText(text))).check(matches(isDisplayed()))
    }

    protected fun delay(seconds: Long) {
        onView(isRoot()).perform(waitOnId(TimeUnit.SECONDS.toMillis(seconds)))
    }

    protected fun checkWithTextIsDisplayed(@IdRes viewId: Int, text: String) {
        onView(allOf(withId(viewId), withText(text), isDisplayed()))
            .check(matches(withText(text)))
    }

    protected fun checkIfViewIsDisplayingText(@IdRes viewId: Int, text: String) {
        onView(allOf(withId(viewId), isDisplayed()))
            .check(matches(withText(text)))
    }

    protected fun swipeLeft(@IdRes viewId: Int) {
        onView(withId(viewId)).perform(swipeLeft())
    }

    protected fun swipeRight(@IdRes viewId: Int) {
        onView(withId(viewId)).perform(swipeRight())
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
            .check(
                WebViewAssertions.webMatches(
                    DriverAtoms.getText(),
                    Matchers.`is`(expectedTitle)
                )
            )
    }

    protected fun verifyWithMatcher(@IdRes viewId: Int, matcher: Matcher<View>) {
        onView(withId(viewId))
            .check(matches(matcher))
    }

    protected fun swipeDownOnTheWebView(@IdRes viewId: Int) {
        onView(withId(viewId)).perform(TestUtil.swipeDownWebView())
        delay(TestConfig.DELAY_LARGE)
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
            Log.e("error", "")
        }
    }

    protected fun swipeUp(@IdRes viewId: Int) {
        onView(allOf(withId(viewId))).perform(swipeUp())
    }

    protected fun clickRecyclerViewItemAtPosition(@IdRes viewId: Int, position: Int) {
        onView(withId(viewId))
            .perform(
                RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                    position,
                    click()
                )
            )
    }

    protected fun scrollToPositionInRecyclerView(@IdRes viewId: Int, position: Int) {
        onView(withId(viewId))
            .perform(
                RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(position)
            )
    }

    protected fun makeViewVisibleAndLongClick(@IdRes viewId: Int, @IdRes parentViewId: Int) {
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

    protected fun clickIfVisible(): ViewAction {
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

    protected fun dismissTooltipIfAny(activity: Activity, @IdRes viewId: Int) = apply {
        onView(allOf(withId(viewId))).inRoot(withDecorView(not(Matchers.`is`(activity.window.decorView))))
            .perform(click())
    }

    protected fun scrollToRecyclerViewInsideNestedScrollView(@IdRes recyclerViewId: Int, position: Int, viewAction: ViewAction) {
        onView(withId(recyclerViewId))
            .perform(NestedScrollViewExtension())
        onView(withId(recyclerViewId))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(position, viewAction))
    }

    protected fun scrollToViewInsideNestedScrollView(@IdRes viewId: Int) {
        onView(withId(viewId)).perform(NestedScrollViewExtension())
    }

    protected fun makeViewVisibleAndClick(@IdRes viewId: Int, @IdRes parentViewId: Int) {
        onView(allOf(withId(viewId), isDescendantOfA(withId(parentViewId))))
            .perform(scrollAndClick())
    }

    fun scrollToRecyclerView(
        recyclerViewId: Int = R.id.feed_view,
        title: String,
        textViewId: Int = R.id.view_card_header_title,
        verticalOffset: Int = 200
    ) = apply {
        var currentOccurrence = 0
        onView(withId(recyclerViewId))
            .perform(
                RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                    hasDescendant(
                        object : BoundedMatcher<View, View>(View::class.java) {
                            override fun describeTo(description: Description?) {
                                description?.appendText("Scroll to Card View with title: $title")
                            }

                            override fun matchesSafely(item: View?): Boolean {
                                val titleView = item?.findViewById<TextView>(textViewId)
                                if (titleView?.text?.toString() == title) {
                                    if (currentOccurrence == 0) {
                                        currentOccurrence++
                                        return true
                                    }
                                    currentOccurrence++
                                }
                                return false
                            }
                        }
                    )
                )
            ).also { view ->
                if (verticalOffset != 0) {
                    view.perform(object : ViewAction {
                        override fun getConstraints(): Matcher<View> =
                            Matchers.any(View::class.java)

                        override fun getDescription(): String = "Scroll"

                        override fun perform(uiController: UiController, view: View) {
                            (view as RecyclerView).scrollBy(0, verticalOffset)
                            uiController.loopMainThreadUntilIdle()
                        }
                    })
                }
            }
    }

    private fun scrollAndClick() = object : ViewAction {
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

    protected fun verifyTextViewColor(
        @IdRes textViewId: Int,
        colorResOrAttr: Int,
        isAttr: Boolean = false
    ) {
        onView(withId(textViewId))
            .check(ColorAssertions.hasColor(colorResOrAttr, isAttr, ColorAssertions.ColorType.TextColor))
    }

    protected fun verifyBackgroundColor(
        @IdRes viewId: Int,
        colorResOrAttr: Int,
        isAttr: Boolean = false
    ) {
        onView(withId(viewId))
            .check(ColorAssertions.hasColor(colorResOrAttr, isAttr, ColorAssertions.ColorType.BackgroundColor))
    }

    protected fun verifyTintColor(
        @IdRes viewId: Int,
        colorResOrAttr: Int,
        isAttr: Boolean = false
    ) {
        onView(withId(viewId))
            .check((matches(ColorMatchers.withTintColor(colorResOrAttr, isAttr))))
    }

    protected fun checkRTLDirectionOfAView(@IdRes viewId: Int) {
        onView(withId(viewId),)
            .check(matches(isLayoutDirectionRTL()))
    }

    protected fun checkRTLDirectionOfRecyclerViewItem(@IdRes recyclerViewId: Int) {
        onView(withId(recyclerViewId))
            .perform(scrollToPosition<RecyclerView.ViewHolder>(0))
            .check(matches(atPosition(0, isLayoutDirectionRTL())))
    }

    private fun atPosition(position: Int, matcher: Matcher<View>) = object : BoundedMatcher<View, RecyclerView>(RecyclerView::class.java) {
        override fun describeTo(description: Description) {
            description.appendText("has item at position $position")
        }

        override fun matchesSafely(recylerView: RecyclerView): Boolean {
            val viewHolder = recylerView.findViewHolderForAdapterPosition(position)
                ?: return false
            return matcher.matches(viewHolder.itemView)
        }
    }

    private fun isLayoutDirectionRTL() = object : TypeSafeMatcher<View>() {
        override fun describeTo(description: Description) {
            description.appendText("with layout direction RTL")
        }

        override fun matchesSafely(view: View): Boolean {
            return view.layoutDirection == View.LAYOUT_DIRECTION_RTL
        }
    }

    private fun clickChildViewWithId(@IdRes id: Int) = object : ViewAction {
        override fun getConstraints() = null

        override fun getDescription() = "Click on a child view with specified id."

        override fun perform(uiController: UiController, view: View) {
            val v = view.findViewById<View>(id)
            v?.performClick()
        }
    }

    private fun matchesAtPosition(position: Int, @IdRes targetViewId: Int, assertion: (View) -> Unit): ViewAssertion {
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

    private fun hasText(text: String) = object : BoundedMatcher<View, TextView>(TextView::class.java) {
        override fun describeTo(description: Description?) {
            description?.appendText("contains text $text")
        }

        override fun matchesSafely(item: TextView): Boolean {
            return item.text.toString().contains(text, ignoreCase = true)
        }
    }
}

class NestedScrollViewExtension(scrollToAction: ViewAction = ViewActions.scrollTo()) : ViewAction by scrollToAction {
    override fun getConstraints(): Matcher<View> {
        return Matchers.allOf(
            ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE),
            ViewMatchers.isDescendantOfA(Matchers.anyOf(
                ViewMatchers.isAssignableFrom(NestedScrollView::class.java),
                ViewMatchers.isAssignableFrom(ScrollView::class.java),
                ViewMatchers.isAssignableFrom(HorizontalScrollView::class.java),
                ViewMatchers.isAssignableFrom(ListView::class.java))))
    }
}
