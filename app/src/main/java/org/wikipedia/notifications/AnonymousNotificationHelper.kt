package org.wikipedia.notifications

import io.reactivex.rxjava3.core.Observable
import org.wikipedia.auth.AccountUtil
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import java.time.Instant
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

object AnonymousNotificationHelper {
    private const val NOTIFICATION_DURATION_DAYS = 7L

    fun onEditSubmitted() {
        if (!AccountUtil.isLoggedIn) {
            Prefs.lastAnonEditTime = Instant.now()
        }
    }

    fun observableForAnonUserInfo(wikiSite: WikiSite): Observable<MwQueryResponse> {
        return if (Prefs.lastAnonEditTime.until(Instant.now(), ChronoUnit.DAYS) < NOTIFICATION_DURATION_DAYS) {
            ServiceFactory.get(wikiSite).userInfo
        } else {
            Observable.just(MwQueryResponse())
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
        response.query?.firstPage()?.revisions?.firstOrNull()?.localDateTime?.let {
            if (it.until(LocalDateTime.now(), ChronoUnit.DAYS) < NOTIFICATION_DURATION_DAYS) {
                Prefs.hasAnonymousNotification = true
                Prefs.lastAnonNotificationTime = Instant.now()
                Prefs.lastAnonNotificationLang = title.wikiSite.languageCode
                return true
            }
        }
        return false
    }

    fun isWithinAnonNotificationTime(): Boolean {
        return Prefs.lastAnonNotificationTime.until(Instant.now(), ChronoUnit.DAYS) < NOTIFICATION_DURATION_DAYS
    }
}
