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

        suspend fun unsubscribePushToken(csrfToken: String, pushToken: String): MwQueryResponse {
            // stub
            return MwQueryResponse()
        }
    }
}
