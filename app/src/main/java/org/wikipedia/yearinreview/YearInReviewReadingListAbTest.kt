package org.wikipedia.yearinreview

import org.wikipedia.analytics.ABTest

class YearInReviewReadingListAbTest : ABTest("yirReadingList", GROUP_SIZE_2) {
    fun isTestGroupUser(): Boolean {
        return group == GROUP_2
    }

    override fun getGroupName(): String {
        return if (isTestGroupUser()) "android_yir_2025_b" else "android_yir_2025_a"
    }
}
