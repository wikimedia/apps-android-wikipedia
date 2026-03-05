package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.dataclient.okhttp.HttpStatusException

class ClientErrorEvent {

    fun logError(throwable: Throwable) {

        val event = when (throwable) {
            is HttpStatusException -> ClientErrorEventImpl(throwable.message, errorClass = throwable::class.simpleName)
            else -> ClientErrorEventImpl(throwable.message, errorClass = throwable::class.simpleName)
        }

        EventPlatformClient.submit(event)
    }

    @Suppress("unused")
    @Serializable
    @SerialName("/mediawiki/client/error/2.0.0")
    class ClientErrorEventImpl(
        private val message: String?,
        @SerialName("error_class") private val errorClass: String? = null,
        @SerialName("error_context") private val errorContext: String? = null
    ) : Event("mediawiki.client.error")
}
