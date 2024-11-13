package org.wikipedia.analytics.metricsplatform

import org.wikipedia.analytics.ABTest

class RabbitHolesABCTest : ABTest("rabbitHoles", GROUP_SIZE_3) {
    fun getGroupName(): String {
        return when (group) {
            GROUP_2 -> "search"
            GROUP_3 -> "list"
            else -> "control"
        }
    }
}
