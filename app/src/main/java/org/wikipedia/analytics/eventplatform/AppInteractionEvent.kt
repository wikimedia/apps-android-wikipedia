package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Suppress("unused")
@Serializable
@OptIn(ExperimentalSerializationApi::class)
@SerialName("/analytics/mobile_apps/app_interaction/1.0.0")
class AppInteractionEvent(
    private val action: String,
    private val active_interface: String,
    private val action_data: String,
    private val primary_language: String,
    private val wiki_id: String,
    @Transient private val streamName: String = "",
    @EncodeDefault(EncodeDefault.Mode.ALWAYS) private val platform: String = "android",
) : MobileAppsEvent(streamName)
