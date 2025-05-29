package org.wikipedia.readinglist.recommended

import org.wikipedia.analytics.ABTest

class RecommendedReadingListAbTest : ABTest("recommendedReadingList", GROUP_SIZE_2) {
    fun getGroupName(): String {
        return when (group) {
            GROUP_2 -> "b" // test group
            else -> "a" // control
        }
    }
}
