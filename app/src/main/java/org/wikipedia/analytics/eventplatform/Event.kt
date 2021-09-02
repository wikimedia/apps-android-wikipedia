package org.wikipedia.analytics.eventplatform

import com.google.gson.annotations.SerializedName
import org.wikipedia.util.DateUtil.iso8601DateFormat
import java.util.*

/** Base class for an Event Platform event.  */
open class Event(private val schema: String, stream: String) {

    @SerializedName("app_session_id")
    var sessionId: String? = null

    @SerializedName("app_install_id")
    var appInstallId: String? = null

    private val meta: Meta?
    private val dt: String?
    val stream: String
        get() = meta!!.stream

    private class Meta(val stream: String)

    init {
        meta = Meta(stream)
        dt = iso8601DateFormat(Date())
    }
}
