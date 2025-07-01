package org.wikimedia.metricsplatform

import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.wikimedia.metricsplatform.event.EventProcessed

class EventSenderDefault(private val gson: com.google.gson.Gson,
                         private val httpClient: OkHttpClient
) : EventSender {
    override fun sendEvents(baseUri: java.net.URL, events: List<EventProcessed>) {
        val request = okhttp3.Request.Builder()
            .url(baseUri)
            .header("Accept", "application/json")
            .header(
                "User-Agent",
                "Metrics Platform Client/Java " + MetricsClient.METRICS_PLATFORM_LIBRARY_VERSION
            )
            .post(RequestBody.create(gson.toJson(events), parse.parse("application/json")))
            .build()

        httpClient.newCall(request).execute().use { response ->
            val status: kotlin.Int = response.code
            val body: ResponseBody? = response.body
            if (!response.isSuccessful || status == 207) {
                // In the case of a multi-status response (207), it likely means that one or more
                // events were rejected. In such a case, the error is actually contained in
                // the normal response body.
                throw java.io.IOException(body.string())
            }
            //log.log(java.util.logging.Level.INFO, "Sent " + events.size + " events successfully.")
        }
    }
}
