package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil

@Suppress("unused")
@Serializable
sealed class MobileAppsEvent(@Transient private val _streamName: String = "") : Event(_streamName) {

    @SerialName("is_anon") @Required private val anon = !AccountUtil.isLoggedIn
    @SerialName("app_session_id") @Required private val sessionId = EventPlatformClient.AssociationController.sessionId
    @SerialName("app_install_id") @Required private val appInstallId = WikipediaApp.instance.appInstallID
}

@Suppress("unused")
@Serializable
sealed class MobileAppsEventWithTemp(@Transient private val _streamName: String = "") : MobileAppsEvent(_streamName) {
    @SerialName("is_temp") @Required private val temp = AccountUtil.isTemporaryAccount
}
