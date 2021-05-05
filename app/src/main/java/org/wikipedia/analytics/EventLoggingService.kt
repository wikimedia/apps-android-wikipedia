package org.wikipedia.analytics

import android.net.Uri
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory
import org.wikipedia.settings.Prefs
import org.wikipedia.util.ReleaseUtil
import org.wikipedia.util.log.L

class EventLoggingService private constructor() {

    fun log(event: JSONObject?) {
        if (!Prefs.isEventLoggingEnabled() || !WikipediaApp.getInstance().isOnline) {
            // Do not send events if the user opted out of EventLogging or the device is offline.
            return
        }
        Completable.fromAction {
                    val eventStr = event.toString()
                    val dataURL = Uri.parse(EVENTLOG_URL)
                            .buildUpon().query(eventStr)
                            .build().toString()
                    if (ReleaseUtil.isDevRelease) {
                        L.d(eventStr)
                    }
                    if (dataURL.length > MAX_URL_LEN) {
                        L.logRemoteErrorIfProd(RuntimeException("EventLogging max length exceeded"))
                    }
                    val request = Request.Builder().url(dataURL).post(EMPTY_REQ).build()
                    OkHttpConnectionFactory.client.newCall(request).execute().close()
                }
                .subscribeOn(Schedulers.io())
                .subscribe({}) { throwable -> L.d("Lost EL data: " + event.toString(), throwable) }
    }

    companion object {
        private const val EVENTLOG_URL_PROD = "https://meta.wikimedia.org/beacon/event"
        private const val EVENTLOG_URL_DEV = "https://deployment.wikimedia.beta.wmflabs.org/beacon/event"

        // https://github.com/wikimedia/mediawiki-extensions-EventLogging/blob/8b3cb1b/modules/ext.eventLogging.core.js#L57
        private const val MAX_URL_LEN = 2000
        private val EMPTY_REQ = RequestBody.create(null, ByteArray(0))
        private val EVENTLOG_URL = if (ReleaseUtil.isDevRelease) EVENTLOG_URL_DEV else EVENTLOG_URL_PROD
        val instance = EventLoggingService()
    }
}
