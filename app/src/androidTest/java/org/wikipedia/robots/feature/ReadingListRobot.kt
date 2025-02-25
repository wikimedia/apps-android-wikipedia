package org.wikipedia.robots.feature

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.wikipedia.R
import org.wikipedia.base.TestConfig
import org.wikipedia.base.base.BaseRobot

class ReadingListRobot : BaseRobot() {

    fun clickOnList(position: Int) = apply {
        list.clickOnItemInList(
            listId = R.id.recycler_view,
            position
        )
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun clickOnList(title: String) = apply {
        list.scrollToRecyclerView(
            recyclerViewId = R.id.recycler_view,
            title = title,
            textViewId = R.id.item_title,
            action = {
                click.onViewWithId(R.id.item_title)
            }
        )
        delay(TestConfig.DELAY_SHORT)
    }

    fun saveArticleToReadingList() = apply {
        click.onViewWithId(R.id.page_save)
        delay(TestConfig.DELAY_SHORT)
    }

    fun addToReadingList(context: Context) = apply {
        click.onViewWithText(context.getString(R.string.reading_list_add_to_list_button))
        delay(TestConfig.DELAY_SHORT)
    }

    fun typeNameOfTheList(title: String, context: Context) = apply {
        input.typeTextInView(viewId = R.id.text_input, title)
        delay(TestConfig.DELAY_MEDIUM)
        if (verify.isViewWithTextVisible(context.getString(R.string.reading_list_title_exists, title))) {
            input.typeTextInView(viewId = R.id.text_input, "$title${Math.random()}")
        }
    }

    fun saveTheList(context: Context) = apply {
        click.onViewWithText(context.getString(R.string.text_input_dialog_ok_button_text))
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun viewTheList(context: Context) = apply {
        click.onViewWithText(context.getString(R.string.reading_list_added_view_button))
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun dismissTooltip(activity: Activity) = apply {
        system.dismissTooltipIfAny(activity, viewId = R.id.buttonView)
    }

    fun clickOnGotIt() = apply {
        click.onViewWithText("Got it")
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun verifySavedArticleExists(title: String) = apply {
        verify.viewWithTextDisplayed(title)
    }

    fun verifyArticleHasNotDownloaded() = apply {
        delay(TestConfig.DELAY_MEDIUM)
        onView(withId(R.id.reading_list_recycler_view))
            .perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(1))
            .check(
                matches(
                    atPosition(
                        1,
                        hasDescendant(
                            allOf(
                                withId(R.id.page_list_item_action),
                                withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)
                            )
                        )
                    )
                )
            )
    }

    fun verifyArticleHasDownloaded() = apply {
        delay(TestConfig.DELAY_MEDIUM)
        onView(withId(R.id.reading_list_recycler_view))
            .perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(1))
            .check(
                matches(
                    atPosition(
                        1,
                        hasDescendant(
                            allOf(
                                withId(R.id.page_list_item_action),
                                withEffectiveVisibility(ViewMatchers.Visibility.GONE)
                            )
                        )
                    )
                )
            )
    }

    private fun atPosition(position: Int, itemMatcher: Matcher<View>): Matcher<View> {
        return object : BoundedMatcher<View, RecyclerView>(RecyclerView::class.java) {
            override fun describeTo(description: Description) {
                description.appendText("has item at position $position")
                itemMatcher.describeTo(description)
            }

            override fun matchesSafely(item: RecyclerView): Boolean {
                val viewHolder = item.findViewHolderForAdapterPosition(position) ?: return false
                return itemMatcher.matches(viewHolder.itemView)
            }
        }
    }

    fun navigateUp() = apply {
        click.onDisplayedViewWithContentDescription("Navigate up")
        delay(TestConfig.DELAY_SHORT)
    }

    fun clickNoThanks(context: Context) = apply {
        try {
            click.onViewWithText(context.getString(R.string.reading_list_prompt_turned_sync_on_dialog_no_thanks))
            delay(TestConfig.DELAY_MEDIUM)
        } catch (e: Exception) {
            Log.e("ReadingListRobot: ", "${e.message}")
        }
    }

    fun clickCreateList() = apply {
        click.onViewWithId(R.id.create_button)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun pressBack() = apply {
        delay(TestConfig.DELAY_MEDIUM)
        goBack()
    }
}
