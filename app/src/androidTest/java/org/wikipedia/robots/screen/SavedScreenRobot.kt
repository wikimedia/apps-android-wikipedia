package org.wikipedia.robots.screen

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
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
        try {
            clickOnViewWithId(R.id.negativeButton)
            delay(TestConfig.DELAY_SHORT)
        } catch (e: Exception) {
            Log.e("SavedScreenRobot: ", "${e.message}")
        }
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

    fun verifySavedArticle(title: String) = apply {
        onView(
            allOf(
                withId(R.id.page_list_item_title),
                withText(title)
            )
        ).check(matches(isDisplayed()))
    }

    fun verifyImageIsVisible(position: Int) = apply {
        checkImageIsVisibleInsideARecyclerView(
            listId = R.id.reading_list_recycler_view,
            childItemId = R.id.page_list_item_image,
            position = position
        )
        delay(TestConfig.DELAY_SHORT)
    }

    fun verifyPageIsOffline(context: Context) = apply {
        try {
            verifyMessageOfSnackbar(context.getString(R.string.page_offline_notice_last_date))
        } catch (e: Exception) {
            Log.e("SavedScreenRobotError:", "Snackbar is not visible.")
        }
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
