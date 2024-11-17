package org.wikipedia.push

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
