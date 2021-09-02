package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.util.DateUtil.iso8601DateFormat
import java.util.*

/** Base class for an Event Platform event.  */
@Serializable
open class Event(private val schema: String, val stream: String) {

    @SerialName("app_session_id")
    var sessionId: String? = null

    @SerialName("app_install_id")
    var appInstallId: String? = null

    private val meta: Meta?
    private val dt: String?

    fun getStreamStr(): String? {
        return meta?.stream
    }

    @Serializable
    private class Meta(val stream: String)

    init {
        meta = Meta(stream)
        dt = iso8601DateFormat(Date())
    }
}
