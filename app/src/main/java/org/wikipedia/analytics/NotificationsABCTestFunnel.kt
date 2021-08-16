package org.wikipedia.analytics

import org.wikipedia.WikipediaApp

class NotificationsABCTestFunnel :
    Funnel(WikipediaApp.getInstance(), SCHEMA_NAME, REV_ID, SAMPLE_LOG_ALL) {
    private val abTestImpl = ABTestFunnel("notificationIcons", ABTestFunnel.GROUP_SIZE_3)

    val aBTestGroup: Int get() = abTestImpl.aBTestGroup

    fun logShow() {
        val item = when(aBTestGroup) {
            0 -> "inbox"
            1 -> "bell"
            else -> "more"
        }
        log(
            "action", "show",
            "menuItem", item
        )
    }

    fun logSelect() {
        val item = when (aBTestGroup) {
            0 -> "inbox"
            1 -> "bell"
            else -> "more"
        }
        log(
            "action", "select",
            "menuItem", item
        )
    }

    companion object {
        private const val SCHEMA_NAME = "MobileWikiAppNavMenu"
        private const val REV_ID = 21870873
    }
}
