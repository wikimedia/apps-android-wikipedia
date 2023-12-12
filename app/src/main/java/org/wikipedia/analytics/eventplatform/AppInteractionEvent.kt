package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Suppress("unused")
@Serializable
@SerialName("/analytics/mobile_apps/app_interaction/1.0.0")
class AppInteractionEvent(
    private val action: String,
    private val active_interface: String,
    private val action_data: String,
    private val primary_language: String,
    private val wiki_id: String,
    private var platform: String,
) : MobileAppsEvent(STREAM_NAME) {
    companion object {
        var STREAM_NAME = "app_donor_experience"
    }
}
