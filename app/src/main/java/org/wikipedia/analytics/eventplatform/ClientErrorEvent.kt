package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.Response

class ClientErrorEvent {

    fun logError(throwable: Throwable) {
        EventPlatformClient.submit(ClientErrorEventImpl(
            message = throwable.message,
            errorClass = throwable::class.simpleName,
            stackTrace = throwable.stackTrace.take(2).joinToString(",")
        ))
    }

    fun logHttpResponse(response: Response) {
        EventPlatformClient.submit(ClientErrorEventImpl(
            message = response.message.ifEmpty { this::class.simpleName },
            errorClass = this::class.simpleName,
            url = response.request.url.toString(),
            http = Http(
                method = response.request.method,
                protocol = response.protocol.toString(),
                statusCode = response.code
            )
        ))
    }

    @Suppress("unused")
    @Serializable
    @SerialName("/mediawiki/client/error/2.0.0")
    class ClientErrorEventImpl(
        private val message: String?,
        @SerialName("error_class") private val errorClass: String? = null,
        @SerialName("error_context") private val errorContext: String? = null,
        @SerialName("stack_trace") private val stackTrace: String? = null,
        private val http: Http? = null,
        private val url: String? = null
    ) : Event("mediawiki.client.error")

    @Suppress("unused")
    @Serializable
    class Http(
        private val method: String? = null,
        private val protocol: String? = null,
        @SerialName("status_code") private val statusCode: Int? = null
    )
}
