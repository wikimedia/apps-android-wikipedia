package org.wikipedia.robots.feature

import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.contrib.RecyclerViewActions.scrollTo
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.wikipedia.Constants
import org.wikipedia.Constants.SUGGESTED_EDITS
import org.wikipedia.R
import org.wikipedia.TestUtil.childAtPosition
import org.wikipedia.base.BaseRobot
import org.wikipedia.base.ColorAssertions
import org.wikipedia.base.TestConfig

class ExploreFeedRobot : BaseRobot() {
    fun clickOnThisDayCard() = apply {
        onView(
            allOf(
                withId(R.id.on_this_day_page), childAtPosition(
                    allOf(
                        withId(R.id.event_layout),
                        childAtPosition(withId(R.id.on_this_day_card_view_click_container), 0)
                    ), 3
                ), isDisplayed()
            )
        )
            .perform(click())
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun clickRandomArticle() = apply {
        // Random article card seen and saved to reading lists
        makeViewVisibleAndClick(
            viewId = R.id.view_featured_article_card_content_container,
            parentViewId = R.id.feed_view
        )
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun dismissContributionDialog() = apply {
        performIfDialogShown("No, thanks", action = {
            clickOnViewWithText("No, thanks")
        })
    }

    fun dismissBigEnglishCampaignDialog() = apply {
        performIfDialogShown("Maybe later", action = {
            clickOnViewWithText("Maybe later")
        })
    }

    fun pressBack() = apply {
        goBack()
        delay(TestConfig.DELAY_LARGE)
    }

    fun navigateUp() = apply {
        clickOnDisplayedViewWithContentDescription("Navigate up")
    }

    fun clickTopReadArticle() = apply {
        try {
            onView(
                allOf(
                    withId(R.id.view_list_card_list),
                    childAtPosition(withId(R.id.view_list_card_list_container), 0)
                )
            ).perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))
                .perform()
            pressBack()
            delay(TestConfig.DELAY_MEDIUM)
        } catch (e: NoMatchingViewException) {
            Log.e("clickError", "")
        }
    }

    fun clickBecauseYouReadArticle() = apply {
        onView(
            allOf(
                withId(R.id.view_list_card_list),
                childAtPosition(withId(R.id.view_list_card_list_container), 0)
            )
        )
            .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))
    }

    fun clickNewsArticle() = apply {
        onView(
            allOf(
                withId(R.id.news_cardview_recycler_view),
                childAtPosition(withId(R.id.rtl_container), 1)
            )
        )
            .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun clickAddArticleDescription() = apply {
        clickOnDisplayedViewWithContentDescription(description = "Add article descriptions")
    }

    fun openOverflowMenuItem() = apply {
        clickOnViewWithId(R.id.page_toolbar_button_show_overflow_menu)
        delay(TestConfig.DELAY_SHORT)
    }

    fun clickPictureOfTheDay() = apply {
        clickOnViewWithId(R.id.view_featured_image_card_content_container)
        delay(TestConfig.DELAY_SHORT)
    }

    fun clickTodayOnWikipedia() = apply {
        clickOnViewWithIdAndContainsString(R.id.footerActionButton, text = "View main page")
        delay(TestConfig.DELAY_LARGE)
    }

    fun clickOnFeaturedArticle() = apply {
        makeViewVisibleAndClick(
            viewId = R.id.view_featured_article_card_content_container,
            parentViewId = R.id.feed_view
        )
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun stayOnFeaturedArticleFor(milliseconds: Long) = apply {
        makeViewVisibleAndClick(
            viewId = R.id.view_featured_article_card_content_container,
            parentViewId = R.id.feed_view
        )
        Thread.sleep(milliseconds)
    }

    fun scrollToSuggestedEditsIfVisible() = apply {
        try {
            scrollToRecyclerView(title = SUGGESTED_EDITS)
            clickAddArticleDescription()
            pressBack()
        } catch (e: Exception) {
            Log.e("ScrollError:", "Suggested edits not visible or espresso cannot find it.")
        }
    }

    private fun changWatchListArticleExpiryFromTheSnackBar() = apply {
        clickOnDisplayedViewWithIdAnContentDescription(
            viewId = com.google.android.material.R.id.snackbar_action,
            "Change"
        )
        clickOnViewWithId(R.id.watchlistExpiryOneMonth)
        delay(TestConfig.DELAY_SHORT)
    }

    fun scrollToCardWithTitle(title: String, @IdRes viewId: Int = R.id.view_card_header_title) =
        apply {
            onView(withId(R.id.feed_view))
                .perform(
                    scrollTo<RecyclerView.ViewHolder>(
                        hasDescendant(
                            scrollToCardViewWithTitle(title, viewId)
                        )
                    )
                )
                .perform()
            delay(TestConfig.DELAY_MEDIUM)
        }

    fun swipeToRefresh() = apply {
        onView(withId(R.id.swipe_refresh_layout))
            .perform(ViewActions.swipeDown())
        delay(TestConfig.DELAY_SWIPE_TO_REFRESH)
    }

    fun scrollToItem(
        recyclerViewId: Int = R.id.feed_view,
        title: String,
        textViewId: Int = R.id.view_card_header_title,
        verticalOffset: Int = 200
    ) = apply {
        scrollToRecyclerView(
            recyclerViewId,
            title,
            textViewId,
            verticalOffset
        )
    }

    fun assertFeaturedArticleTitleColor() = apply {
        onView(allOf(
            withId(R.id.view_card_header_title),
            withText(Constants.FEATURED_ARTICLE)
        )).check(ColorAssertions.hasColor(R.attr.primary_color, isAttr = true, ColorAssertions.ColorType.TextColor))
    }

    fun assertTopReadTitleColor() = apply {
        onView(allOf(
            withId(R.id.view_card_header_title),
            withText(Constants.TOP_READ_ARTICLES)
        )).check(ColorAssertions.hasColor(R.attr.primary_color, isAttr = true, ColorAssertions.ColorType.TextColor))
    }

    private fun scrollToCardViewWithTitle(
        title: String,
        @IdRes textViewId: Int = R.id.view_card_header_title,
    ): Matcher<View> {
        var currentOccurrence = 0
        return object : BoundedMatcher<View, View>(View::class.java) {
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
    }
}
