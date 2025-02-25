package org.wikipedia.base.actions

import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ListView
import android.widget.ScrollView
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.hamcrest.Matchers.anything
import org.junit.Assert.assertEquals
import org.wikipedia.R

class ListActions {
    fun clickOnListView(@IdRes viewId: Int, @IdRes childView: Int, position: Int) = apply {
        onData(anything())
            .inAdapterView(withId(viewId))
            .atPosition(position)
            .onChildView(withId(childView))
            .perform(click())
    }

    fun clickOnItemInList(@IdRes listId: Int, position: Int) {
        onView(withId(listId))
            .perform(
                RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                    position,
                    click()
                )
            )
    }

    fun clickOnItemInList(textViewId: Int) {
        onView(withId(textViewId))
            .perform(click())
    }

        fun longClickOnItemInList(@IdRes listId: Int, position: Int) {
        onView(withId(listId))
            .perform(
                RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                    position,
                    longClick()
                )
            )
    }

    fun clickRecyclerViewItemAtPosition(@IdRes viewId: Int, position: Int) {
        onView(withId(viewId))
            .perform(
                RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                    position,
                    click()
                )
            )
    }

    fun scrollToPositionInRecyclerView(@IdRes viewId: Int, position: Int) {
        onView(withId(viewId))
            .perform(
                RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(position)
            )
    }

    fun clickOnSpecificItemInList(@IdRes listId: Int, @IdRes itemId: Int, position: Int) {
        onView(withId(listId))
            .perform(
                RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(position),
                RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                    position,
                    clickChildViewWithId(itemId)
                )
            )
    }

    fun scrollToRecyclerViewInsideNestedScrollView(@IdRes recyclerViewId: Int, position: Int, viewAction: ViewAction) {
        onView(withId(recyclerViewId))
            .perform(NestedScrollViewExtension())
        onView(withId(recyclerViewId))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(position, viewAction))
    }

    fun scrollToViewInsideNestedScrollView(@IdRes viewId: Int) {
        onView(withId(viewId)).perform(NestedScrollViewExtension())
    }

    fun scrollToRecyclerView(
        recyclerViewId: Int = R.id.feed_view,
        title: String,
        textViewId: Int = R.id.view_card_header_title,
        verticalOffset: Int = 200,
        action: (() -> Unit)? = null
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
        action?.invoke()
    }

    fun verifyRecyclerViewItemCount(@IdRes viewId: Int, expectedCount: Int) {
        onView(withId(viewId))
            .check(hasItemCount(expectedCount))
    }

    private fun clickChildViewWithId(@IdRes id: Int) = object : ViewAction {
        override fun getConstraints() = null

        override fun getDescription() = "Click on a child view with specified id."

        override fun perform(uiController: UiController, view: View) {
            val v = view.findViewById<View>(id)
            v?.performClick()
        }
    }

    private fun hasItemCount(expectedCount: Int): ViewAssertion {
        return ViewAssertion { view, noViewFoundException ->
            if (view == null) {
                throw noViewFoundException
            }
            val recyclerView = view as RecyclerView
            val adapter = recyclerView.adapter
            assertEquals(adapter?.itemCount, expectedCount)
        }
    }
}

class NestedScrollViewExtension(scrollToAction: ViewAction = ViewActions.scrollTo()) : ViewAction by scrollToAction {
    override fun getConstraints(): Matcher<View> {
        return Matchers.allOf(
            ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE),
            ViewMatchers.isDescendantOfA(
                Matchers.anyOf(
                ViewMatchers.isAssignableFrom(NestedScrollView::class.java),
                ViewMatchers.isAssignableFrom(ScrollView::class.java),
                ViewMatchers.isAssignableFrom(HorizontalScrollView::class.java),
                ViewMatchers.isAssignableFrom(ListView::class.java))))
    }
}
