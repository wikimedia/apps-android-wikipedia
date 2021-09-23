package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/** Base class for an Event Platform event.  */
@Serializable
open class Event(@Transient val stream: String = "") {

    @SerialName("app_session_id")
    var sessionId: String? = null

    @SerialName("app_install_id")
    var appInstallId: String? = null

    private val meta: Meta = Meta(stream)

    var dt: String? = null

    @Serializable
    private class Meta(val stream: String)
}
