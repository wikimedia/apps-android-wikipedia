package org.wikipedia.analytics.metricsplatform

import org.wikipedia.analytics.ABTest

// TODO: update abTestName
class RecommendedContentABCTest : ABTest("mBART25", GROUP_SIZE_3) {

    override fun assignGroup() {
        super.assignGroup()
        // TODO: log assigned group
    }
}
