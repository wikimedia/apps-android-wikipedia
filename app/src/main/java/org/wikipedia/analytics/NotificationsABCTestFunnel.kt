package org.wikipedia.analytics

import androidx.core.view.isVisible
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.main.MainActivity
import org.wikipedia.page.PageActivity
import org.wikipedia.settings.Prefs

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

    fun adaptPageActivity(activity: PageActivity) {
        when (aBTestGroup) {
            0 -> {
                activity.binding.pageToolbarButtonNotifications.isVisible = false
            }
            1 -> {
                activity.binding.pageToolbarButtonNotifications.isVisible = false
            }
            else -> {
                activity.binding.pageToolbarButtonNotifications.isVisible = false
            }
        }
    }

    fun adaptMainActivity(activity: MainActivity) {

    }

    fun adaptMainMenu() {

    }

    fun adaptPageMenu() {

    }

    companion object {
        private const val SCHEMA_NAME = "MobileWikiAppNavMenu"
        private const val REV_ID = 21870873
    }
}
