package org.wikipedia.robots.feature

import android.util.Log
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.google.android.material.imageview.ShapeableImageView
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.hamcrest.Matchers.allOf
import org.wikipedia.R
import org.wikipedia.TestUtil.childAtPosition
import org.wikipedia.TestUtil.isDisplayed
import org.wikipedia.base.BaseRobot
import org.wikipedia.base.ColorAssertions
import org.wikipedia.base.TestConfig
import org.wikipedia.feed.view.FeedView

class ExploreFeedRobot : BaseRobot() {
    fun longClickFeaturedArticleCardContainer() = apply {
        makeViewVisibleAndLongClick(viewId = R.id.view_featured_article_card_content_container, parentViewId = R.id.feed_view)
        delay(TestConfig.DELAY_SHORT)
    }

    fun clickSave() = apply {
        try {
            onView(allOf(withId(R.id.title), withText("Save"),
                childAtPosition(childAtPosition(withId(androidx.appcompat.R.id.content), 0), 0), isDisplayed()))
                .perform(click())
        } catch (e: NoMatchingViewException) {
            Log.d("Test", "Save button not found or not visible")
            goBack()
        }
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun longClickNewsArticleAndSave() = apply {
        // News card seen and news item saved to reading lists
        onView(allOf(withId(R.id.news_story_items_recyclerview),
            childAtPosition(withClassName(Matchers.`is`("android.widget.LinearLayout")), 1)))
            .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(0, longClick()))
        onView(allOf(withId(R.id.title), withText("Save"),
            childAtPosition(childAtPosition(withId(androidx.appcompat.R.id.content), 0), 0), isDisplayed()))
            .perform(click())
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun longClickOnThisDayCardAndSave() = apply {
        // On this day card seen and saved to reading lists
        onView(allOf(withId(R.id.on_this_day_page), childAtPosition(allOf(withId(R.id.event_layout),
            childAtPosition(withId(R.id.on_this_day_card_view_click_container), 0)), 3), isDisplayed()))
            .perform(longClick())
        onView(allOf(withId(R.id.title), withText("Save"),
            childAtPosition(childAtPosition(withId(androidx.appcompat.R.id.content), 0), 0), isDisplayed()))
            .perform(click())
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun longClickRandomArticleAndSave() = apply {
        // Random article card seen and saved to reading lists
        onView(allOf(withId(R.id.view_featured_article_card_content_container),
            childAtPosition(childAtPosition(withClassName(Matchers.`is`("org.wikipedia.feed.random.RandomCardView")), 0), 1), isDisplayed()))
            .perform(longClick())
        onView(allOf(withId(R.id.title), withText("Save"),
            childAtPosition(childAtPosition(withId(androidx.appcompat.R.id.content), 0), 0), isDisplayed()))
            .perform(click())
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun clickMainPageCard() = apply {
        onView(allOf(withId(R.id.footerActionButton), withText("View main page  "),
            childAtPosition(allOf(withId(R.id.card_footer), childAtPosition(withClassName(Matchers.`is`("android.widget.LinearLayout")), 1)), 0), isDisplayed()))
            .perform(click())
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun pressBack() = apply {
        goBack()
        delay(TestConfig.DELAY_LARGE)
    }

    fun scrollToPositionOnTheFeed(position: Int) = apply {
        onView(withId(R.id.feed_view))
            .perform(scrollToPositionForFeedView(position))
        delay(TestConfig.DELAY_LARGE)
    }

    fun scrollToPositionOnFeedAndClick(position: Int) = apply {
        onView(withId(R.id.feed_view))
            .perform(scrollToPositionForFeedView(position), click())
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun topReadCardCanBeSeenAndSaved() = apply {
        onView(allOf(withId(R.id.view_list_card_list), childAtPosition(withId(R.id.view_list_card_list_container), 0)))
            .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(1, longClick()))
    }

    fun clickNewsArticle() = apply {
        onView(allOf(withId(R.id.news_cardview_recycler_view), childAtPosition(withId(R.id.rtl_container), 1)))
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

    fun verifyFeaturedArticleImageIsNotVisible() = apply {
        checkViewDoesNotExist(viewId = R.id.articleImage)
        delay(TestConfig.DELAY_MEDIUM)
    }

    // @TODO: flaky test due to snackbar
    fun addOrRemoveToWatchList() = apply {
        val isVisible = onView(withText("Watch"))
        if (isVisible.isDisplayed()) {
            clickOnViewWithText("Watch")
            onView(withId(com.google.android.material.R.id.snackbar_text))
                .check(matches(isDisplayed()))
            changWatchListArticleExpiryFromTheSnackBar()
        } else {
            clickOnViewWithText("Unwatch")
            onView(withId(com.google.android.material.R.id.snackbar_text))
                .check(matches(isDisplayed()))
            delay(TestConfig.DELAY_SHORT)
        }
    }

    private fun changWatchListArticleExpiryFromTheSnackBar() = apply {
        clickOnDisplayedViewWithIdAnContentDescription(viewId = com.google.android.material.R.id.snackbar_action, "Change")
        clickOnViewWithId(R.id.watchlistExpiryOneMonth)
        delay(TestConfig.DELAY_SHORT)
    }

    private fun scrollToPositionForFeedView(position: Int): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return isAssignableFrom(FeedView::class.java)
            }

            override fun getDescription(): String {
                return "Scroll feed view to position $position"
            }

            override fun perform(uiController: UiController, view: View) {
                val feedView = view as FeedView
                val layoutManager = feedView.layoutManager as StaggeredGridLayoutManager

                feedView.smoothScrollToPosition(position)
                uiController.loopMainThreadForAtLeast(1000)

                layoutManager.scrollToPositionWithOffset(position, 0)
                uiController.loopMainThreadForAtLeast(500)
            }
        }
    }

    fun verifyTopReadArticleIsGreyedOut() = apply {
        delay(TestConfig.DELAY_MEDIUM)
        onView(withId(R.id.view_list_card_list))
            .check { view, _ ->
                val recyclerView = view as RecyclerView
                val viewHolder = recyclerView.findViewHolderForAdapterPosition(0)
                    ?: throw AssertionError("No viewHolder found at position 0")
                val imageView = viewHolder.itemView.findViewById<ShapeableImageView>(R.id.view_list_card_item_image)
                    ?: throw AssertionError("No ImageView found with id view_list_card_item_image")
                ColorAssertions.hasColor(
                    colorResOrAttr = R.attr.border_color,
                    isAttr = true,
                    colorType = ColorAssertions.ColorType.ShapeableImageViewColor
                ).check(imageView, null)
            }
    }
}
