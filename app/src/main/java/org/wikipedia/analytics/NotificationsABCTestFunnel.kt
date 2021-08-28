package org.wikipedia.analytics

import org.wikipedia.WikipediaApp

class NotificationsABCTestFunnel :
    Funnel(WikipediaApp.instance, SCHEMA_NAME, REV_ID, SAMPLE_LOG_ALL) {
    private val abTestImpl = ABTestFunnel("notificationIcons", ABTestFunnel.GROUP_SIZE_3)

    val aBTestGroup: Int get() = abTestImpl.aBTestGroup

    fun logShow() {
        val item = when (aBTestGroup) {
            0 -> TEST_GROUP_INBOX
            1 -> TEST_GROUP_BELL
            else -> TEST_GROUP_MORE
        }
        log(
            "action", "show",
            "menuItem", item
        )
    }

    fun logSelect() {
        val item = when (aBTestGroup) {
            0 -> TEST_GROUP_INBOX
            1 -> TEST_GROUP_BELL
            else -> TEST_GROUP_MORE
        }
        log(
            "action", "select",
            "menuItem", item
        )
    }

    companion object {
        private const val SCHEMA_NAME = "MobileWikiAppNavMenu"
        private const val REV_ID = 21870873

        private const val TEST_GROUP_INBOX = "inbox"
        private const val TEST_GROUP_BELL = "bell"
        private const val TEST_GROUP_MORE = "more"
    }
}
