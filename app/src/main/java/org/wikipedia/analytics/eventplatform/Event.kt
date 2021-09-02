package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.util.DateUtil.iso8601DateFormat
import java.util.*

/** Base class for an Event Platform event.  */
@Serializable
open class Event(@SerialName("\$schema") val schema: String, stream: String) {

    @SerialName("app_session_id")
    var sessionId: String? = null

    @SerialName("app_install_id")
    var appInstallId: String? = null

    private val meta: Meta = Meta(stream)
    private val dt: String = iso8601DateFormat(Date())
    val stream: String
        get() = meta.stream

    fun getStreamStr(): String {
        return meta.stream
    }

    @Serializable
    private class Meta(val stream: String)
}
