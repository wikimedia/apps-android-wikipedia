package org.wikipedia.feed.interests

import org.wikipedia.analytics.ABTest
import org.wikipedia.settings.RemoteConfig
import org.wikipedia.util.ReleaseUtil

class NewWithinInterestABTest : ABTest("new-within-interest-redux", GROUP_SIZE_2) {
    override fun getGroupName(): String {
        return when (group) {
            GROUP_2 -> "treatment"
            else -> "control"
        }
    }

    fun isTestGroupUser(): Boolean {
        return group == GROUP_2
    }

    fun isTestActive(): Boolean {
        return !ReleaseUtil.isPreProdRelease && RemoteConfig.config.androidv1?.newArticlesWithinInterestEnabled ?: false
    }
}
