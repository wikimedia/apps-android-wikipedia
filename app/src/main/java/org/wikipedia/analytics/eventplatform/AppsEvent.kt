package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
sealed class AppsEvent(@Transient val streamName: String = "") : Event(streamName) {

    @SerialName("app_session_id")
    var sessionId: String? = null

    @SerialName("app_install_id")
    var appInstallId: String? = null
}
