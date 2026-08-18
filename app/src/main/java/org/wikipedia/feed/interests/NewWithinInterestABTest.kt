package org.wikipedia.feed.interests

import org.wikipedia.analytics.ABTest

class NewWithinInterestABTest : ABTest("new-within-interest", GROUP_SIZE_2) {
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
