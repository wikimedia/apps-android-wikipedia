package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.wikipedia.auth.AccountUtil

@Suppress("unused")
@Serializable
sealed class MobileAppsEvent(@Transient private val _streamName: String = "") : MobileAppsEventBase(_streamName) {
    @SerialName("is_anon") private val anon: Boolean

    init {
        // Note: DO NOT join the declaration of these fields with the assignment. This seems to be
        // necessary for polymorphic serialization.
        anon = !AccountUtil.isLoggedIn
    }
}
