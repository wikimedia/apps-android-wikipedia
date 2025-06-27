package org.wikimedia.metrics_platform.context

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Client metadata context fields.
 *
 * ClientData includes immutable and mutable contextual data from the client.
 * This metadata is added to every event submission when queued for processing.
 *
 * All fields of nested data objects are nullable, and boxed types are used in place of their equivalent primitive types
 * to avoid unexpected default values from being used where the true value is null.
 */
@Serializable
open class ClientData (
    @SerialName("agent") val agentData: AgentData = AgentData(),
    @SerialName("page") val pageData: PageData = PageData(),
    @SerialName("mediawiki") val mediawikiData: MediawikiData = MediawikiData(),
    @SerialName("performer") val performerData: PerformerData = PerformerData(),
    @SerialName("domain") val domain: String? = null
)
