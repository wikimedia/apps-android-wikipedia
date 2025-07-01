package org.wikimedia.metricsplatform.context

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Interaction data fields.
 *
 * Common interaction fields that describe the event being submitted. Most fields are nullable.
 */
@Serializable
class InteractionData(
    @SerialName("action") val action: String? = null,
    @SerialName("action_subtype") val actionSubtype: String? = null,
    @SerialName("action_source") val actionSource: String? = null,
    @SerialName("action_context") val actionContext: String? = null,
    @SerialName("element_id") val elementId: String? = null,
    @SerialName("element_friendly_name") val elementFriendlyName: String? = null,
    @SerialName("funnel_entry_token") val funnelEntryToken: String? = null,
    @SerialName("funnel_event_sequence_position") val funnelEventSequencePosition: Int? = null
)
