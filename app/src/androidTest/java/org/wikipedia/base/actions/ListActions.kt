package org.wikipedia.base.actions

import android.util.Log
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
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.google.android.material.tabs.TabLayout
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.anything
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.not
import org.junit.Assert.assertEquals
import org.wikipedia.R
import org.wikipedia.views.DefaultViewHolder
import java.util.concurrent.atomic.AtomicInteger

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

    fun scrollToRecyclerViewInsideNestedScrollView(
        @IdRes recyclerViewId: Int,
        position: Int,
        viewAction: ViewAction
    ) {
        onView(withId(recyclerViewId))
            .perform(NestedScrollViewExtension())
        onView(withId(recyclerViewId))
            .perform(
                RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                    position,
                    viewAction
                )
            )
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

    fun verifyItemDoesNotExistWithText(
        recyclerViewId: Int,
        text: String
    ) {
        onView(withId(recyclerViewId))
            .check(
                matches(
                    not(
                        hasDescendant(
                            allOf(
                                withText(text),
                                isDisplayed()
                            )
                        )
                    )
                )
            )
    }

    fun verifyItemExistWithText(
        recyclerViewId: Int,
        text: String
    ) {
        onView(withId(recyclerViewId))
            .check(
                matches(
                    hasDescendant(
                        allOf(
                            withText(text),
                            isDisplayed()
                        )
                    )
                )
            )
    }

    fun selectTabWithText(@IdRes viewId: Int, text: String) {
        onView(withId(viewId))
            .perform(object : ViewAction {
                override fun getConstraints(): Matcher<View?> = isDisplayed()

                override fun getDescription(): String = "Select tab with text: $text"

                override fun perform(
                    uiController: UiController,
                    view: View
                ) {
                    val tabLayout = view as TabLayout
                    for (i in 0 until tabLayout.tabCount) {
                        val tab = tabLayout.getTabAt(i)
                        val labelView = tab?.customView?.findViewById<TextView>(R.id.language_label)
                        if (labelView?.text == text) {
                            tab.select()
                            break
                        }
                    }
                    uiController.loopMainThreadUntilIdle()
                }
            })
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

    fun moveClickIntoViewAndClick(childId: Int): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> = isAssignableFrom(View::class.java)
            override fun getDescription(): String = "click child view with id $childId"
            override fun perform(uiController: UiController, view: View) {
                val child = view.findViewById<View>(childId)

                // Find the parent RecyclerView
                var parent = view.parent
                while (parent != null && parent !is RecyclerView) {
                    parent = parent.parent
                }

                if (parent is RecyclerView) {
                    // Calculate scroll distance to bring child into view
                    val childLocation = IntArray(2)
                    child.getLocationOnScreen(childLocation)
                    val recyclerLocation = IntArray(2)
                    parent.getLocationOnScreen(recyclerLocation)

                    val scrollY =
                        childLocation[1] - recyclerLocation[1] - 100 // -100 for some padding/toolbar

                    if (scrollY != 0) {
                        parent.smoothScrollBy(0, scrollY)
                        uiController.loopMainThreadForAtLeast(500)
                    }
                } else {
                    // Fallback if no RecyclerView found (unlikely in this context)
                    child.requestRectangleOnScreen(
                        android.graphics.Rect(
                            0,
                            0,
                            child.width,
                            child.height
                        ), true
                    )
                    uiController.loopMainThreadForAtLeast(300)
                }
                child.performClick()
            }
        }
    }

    fun clickNestedItem(nestedRecyclerViewId: Int, position: Int): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View?>? = isAssignableFrom(View::class.java)

            override fun getDescription(): String? =
                "click item at position $position in nested RecyclerView with id $nestedRecyclerViewId"

            override fun perform(
                uiController: UiController,
                view: View
            ) {
                val nestedRecyclerView =
                    view.findViewById<RecyclerView>(nestedRecyclerViewId) ?: throw RuntimeException(
                        "Could not find nested RecyclerView with id $nestedRecyclerViewId"
                    )

                nestedRecyclerView.scrollToPosition(position)
                uiController.loopMainThreadForAtLeast(500)

                val holder = nestedRecyclerView.findViewHolderForAdapterPosition(position)
                    ?: throw RuntimeException("Could not find ViewHolder at position $position in nested list")

                holder.itemView.performClick()
            }
        }
    }

    fun scrollAndPerform(
        viewIdRes: Int = R.id.feed_view,
        title: String,
        action: (Int) -> Unit = {}
    ) {
        val matcher = withCardTitle(title)
        val position = getPosition(viewIdRes, matcher)
        if (position != -1) {
            onView(withId(viewIdRes))
                .perform(scrollToPosition(position))
            action(position)
        } else {
            Log.e("ExploreFeedRobot: ", "Skipping scroll for $title - item not found in adapter.")
        }
    }

    private fun scrollToPosition(position: Int): ViewAction {
        return object : ViewAction {
            override fun getConstraints() = isAssignableFrom(RecyclerView::class.java)
            override fun getDescription() = "scroll to position $position"
            override fun perform(uiController: UiController, view: View) {
                (view as RecyclerView).scrollToPosition(position)
                uiController.loopMainThreadForAtLeast(500)
            }
        }
    }

    private fun withCardTitle(title: String): Matcher<RecyclerView.ViewHolder> {
        return object : BoundedMatcher<RecyclerView.ViewHolder, DefaultViewHolder<*>>(
            DefaultViewHolder::class.java
        ) {
            override fun describeTo(description: Description) {
                description.appendText("ViewHolder with title: $title")
            }

            override fun matchesSafely(item: DefaultViewHolder<*>?): Boolean {
                val view = item?.view ?: return false
                val matcher = hasDescendant(
                    allOf(
                        withId(R.id.view_card_header_title),
                        withText(containsString(title))
                    )
                )
                return matcher.matches(view)
            }
        }
    }

    private fun findViewHolderPosition(
        recyclerView: RecyclerView,
        matcher: Matcher<RecyclerView.ViewHolder>
    ): Int {
        val adapter = recyclerView.adapter ?: return -1
        for (i in 0 until adapter.itemCount) {
            val holder = adapter.createViewHolder(recyclerView, adapter.getItemViewType(i))
            adapter.bindViewHolder(holder, i)
            if (matcher.matches(holder)) {
                return i
            }
        }
        return -1
    }

    private fun getPosition(viewId: Int, matcher: Matcher<RecyclerView.ViewHolder>): Int {
        val position = AtomicInteger(-1)
        onView(withId(viewId)).perform(object : ViewAction {
            override fun getConstraints() = isAssignableFrom(RecyclerView::class.java)
            override fun getDescription() = "get position"
            override fun perform(uiController: UiController, view: View) {
                position.set(findViewHolderPosition(view as RecyclerView, matcher))
            }
        })
        return position.get()
    }
}

class NestedScrollViewExtension(scrollToAction: ViewAction = ViewActions.scrollTo()) :
    ViewAction by scrollToAction {
    override fun getConstraints(): Matcher<View> {
        return Matchers.allOf(
            ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE),
            ViewMatchers.isDescendantOfA(
                Matchers.anyOf(
                    ViewMatchers.isAssignableFrom(NestedScrollView::class.java),
                    ViewMatchers.isAssignableFrom(ScrollView::class.java),
                    ViewMatchers.isAssignableFrom(HorizontalScrollView::class.java),
                    ViewMatchers.isAssignableFrom(ListView::class.java)
                )
            )
        )
    }
}
