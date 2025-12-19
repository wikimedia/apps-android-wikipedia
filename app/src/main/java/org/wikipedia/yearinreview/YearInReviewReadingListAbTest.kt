package org.wikipedia.yearinreview

import org.wikipedia.analytics.ABTest

class YearInReviewReadingListAbTest : ABTest("yirReadingList", GROUP_SIZE_2) {
    fun isTestGroupUser(): Boolean {
        return group == GROUP_2
    }
}
