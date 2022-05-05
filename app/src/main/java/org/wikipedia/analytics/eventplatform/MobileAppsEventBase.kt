package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.wikipedia.settings.Prefs

@Serializable
sealed class MobileAppsEventBase(@Transient private val streamName: String = "") : Event(streamName) {

    @SerialName("app_session_id") private val sessionId: String
    @SerialName("app_install_id") private val appInstallId: String

    init {
        // Note: DO NOT join the declaration of these fields with the assignment. This seems to be
        // necessary for polymorphic serialization.
        sessionId = EventPlatformClient.AssociationController.sessionId
        appInstallId = Prefs.appInstallId.orEmpty()
    }
}
