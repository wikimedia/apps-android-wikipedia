package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.wikipedia.auth.AccountUtil

@Suppress("unused")
@Serializable
sealed class MobileAppsEvent(@Transient private val _streamName: String = "") : MobileAppsEventBase(_streamName) {

    @SerialName("is_anon") @Required private val anon = !AccountUtil.isLoggedIn
}
