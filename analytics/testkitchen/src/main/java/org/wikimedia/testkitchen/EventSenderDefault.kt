package org.wikimedia.testkitchen

import androidx.core.net.toUri
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.wikimedia.testkitchen.config.DestinationEventService
import org.wikimedia.testkitchen.event.Event
import java.io.IOException

class EventSenderDefault(
    private val json: Json,
    private val httpClient: OkHttpClient,
    private val logger: LogAdapter,
    private val isDebug: Boolean = false
) : EventSender {
    override suspend fun sendEvents(destinationEventService: DestinationEventService, events: List<Event>) {
        val baseUri = (destinationEventService.baseUri + "/v1/events" + (if (!isDebug) "?hasty=true" else "")).toUri()
        val eventStr = json.encodeToString(events)
        val request = Request.Builder()
            .url(baseUri.toString())
            .header("Accept", "application/json")
            .header(
                "User-Agent",
                "TestKitchenClient/Kotlin " + TestKitchenClient.LIBRARY_VERSION
            )
            .post(eventStr.toRequestBody("application/json".toMediaTypeOrNull()))
            .build()
        val response = httpClient.newCall(request).execute()
        val status = response.code
        val body = response.body
        if (!response.isSuccessful || status == 207) {
            // In the case of a multi-status response (207), it likely means that one or more
            // events were rejected. In such a case, the error is actually contained in
            // the normal response body.
            throw IOException(body?.string().orEmpty())
        }
        logger.info("Sent " + events.size + " events successfully.")
    }
}
