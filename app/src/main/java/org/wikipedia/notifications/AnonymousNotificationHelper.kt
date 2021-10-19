package org.wikipedia.notifications

import io.reactivex.rxjava3.core.Observable
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.settings.Prefs

object AnonymousNotificationHelper {

    fun observableForAnonUserInfo(wikiSite: WikiSite): Observable<MwQueryResponse> {
        // TODO: only if they've edited within the last 7 days
        return ServiceFactory.get(wikiSite).userInfo
    }

    fun shouldCheckAnonNotifications(response: MwQueryResponse): Boolean {
        val hasMessages = response.query?.userInfo?.messages == true
        if (hasMessages) {
            if (!response.query?.userInfo?.name.isNullOrEmpty()) {
                Prefs.lastAnonUserWithMessages = response.query?.userInfo?.name
            }
        }
        return hasMessages
    }



}
