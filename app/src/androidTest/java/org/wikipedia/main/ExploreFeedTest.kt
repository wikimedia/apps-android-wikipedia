package org.wikipedia.main

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.base.BaseTest
import org.wikipedia.robots.ExploreFeedRobot

@LargeTest
@RunWith(AndroidJUnit4::class)
class ExploreFeedTest : BaseTest<MainActivity>(
    activityClass = MainActivity::class.java,
    isInitialOnboardingEnabled = false
) {

    private val exploreFeedRobot = ExploreFeedRobot()

    @Test
    fun runExploreFeedTest() {
        exploreFeedRobot
            .featuredArticleSeenAndSavedToReadingList()
            .clickSave()
            .scrollToPositionOnTheFeed(4)
            .topReadCardCanBeSeenAndSaved()
            .clickSave()
    }
}
