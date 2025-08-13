package org.wikipedia.notifications

import org.wikipedia.auth.AccountUtil
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
object AnonymousNotificationHelper {
    private val NOTIFICATION_DURATION_DAYS = 7.days

    fun onEditSubmitted() {
        if (!AccountUtil.isLoggedIn) {
            Prefs.lastAnonEditTime = System.currentTimeMillis()
        }
    }

    suspend fun maybeGetAnonUserInfo(wikiSite: WikiSite): MwQueryResponse {
        val lastAnon = Instant.fromEpochMilliseconds(Prefs.lastAnonEditTime)
        return if (Clock.System.now() - lastAnon < NOTIFICATION_DURATION_DAYS) {
            ServiceFactory.get(wikiSite).getUserInfo()
        } else {
            MwQueryResponse()
        }
    }

    fun shouldCheckAnonNotifications(response: MwQueryResponse): Boolean {
        if (isWithinAnonNotificationTime()) {
            return false
        }
        val hasMessages = response.query?.userInfo?.messages == true
        if (hasMessages) {
            if (!response.query?.userInfo?.name.isNullOrEmpty()) {
                Prefs.lastAnonUserWithMessages = response.query?.userInfo?.name
            }
        }
        return hasMessages
    }

    fun anonTalkPageHasRecentMessage(response: MwQueryResponse, title: PageTitle): Boolean {
        response.query?.firstPage()?.revisions?.firstOrNull()?.timestamp?.let {
            val now = Clock.System.now()
            if (now - it < NOTIFICATION_DURATION_DAYS) {
                Prefs.hasAnonymousNotification = true
                Prefs.lastAnonNotificationTime = now.toEpochMilliseconds()
                Prefs.lastAnonNotificationLang = title.wikiSite.languageCode
                return true
            }
        }
        return false
    }

    fun isWithinAnonNotificationTime(): Boolean {
        val lastAnon = Instant.fromEpochMilliseconds(Prefs.lastAnonNotificationTime)
        return Clock.System.now() - lastAnon < NOTIFICATION_DURATION_DAYS
    }
}
