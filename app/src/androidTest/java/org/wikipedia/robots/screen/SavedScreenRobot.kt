package org.wikipedia.robots.screen

import android.app.Activity
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers.allOf
import org.wikipedia.R
import org.wikipedia.base.BaseRobot
import org.wikipedia.base.TestConfig

class SavedScreenRobot : BaseRobot() {

    fun clickItemOnTheList(position: Int) = apply {
        clickRecyclerViewItemAtPosition(R.id.recycler_view, position)
        delay(TestConfig.DELAY_LARGE)
    }

    fun clickItemOnReadingList(position: Int) = apply {
        clickRecyclerViewItemAtPosition(R.id.reading_list_recycler_view, position)
        delay(TestConfig.DELAY_LARGE)
    }

    fun dismissTooltip(activity: Activity) = apply {
        dismissTooltipIfAny(activity, viewId = R.id.buttonView)
    }

    fun assertIfListMatchesTheArticleTitle(text: String) = apply {
        checkWithTextIsDisplayed(viewId = R.id.page_list_item_title, text)
        delay(TestConfig.DELAY_SHORT)
    }

    fun openArticleWithTitle(text: String) = apply {
        clicksOnDisplayedViewWithText(viewId = R.id.page_list_item_title, text)
        delay(TestConfig.DELAY_LARGE)
    }

    fun dismissSyncReadingList() = apply {
        clickOnViewWithId(R.id.negativeButton)
        delay(TestConfig.DELAY_SHORT)
    }

    fun swipeToDelete(position: Int) = apply {
        onView(withId(R.id.reading_list_recycler_view))
            .perform(
                RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                    position,
                    ViewActions.swipeLeft()
                )
            )
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun verifySavedArticleIsRemoved(title: String) = apply {
        onView(
            allOf(
                withId(R.id.page_list_item_title),
                withText(title)
            )
        ).check(doesNotExist())
    }

    fun clickFilterList() = apply {
        clickOnViewWithId(R.id.menu_search_lists)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun pressBack() = apply {
        goBack()
        delay(TestConfig.DELAY_SHORT)
    }
}
