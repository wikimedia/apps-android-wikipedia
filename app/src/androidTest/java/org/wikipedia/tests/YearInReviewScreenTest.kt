package org.wikipedia.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.base.BaseTest
import org.wikipedia.base.DataInjector
import org.wikipedia.robots.feature.YearInReviewRobot
import org.wikipedia.yearinreview.YearInReviewActivity

@LargeTest
@RunWith(AndroidJUnit4::class)
class YearInReviewScreenTest : BaseTest<YearInReviewActivity>(
    activityClass = YearInReviewActivity::class.java,
    dataInjector = DataInjector()
) {
    val yearInReviewRobot = YearInReviewRobot()

    @Test
    fun runTest1() {
        yearInReviewRobot
            .getStarted()
            .setLeftMargin()
            .setRightMargin()
    }

    @Test
    fun runTest2() {
    }
}
