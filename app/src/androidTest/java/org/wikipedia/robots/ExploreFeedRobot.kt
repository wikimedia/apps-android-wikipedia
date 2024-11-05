package org.wikipedia.robots

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers
import org.hamcrest.Matchers.allOf
import org.wikipedia.R
import org.wikipedia.TestUtil.childAtPosition
import org.wikipedia.base.BaseRobot
import org.wikipedia.base.TestConfig

class ExploreFeedRobot : BaseRobot() {

    fun featuredArticleSeenAndSavedToReadingList() = apply {
        delay(TestConfig.DELAY_SHORT)
        onView(
            allOf(
                withId(R.id.view_featured_article_card_content_container),
            childAtPosition(childAtPosition(withClassName(Matchers.`is`("org.wikipedia.feed.featured.FeaturedArticleCardView")), 0), 1), isDisplayed()
            )
        )
            .perform(scrollTo(), longClick())
    }

    fun clickSave() = apply {
        onView(allOf(withId(R.id.title), withText("Save"),
            childAtPosition(childAtPosition(withId(androidx.appcompat.R.id.content), 0), 0), isDisplayed()))
            .perform(click())
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun scrollToPositionOnTheFeed(position: Int) = apply {
        scrollToPositionInRecyclerView(R.id.feed_view, position)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun topReadCardCanBeSeenAndSaved() = apply {
        onView(allOf(withId(R.id.view_list_card_list), childAtPosition(withId(R.id.view_list_card_list_container), 0)))
            .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(1, longClick()))
    }
}
