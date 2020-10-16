package org.wikipedia.push

import io.reactivex.rxjava3.core.Observable
import org.wikipedia.dataclient.mwapi.MwQueryResponse

class WikipediaFirebaseMessagingService {
    companion object {
        fun isUsingPush(): Boolean {
            return false
        }

        fun updateSubscription() {
            // stub
        }

        fun unsubscribePushToken(csrfToken: String, pushToken: String): Observable<MwQueryResponse> {
            // stub
            return Observable.just(MwQueryResponse())
        }
    }
}
