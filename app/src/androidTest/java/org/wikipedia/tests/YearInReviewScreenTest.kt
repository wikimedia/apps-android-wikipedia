package org.wikipedia.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.base.BaseTest
import org.wikipedia.robots.feature.YearInReviewRobot
import org.wikipedia.yearinreview.YearInReviewActivity

@LargeTest
@RunWith(AndroidJUnit4::class)
class YearInReviewScreenTest : BaseTest<YearInReviewActivity>(
    activityClass = YearInReviewActivity::class.java
) {

    private val yearInReviewRobot = YearInReviewRobot()

    @Test
    fun runTest() {
        yearInReviewRobot
            .setComposeTestRule(composeTestRule)
    }
}
