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
import org.wikipedia.base.TestConfig
import org.wikipedia.base.base.BaseRobot

class SavedScreenRobot : BaseRobot() {

    fun clickItemOnTheList(position: Int) = apply {
        list.clickRecyclerViewItemAtPosition(R.id.recycler_view, position)
        delay(TestConfig.DELAY_LARGE)
    }

    fun clickItemOnReadingList(position: Int) = apply {
        list.clickRecyclerViewItemAtPosition(R.id.reading_list_recycler_view, position)
        delay(TestConfig.DELAY_LARGE)
    }

    fun dismissTooltip(activity: Activity) = apply {
        system.dismissTooltipIfAny(activity, viewId = R.id.buttonView)
    }

    fun assertIfListMatchesTheArticleTitle(text: String) = apply {
        verify.withTextIsDisplayed(viewId = R.id.page_list_item_title, text)
        delay(TestConfig.DELAY_SHORT)
    }

    fun openArticleWithTitle(text: String) = apply {
        click.onDisplayedViewWithText(viewId = R.id.page_list_item_title, text)
        delay(TestConfig.DELAY_LARGE)
    }

    fun dismissSyncReadingList() = apply {
        try {
            click.onViewWithId(R.id.negativeButton)
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
        verify.imageIsVisibleInsideARecyclerView(
            listId = R.id.reading_list_recycler_view,
            childItemId = R.id.page_list_item_image,
            position = position
        )
        delay(TestConfig.DELAY_SHORT)
    }

    fun verifyPageIsOffline(context: Context) = apply {
        try {
            verify.messageOfSnackbar(context.getString(R.string.page_offline_notice_last_date))
        } catch (e: Exception) {
            Log.e("SavedScreenRobotError:", "Snackbar is not visible.")
        }
    }

    fun clickFilterList() = apply {
        click.onViewWithId(R.id.menu_search_lists)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun pressBack() = apply {
        goBack()
        delay(TestConfig.DELAY_SHORT)
    }
}
