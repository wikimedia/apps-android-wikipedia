package org.wikipedia.yearinreview

import org.wikipedia.analytics.ABTest

class YearInReviewReadingListAbTest : ABTest("yirReadingList", GROUP_SIZE_2) {

    fun getGroupName(): String {
        return when (group) {
            GROUP_2 -> "b" // test group
            else -> "a" // control
        }
    }

    fun isTestGroupUser(): Boolean {
        return group == GROUP_2
    }
}
