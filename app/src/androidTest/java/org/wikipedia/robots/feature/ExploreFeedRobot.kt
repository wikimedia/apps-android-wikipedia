package org.wikipedia.robots.feature

import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.contrib.RecyclerViewActions.scrollTo
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.hamcrest.Matchers.allOf
import org.wikipedia.R
import org.wikipedia.TestUtil.childAtPosition
import org.wikipedia.TestUtil.isDisplayed
import org.wikipedia.base.BaseRobot
import org.wikipedia.base.TestConfig
import org.wikipedia.test.loggedoutuser.ExploreFeedTest.Companion.MAIN_PAGE

class ExploreFeedRobot : BaseRobot() {
    fun longClickFeaturedArticleCardContainer() = apply {
        makeViewVisibleAndLongClick(
            viewId = R.id.view_featured_article_card_content_container,
            parentViewId = R.id.feed_view
        )
        delay(TestConfig.DELAY_SHORT)
    }

    fun clickSave() = apply {
        try {
            onView(
                allOf(
                    withId(R.id.title),
                    withText("Save"),
                    childAtPosition(childAtPosition(withId(androidx.appcompat.R.id.content), 0), 0),
                    isDisplayed()
                )
            )
                .perform(click())
        } catch (e: NoMatchingViewException) {
            Log.d("Test", "Save button not found or not visible")
            goBack()
        }
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun longClickNewsArticleAndSave() = apply {
        // News card seen and news item saved to reading lists
        onView(
            allOf(
                withId(R.id.news_story_items_recyclerview),
                childAtPosition(withClassName(Matchers.`is`("android.widget.LinearLayout")), 1)
            )
        )
            .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(0, longClick()))
        onView(
            allOf(
                withId(R.id.title),
                withText("Save"),
                childAtPosition(childAtPosition(withId(androidx.appcompat.R.id.content), 0), 0),
                isDisplayed()
            )
        )
            .perform(click())
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun longClickOnThisDayCardAndSave() = apply {
        // On this day card seen and saved to reading lists
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
            .perform(longClick())
        onView(
            allOf(
                withId(R.id.title),
                withText("Save"),
                childAtPosition(childAtPosition(withId(androidx.appcompat.R.id.content), 0), 0),
                isDisplayed()
            )
        )
            .perform(click())
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun longClickRandomArticleAndSave() = apply {
        // Random article card seen and saved to reading lists
        onView(
            allOf(
                withId(R.id.view_featured_article_card_content_container),
                childAtPosition(
                    childAtPosition(
                        withClassName(Matchers.`is`("org.wikipedia.feed.random.RandomCardView")),
                        0
                    ), 1
                ), isDisplayed()
            )
        )
            .perform(longClick())
        onView(
            allOf(
                withId(R.id.title),
                withText("Save"),
                childAtPosition(childAtPosition(withId(androidx.appcompat.R.id.content), 0), 0),
                isDisplayed()
            )
        )
            .perform(click())
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun pressBack() = apply {
        goBack()
        delay(TestConfig.DELAY_LARGE)
    }

    fun topReadCardCanBeSeenAndSaved() = apply {
        onView(
            allOf(
                withId(R.id.view_list_card_list),
                childAtPosition(withId(R.id.view_list_card_list_container), 0)
            )
        )
            .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(1, longClick()))
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
        clickOnDisplayedViewWithIdAnContentDescription(
            viewId = com.google.android.material.R.id.snackbar_action,
            "Change"
        )
        clickOnViewWithId(R.id.watchlistExpiryOneMonth)
        delay(TestConfig.DELAY_SHORT)
    }

    fun scrollToViewMainPageAndClick() = apply {
        scrollToCardWithTitleAndClick(title = MAIN_PAGE, viewId = R.id.footerActionButton)
    }

    fun scrollToCardWithTitleAndClick(title: String, @IdRes viewId: Int = R.id.view_card_header_title) = apply {
        onView(withId(R.id.feed_view))
            .perform(
                scrollTo<RecyclerView.ViewHolder>(
                    hasDescendant(
                        scrollToCardViewWithTitle(title, viewId)
                    )
                ),
                click()
            )
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun scrollToCardWithTitle(title: String, @IdRes viewId: Int = R.id.view_card_header_title) = apply {
        onView(withId(R.id.feed_view))
            .perform(
                scrollTo<RecyclerView.ViewHolder>(
                    hasDescendant(
                        scrollToCardViewWithTitle(title, viewId)
                    )
                )
            )
        delay(TestConfig.DELAY_MEDIUM)
    }

    private fun scrollToCardViewWithTitle(title: String, @IdRes viewId: Int = R.id.view_card_header_title): Matcher<View> {
        var currentOccurrence = 0
        return object : BoundedMatcher<View, View>(View::class.java) {
            override fun describeTo(description: Description?) {
                description?.appendText("Scroll to Card View with title: $title")
            }

            override fun matchesSafely(item: View?): Boolean {
                val titleView = item?.findViewById<TextView>(viewId)
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
