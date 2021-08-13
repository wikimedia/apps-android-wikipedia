package org.wikipedia.analytics.eventplatform

import com.google.gson.annotations.SerializedName

/**
 * Gson POJO representing a response body from the Event Platform's intake API (EventGate).
 *
 * In case of success, a 201 (Success) or 202 (Hasty success) response will be returned with no
 * body. In the case of partial or total failure, failing events will be returned in the "invalid"
 * or "error" arrays in the response body.
 *
 * N.B. The response body will always be empty when sending events hastily. This class is provided
 * in anticipation of adding retry behavior for failed events in the future.
 */
class EventServiceResponse {

    @SerializedName("invalid") val invalidEvents: List<Any> = emptyList()
    @SerializedName("error") val errorEvents = emptyList<Any>()
}
