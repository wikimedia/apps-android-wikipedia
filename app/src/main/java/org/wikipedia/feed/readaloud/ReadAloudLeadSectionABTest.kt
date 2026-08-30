package org.wikipedia.feed.readaloud

import org.wikipedia.analytics.ABTest

class ReadAloudLeadSectionABTest : ABTest("readAloudLeadSection", GROUP_SIZE_2) {
    override fun getGroupName(): String {
        return when (group) {
            GROUP_2 -> "test"
            else -> "control"
        }
    }

    fun isTestGroupUser(): Boolean {
        return group == GROUP_2
    }
}
