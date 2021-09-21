package org.wikipedia.analytics.eventplatform

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.util.DateUtil
import java.util.*

/** Base class for an Event Platform event.  */
@Serializable
open class Event(@SerialName("\$schema") val schema: String, val stream: String) {

    @SerialName("app_session_id") @SerializedName("app_session_id")
    var sessionId: String? = null

    @SerialName("app_install_id") @SerializedName("app_install_id")
    var appInstallId: String? = null

    private val meta: Meta = Meta(stream)

    private val dt: String = DateUtil.iso8601DateFormat(Date())

    @Serializable
    private class Meta(val stream: String)
}
