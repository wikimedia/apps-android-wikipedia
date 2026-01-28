package org.wikimedia.testkitchen.event

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.wikimedia.testkitchen.config.sampling.SampleConfig
import org.wikimedia.testkitchen.context.AgentData
import org.wikimedia.testkitchen.context.ClientData
import org.wikimedia.testkitchen.context.InteractionData
import org.wikimedia.testkitchen.context.MediawikiData
import org.wikimedia.testkitchen.context.PageData
import org.wikimedia.testkitchen.context.PerformerData

@Suppress("CanConvertToMultiDollarString")
@Serializable
class Event {
    @Transient var clientData: ClientData = ClientData()
    @Transient var interactionData: InteractionData = InteractionData()

    @SerialName("\$schema") var schema: String = ""
    @SerialName("dt") var timestamp: String? = null
    val meta: Meta
    @SerialName("agent") var agentData: AgentData? = null
    @SerialName("page") var pageData: PageData? = null
    @SerialName("mediawiki") var mediawikiData: MediawikiData? = null
    @SerialName("performer") var performerData: PerformerData? = null
    var action: String? = null
    @SerialName("action_subtype") private var actionSubtype: String? = null
    @SerialName("action_source") private var actionSource: String? = null
    @SerialName("action_context") private var actionContext: String? = null
    @SerialName("element_id") private var elementId: String? = null
    @SerialName("element_friendly_name") private var elementFriendlyName: String? = null
    @SerialName("funnel_entry_token") private var funnelEntryToken: String? = null
    @SerialName("funnel_event_sequence_position") private var funnelEventSequencePosition: Int? = null

    // TODO?
    var sample: SampleConfig? = null

    @Serializable
    class Meta(
        var stream: String = "",
        var domain: String? = null
    )

    /**
     * Constructor for EventProcessed.
     *
     * @param schema schema id
     * @param stream stream name
     * @param dt timestamp
     * @param clientData agent, mediawiki, page, performer data
     * @param sample sample configuration
     * @param interactionData contextual data of the interaction
     *
     * Although 'setInteractionData()' sets the 'action' property for the event,
     * because 'action' is a nonnull property for both 'EventProcessed' and the
     * 'InteractionData' data object, removing the redundant setting of 'action'
     * triggers NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR in spotbugs so
     * leaving it as is for the time being rather than suppressing the error.
     */
    constructor(
        schema: String,
        stream: String,
        dt: String?,
        clientData: ClientData,
        interactionData: InteractionData,
        sample: SampleConfig? = null
    ) : super() {
        this.meta = Meta(stream)
        this.schema = schema
        this.sample = sample
        this.timestamp = dt
        applyClientData(clientData)
        applyInteractionData(interactionData)
    }

    fun applyClientData(clientData: ClientData) {
        this.clientData = clientData
        meta.domain = clientData.domain
        agentData = clientData.agentData
        pageData = clientData.pageData
        mediawikiData = clientData.mediawikiData
        performerData = clientData.performerData
    }

    private fun applyInteractionData(interactionData: InteractionData) {
        this.interactionData = interactionData
        this.action = interactionData.action
        this.actionContext = interactionData.actionContext
        this.actionSource = interactionData.actionSource
        this.actionSubtype = interactionData.actionSubtype
        this.elementId = interactionData.elementId
        this.elementFriendlyName = interactionData.elementFriendlyName
        this.funnelEntryToken = interactionData.funnelEntryToken
        this.funnelEventSequencePosition = interactionData.funnelEventSequencePosition
    }
}
