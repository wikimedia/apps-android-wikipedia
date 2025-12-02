package org.wikipedia.robots.feature

import BaseRobot
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import com.google.android.material.imageview.ShapeableImageView
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.wikipedia.R
import org.wikipedia.TestConstants
import org.wikipedia.TestConstants.SUGGESTED_EDITS
import org.wikipedia.TestUtil.childAtPosition
import org.wikipedia.base.TestConfig
import org.wikipedia.base.TestThemeColorType
import org.wikipedia.base.TestWikipediaColors
import org.wikipedia.base.utils.ColorAssertions
import org.wikipedia.theme.Theme

class ExploreFeedRobot : BaseRobot() {
    fun clickOnThisDayCard() = apply {
        onView(allOf(
            withId(R.id.on_this_day_card_view_click_container),
            isDescendantOfA(withId(R.id.feed_view)),
            isDisplayed()
        )).perform(scrollTo(), click())
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun verifyFeedViewSize(expectedCount: Int) = apply {
        list.verifyRecyclerViewItemCount(
            viewId = R.id.feed_view,
            expectedCount = expectedCount
        )
    }

    fun clickRandomArticle() = apply {
        // Random article card seen and saved to reading lists
        click.onViewWithId(R.id.articleImage)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun pressBack() = apply {
        goBack()
        delay(TestConfig.DELAY_LARGE)
    }

    fun navigateUp() = apply {
        click.onDisplayedViewWithContentDescription("Navigate up")
    }

    fun clickTopReadArticle() = apply {
        val nestedListMatcher = allOf(
            withId(R.id.view_list_card_list),
            isDescendantOfA(withId(R.id.feed_view)),
            isDisplayed()
        )
        onView(nestedListMatcher).check(matches(isDisplayed()))
        onView(nestedListMatcher).perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click())
        )
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
        click.onDisplayedViewWithContentDescription(description = "Add article descriptions")
    }

    fun openOverflowMenuItem() = apply {
        click.onViewWithId(R.id.page_toolbar_button_show_overflow_menu)
        delay(TestConfig.DELAY_SHORT)
    }

    fun verifyFeaturedArticleImageIsNotVisible() = apply {
        verify.viewWithIdIsNotVisible(viewId = R.id.articleImage)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun clickPictureOfTheDay() = apply {
        click.onViewWithId(R.id.view_featured_image_card_image)
        delay(TestConfig.DELAY_SHORT)
    }

    fun clickTodayOnWikipedia() = apply {
        click.onViewWithIdAndContainsString(R.id.footerActionButton, text = "Today on Wikipedia")
        delay(TestConfig.DELAY_LARGE)
    }

    fun clickOnFeaturedArticle() = apply {
        click.onViewWithId(R.id.articleImage)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun stayOnFeaturedArticleFor(milliseconds: Long) = apply {
        scroll.toViewAndMakeVisibleAndClick(
            viewId = R.id.view_featured_article_card_content_container,
            parentViewId = R.id.feed_view
        )
        Thread.sleep(milliseconds)
    }

    fun scrollToSuggestedEditsIfVisible() = apply {
        try {
            list.scrollToRecyclerView(title = SUGGESTED_EDITS)
            clickAddArticleDescription()
            pressBack()
        } catch (e: Exception) {
            Log.e("ScrollError:", "Suggested edits not visible or espresso cannot find it.")
        }
    }

    fun swipeToRefresh() = apply {
        onView(withId(R.id.swipe_refresh_layout))
            .perform(ViewActions.swipeDown())
        delay(TestConfig.DELAY_SWIPE_TO_REFRESH)
    }

    fun scrollToAndPerform(
        title: String,
        shouldSwipeMore: Boolean = false,
        action: (ExploreFeedRobot.() -> Unit)? = null
    ) = apply {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        val appScrollable = UiScrollable(UiSelector().scrollable(true))
        appScrollable.setMaxSearchSwipes(10)
        try {
            appScrollable.setAsVerticalList()
            appScrollable.scrollIntoView(UiSelector().text(title))
            if (shouldSwipeMore) {
                device.swipe(
                    device.displayWidth / 2,
                    device.displayHeight * 3 / 5,
                    device.displayWidth / 2,
                    device.displayHeight * 2 / 5,
                    15
                )
            }
        } catch (e: Exception) {
            Log.e("ExploreFeed", "Scroll attempt failed: ${e.message}")
        }
        Thread.sleep(500)
        action?.invoke(this@ExploreFeedRobot)
    }

    fun assertFeaturedArticleTitleColor(theme: Theme) = apply {
        val color = TestWikipediaColors.getGetColor(theme, colorType = TestThemeColorType.PRIMARY)
        onView(allOf(
            withId(R.id.view_card_header_title),
            withText(TestConstants.FEATURED_ARTICLE)
        )).check(ColorAssertions.hasColor(color, ColorAssertions.ColorType.TextColor))
    }

    fun assertTopReadTitleColor(theme: Theme) = apply {
        val color = TestWikipediaColors.getGetColor(theme, colorType = TestThemeColorType.PRIMARY)
        onView(allOf(
            withId(R.id.view_card_header_title),
            withText(TestConstants.TOP_READ_ARTICLES)
        )).check(ColorAssertions.hasColor(color, ColorAssertions.ColorType.TextColor))
    }

    fun longClickFeaturedArticleCardContainer() = apply {
        scroll.toViewAndMakeVisibleAndLongClick(viewId = R.id.view_featured_article_card_content_container, parentViewId = R.id.feed_view)
        delay(TestConfig.DELAY_SHORT)
    }

    fun clickSave() = apply {
        try {
            click.onViewWithText("Save")
            delay(TestConfig.DELAY_SHORT)
        } catch (e: Exception) {
            Log.e("ExploreFeedRobotError:", "Save text is not found.")
        }
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

    fun verifyTopReadArticleIsGreyedOut(theme: Theme) = apply {
        delay(TestConfig.DELAY_MEDIUM)
        onView(allOf(
            withId(R.id.view_list_card_list),
            isDescendantOfA(withId(R.id.feed_view)),
            isDisplayed()
        )).check { view, _ ->
                val recyclerView = view as RecyclerView
                val viewHolder = recyclerView.findViewHolderForAdapterPosition(1)
                    ?: throw AssertionError("No viewHolder found at position 0")
                val imageView = viewHolder.itemView.findViewById<ShapeableImageView>(R.id.view_list_card_item_image)
                    ?: throw AssertionError("No ImageView found with id view_list_card_item_image")
                val color = TestWikipediaColors.getGetColor(theme, TestThemeColorType.BORDER)
                ColorAssertions.hasColor(
                    colorResId = color,
                    colorType = ColorAssertions.ColorType.ShapeableImageViewColor
                ).check(imageView, null)
            }
    }
}
