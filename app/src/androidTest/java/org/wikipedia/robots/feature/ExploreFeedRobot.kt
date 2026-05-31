package org.wikipedia.robots.feature

import BaseRobot
import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.contrib.RecyclerViewActions.scrollToPosition
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import com.google.android.material.imageview.ShapeableImageView
import org.hamcrest.Matchers.allOf
import org.wikipedia.R
import org.wikipedia.TestConstants
import org.wikipedia.TestConstants.SUGGESTED_EDITS
import org.wikipedia.base.TestConfig
import org.wikipedia.base.TestThemeColorType
import org.wikipedia.base.TestWikipediaColors
import org.wikipedia.base.utils.ColorAssertions
import org.wikipedia.theme.Theme

class ExploreFeedRobot : BaseRobot() {
    fun clickOnFeaturedArticle(position: Int = 0) = apply {
        onView(withId(R.id.feed_view))
            .perform(
                RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                    position,
                    list.moveClickIntoViewAndClick(R.id.view_featured_article_card_content_container)
                )
            )
        delay(TestConfig.DELAY_SHORT)
    }

    fun clickTodayOnWikipedia(position: Int) = apply {
        onView(withId(R.id.feed_view))
            .perform(
                RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                    position,
                    list.moveClickIntoViewAndClick(R.id.footerActionButton)
                )
            )
        delay(TestConfig.DELAY_LARGE)
    }

    fun clickTopReadArticle(position: Int) = apply {
        onView(withId(R.id.feed_view))
            .perform(
                actionOnItemAtPosition<RecyclerView.ViewHolder>(
                    position,
                    list.clickNestedItem(R.id.view_list_card_list, 0)
                )
            )
    }

    fun clickPictureOfTheDay(position: Int) = apply {
        onView(withId(R.id.feed_view))
            .perform(
                RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                    position,
                    list.moveClickIntoViewAndClick(R.id.view_featured_image_card_content_container)
                )
            )
        delay(TestConfig.DELAY_SHORT)
    }

    fun clickNewsArticle(position: Int) = apply {
        onView(withId(R.id.feed_view))
            .perform(
                RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                    position,
                    list.clickNestedItem(R.id.news_cardview_recycler_view, 0)
                )
            )
        delay(TestConfig.DELAY_SHORT)
    }

    fun clickOnThisDayCard(position: Int) = apply {
        onView(withId(R.id.feed_view))
            .perform(
                actionOnItemAtPosition<RecyclerView.ViewHolder>(
                    position,
                    list.moveClickIntoViewAndClick(R.id.on_this_day_card_view_click_container)
                )
            )
        delay(TestConfig.DELAY_SHORT)
    }

    fun clickRandomArticle(position: Int) = apply {
        onView(withId(R.id.feed_view))
            .perform(
                RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                    position,
                    list.moveClickIntoViewAndClick(R.id.view_featured_article_card_content_container)
                )
            )
        delay(TestConfig.DELAY_SHORT)
    }

    fun clickBecauseYouReadArticle(position: Int) = apply {
        onView(withId(R.id.feed_view))
            .perform(
                RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                    position,
                    list.clickNestedItem(R.id.view_list_card_list, 0)
                )
            )
        delay(TestConfig.DELAY_SHORT)
    }

    fun verifyFeedViewSize(expectedCount: Int) = apply {
        list.verifyRecyclerViewItemCount(
            viewId = R.id.feed_view,
            expectedCount = expectedCount
        )
    }

    fun pressBack() = apply {
        goBack()
        delay(TestConfig.DELAY_LARGE)
    }

    fun navigateUp() = apply {
        click.onDisplayedViewWithContentDescription("Navigate up")
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
        delay(TestConfig.DELAY_SHORT)
    }

    fun stayOnFeaturedArticleFor(milliseconds: Long) = apply {
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
        onView(withId(R.id.feed_view)).perform(scrollToPosition<RecyclerView.ViewHolder>(0))
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
        onView(
            allOf(
                withId(R.id.view_card_header_title),
                withText(TestConstants.FEATURED_ARTICLE)
            )
        ).check(ColorAssertions.hasColor(color, ColorAssertions.ColorType.TextColor))
    }

    fun assertTopReadTitleColor(theme: Theme) = apply {
        val color = TestWikipediaColors.getGetColor(theme, colorType = TestThemeColorType.PRIMARY)
        onView(
            allOf(
                withId(R.id.view_card_header_title),
                withText(TestConstants.TOP_READ_ARTICLES)
            )
        ).check(ColorAssertions.hasColor(color, ColorAssertions.ColorType.TextColor))
    }

    fun longClickFeaturedArticleCardContainer() = apply {
        scroll.toViewAndMakeVisibleAndLongClick(
            viewId = R.id.view_featured_article_card_content_container,
            parentViewId = R.id.feed_view
        )
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

    fun scrollAndPerform(
        viewIdRes: Int = R.id.feed_view,
        title: String,
        action: ExploreFeedRobot.(Int) -> Unit = {}
    ) = apply {
        list.scrollAndPerform(viewIdRes, title) { position ->
            action(position)
        }
    }

    fun verifyTopReadArticleIsGreyedOut(theme: Theme) = apply {
        delay(TestConfig.DELAY_SHORT)
        onView(
            allOf(
                withId(R.id.view_list_card_list),
                isDescendantOfA(withId(R.id.feed_view)),
                isDisplayed()
            )
        ).check { view, _ ->
            val recyclerView = view as RecyclerView
            val viewHolder = recyclerView.findViewHolderForAdapterPosition(1)
                ?: throw AssertionError("No viewHolder found at position 0")
            val imageView =
                viewHolder.itemView.findViewById<ShapeableImageView>(R.id.view_list_card_item_image)
                    ?: throw AssertionError("No ImageView found with id view_list_card_item_image")
            val color = TestWikipediaColors.getGetColor(theme, TestThemeColorType.BORDER)
            ColorAssertions.hasColor(
                colorResId = color,
                colorType = ColorAssertions.ColorType.ShapeableImageViewColor
            ).check(imageView, null)
        }
    }
}
