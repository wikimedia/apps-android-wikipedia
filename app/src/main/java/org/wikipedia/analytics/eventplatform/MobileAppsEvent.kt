package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.wikipedia.auth.AccountUtil
import org.wikipedia.settings.Prefs

@Suppress("unused")
@Serializable
sealed class MobileAppsEvent(@Transient private val _streamName: String = "") : Event(_streamName) {

    @SerialName("is_anon") @Required private val anon = !AccountUtil.isLoggedIn
    @SerialName("app_session_id") @Required private val sessionId = EventPlatformClient.AssociationController.sessionId
    @SerialName("app_install_id") @Required private val appInstallId = Prefs.appInstallId.orEmpty()
}
