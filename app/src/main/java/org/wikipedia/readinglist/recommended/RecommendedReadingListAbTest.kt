package org.wikipedia.readinglist.recommended

import org.wikipedia.analytics.ABTest
import org.wikipedia.util.ReleaseUtil

class RecommendedReadingListAbTest : ABTest("recommendedReadingList", GROUP_SIZE_2) {
    fun getGroupName(): String {
        return when (group) {
            GROUP_2 -> "b" // test group
            else -> "a" // control
        }
    }

    fun isTestGroupUser(): Boolean {
        return ReleaseUtil.isDevRelease || group == GROUP_2
    }
}
