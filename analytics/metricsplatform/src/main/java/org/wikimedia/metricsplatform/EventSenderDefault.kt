package org.wikimedia.metricsplatform

import android.net.Uri
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.wikimedia.metricsplatform.event.EventProcessed
import java.io.IOException

class EventSenderDefault(
    private val json: Json,
    private val httpClient: OkHttpClient
) : EventSender {
    override fun sendEvents(baseUri: Uri, events: List<EventProcessed>) {
        val request = Request.Builder()
            .url(baseUri.toString())
            .header("Accept", "application/json")
            .header(
                "User-Agent",
                "Metrics Platform Client/Java " + MetricsClient.METRICS_PLATFORM_LIBRARY_VERSION
            )
            .post(json.encodeToString(events).toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        httpClient.newCall(request).execute().use { response ->
            val status = response.code
            val body = response.body
            if (!response.isSuccessful || status == 207) {
                // In the case of a multi-status response (207), it likely means that one or more
                // events were rejected. In such a case, the error is actually contained in
                // the normal response body.
                throw IOException(body?.string().orEmpty())
            }
            //log.log(java.util.logging.Level.INFO, "Sent " + events.size + " events successfully.")
        }
    }
}
