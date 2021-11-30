package org.wikipedia.notifications

import io.reactivex.rxjava3.core.Observable
import org.wikipedia.auth.AccountUtil
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DateUtil
import java.util.*
import java.util.concurrent.TimeUnit

object AnonymousNotificationHelper {
    private const val NOTIFICATION_DURATION_DAYS = 7L

    fun onEditSubmitted() {
        if (!AccountUtil.isLoggedIn) {
            Prefs.lastAnonEditTime = Date().time
        }
    }

    fun observableForAnonUserInfo(wikiSite: WikiSite): Observable<MwQueryResponse> {
        return if (Date().time - Prefs.lastAnonEditTime < TimeUnit.DAYS.toMillis(NOTIFICATION_DURATION_DAYS)) {
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
        response.query?.firstPage()?.revisions?.firstOrNull()?.timeStamp?.let {
            if (Date().time - DateUtil.iso8601DateParse(it).time < TimeUnit.DAYS.toMillis(NOTIFICATION_DURATION_DAYS)) {
                Prefs.hasAnonymousNotification = true
                Prefs.lastAnonNotificationTime = Date().time
                Prefs.lastAnonNotificationLang = title.wikiSite.languageCode
                return true
            }
        }
        return false
    }

    fun isWithinAnonNotificationTime(): Boolean {
        return Date().time - Prefs.lastAnonNotificationTime < TimeUnit.DAYS.toMillis(NOTIFICATION_DURATION_DAYS)
    }
}
