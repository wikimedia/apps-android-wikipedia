package org.wikipedia.analytics.metricsplatform

import org.wikipedia.analytics.ABTest

class RecommendedContentABCTest : ABTest("recommendedContent", GROUP_SIZE_3) {
    fun getGroupName(): String {
        return when (group) {
            GROUP_2 -> "general"
            GROUP_3 -> "personal"
            else -> "control"
        }
    }
}
