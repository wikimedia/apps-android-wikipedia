package org.wikipedia.yearinreview.data

import org.wikipedia.analytics.ABTest

class YearInReviewEndScreenABTest : ABTest("year-in-review-end-screen", GROUP_SIZE_2) {
    override fun getGroupName(): String {
        return when (group) {
            GROUP_2 -> "treatment"
            else -> "control"
        }
    }

    fun isTestGroupUser(): Boolean {
        return group == GROUP_2
    }
}
