package org.wikipedia.base.actions

import android.view.View
import androidx.annotation.ColorRes
import androidx.annotation.IdRes
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.contrib.RecyclerViewActions.scrollToPosition
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.not
import org.hamcrest.TypeSafeMatcher
import org.wikipedia.base.utils.ColorAssertions
import org.wikipedia.base.utils.ColorMatchers
import org.wikipedia.base.utils.matchesAtPosition

class VerificationActions {
    fun viewExists(@IdRes viewId: Int) {
        onView(withId(viewId)).check(matches(isDisplayed()))
    }

    fun viewWithIdIsNotVisible(@IdRes viewId: Int) {
        onView(withId(viewId)).check(matches(not(isDisplayed())))
    }

    fun viewWithTextDisplayed(text: String) {
        onView(withText(text)).check(matches(isDisplayed()))
    }

    fun textIsNotVisible(text: String) {
        onView(withText(text)).check(matches(not(isDisplayed())))
    }

    fun viewWithIdDoesNotExist(@IdRes viewId: Int) {
        onView(withId(viewId))
            .check(doesNotExist())
    }

    fun viewWithTextDoesNotExist(title: String) {
        onView(withText(title))
            .check(doesNotExist())
    }

    fun viewWithIdDisplayed(@IdRes viewId: Int) {
        onView(withId(viewId)).check(matches(isDisplayed()))
    }

    fun partialString(text: String) {
        onView(withText(containsString(text)))
            .check(matches(isDisplayed()))
    }

    fun viewWithTextVisible(text: String): Boolean {
        var isDisplayed = false
        onView(withText(text)).check { view, noViewFoundException ->
            isDisplayed = noViewFoundException == null && view.isShown
        }
        return isDisplayed
    }

    fun isViewWithTextVisible(text: String): Boolean {
        var isDisplayed = false
        onView(withText(text)).check { view, noViewFoundException ->
            isDisplayed = noViewFoundException == null && view.isShown
        }
        return isDisplayed
    }

    fun viewWithIdAndText(@IdRes viewId: Int, text: String) {
        onView(allOf(withId(viewId), withText(text))).check(matches(isDisplayed()))
    }

    fun withTextIsDisplayed(@IdRes viewId: Int, text: String) {
        onView(allOf(withId(viewId), withText(text), isDisplayed()))
            .check(matches(withText(text)))
    }

    fun ifViewIsDisplayingText(@IdRes viewId: Int, text: String) {
        onView(allOf(withId(viewId), isDisplayed()))
            .check(matches(withText(text)))
    }

    fun withMatcher(@IdRes viewId: Int, matcher: Matcher<View>) {
        onView(withId(viewId))
            .check(matches(matcher))
    }

    fun messageOfSnackbar(text: String) {
        onView(
            allOf(
                withId(com.google.android.material.R.id.snackbar_text),
                withText(text)
            )).check(matches(isDisplayed()))
    }

    fun textViewColor(
        @IdRes textViewId: Int,
        @ColorRes colorResId: Int
    ) {
        onView(withId(textViewId))
            .check(ColorAssertions.hasColor(colorResId, ColorAssertions.ColorType.TextColor))
    }

    protected fun backgroundColor(
        @IdRes viewId: Int,
        @ColorRes colorResId: Int
    ) {
        onView(withId(viewId))
            .check(ColorAssertions.hasColor(colorResId, ColorAssertions.ColorType.BackgroundColor))
    }

    fun tintColor(
        @IdRes viewId: Int,
        colorResOrAttr: Int,
        isAttr: Boolean = false
    ) {
        onView(withId(viewId))
            .check((matches(ColorMatchers.withTintColor(colorResOrAttr, isAttr))))
    }

    fun rTLDirectionOfAView(@IdRes viewId: Int) {
        onView(withId(viewId),)
            .check(matches(isLayoutDirectionRTL()))
    }

    fun rTLDirectionOfRecyclerViewItem(@IdRes recyclerViewId: Int) {
        onView(withId(recyclerViewId))
            .perform(scrollToPosition<RecyclerView.ViewHolder>(0))
            .check(matches(atPosition(0, isLayoutDirectionRTL())))
    }

    fun assertColorForChildItemInAList(
        @IdRes listId: Int,
        @IdRes childItemId: Int,
        @ColorRes colorResId: Int,
        position: Int,
        colorType: ColorAssertions.ColorType = ColorAssertions.ColorType.TextColor
    ) {
        onView(withId(listId))
            .perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(position))
            .check(matchesAtPosition(position, targetViewId = childItemId, assertion = { view ->
                ColorAssertions.hasColor(colorResId, colorType)
                    .check(view, null)
            }))
    }

    fun imageIsVisibleInsideARecyclerView(@IdRes listId: Int,
                                                         @IdRes childItemId: Int,
                                                         position: Int) {
        onView(withId(listId))
            .check(matchesAtPosition(position, targetViewId = childItemId, assertion = { view ->
                matches(isDisplayed())
            }))
    }

    private fun isLayoutDirectionRTL() = object : TypeSafeMatcher<View>() {
        override fun describeTo(description: Description) {
            description.appendText("with layout direction RTL")
        }

        override fun matchesSafely(view: View): Boolean {
            return view.layoutDirection == View.LAYOUT_DIRECTION_RTL
        }
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
}
