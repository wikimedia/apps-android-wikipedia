package org.wikimedia.metrics_platform.event

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikimedia.metrics_platform.config.sampling.SampleConfig
import org.wikimedia.metrics_platform.context.AgentData
import org.wikimedia.metrics_platform.context.ClientData
import org.wikimedia.metrics_platform.context.InteractionData
import org.wikimedia.metrics_platform.context.MediawikiData
import org.wikimedia.metrics_platform.context.PageData
import org.wikimedia.metrics_platform.context.PerformerData

@Serializable
class EventProcessed : Event {
    @SerialName("agent") var agentData: AgentData = AgentData()
    @SerialName("page") var pageData: PageData = PageData()
    @SerialName("mediawiki") var mediawikiData: MediawikiData = MediawikiData()
    @SerialName("performer") var performerData: PerformerData = PerformerData()
    @SerialName("action") var action: String
    @SerialName("action_subtype") private var actionSubtype: String? = null
    @SerialName("action_source") private var actionSource: String? = null
    @SerialName("action_context") private var actionContext: String? = null
    @SerialName("element_id") private var elementId: String? = null
    @SerialName("element_friendly_name") private var elementFriendlyName: String? = null
    @SerialName("funnel_entry_token") private var funnelEntryToken: String? = null
    @SerialName("funnel_event_sequence_position") private var funnelEventSequencePosition: Int? = null

    /**
     * Constructor for EventProcessed.
     *
     * @param schema schema id
     * @param stream stream name
     * @param name event name
     * @param clientData agent, mediawiki, page, performer data
     */
    constructor(
        schema: String?,
        stream: String?,
        @Nonnull name: String?,
        clientData: ClientData
    ) : super(schema!!, stream, name) {
        this.clientData = clientData
        this.agentData = clientData.agentData
        this.pageData = clientData.pageData
        this.mediawikiData = clientData.mediawikiData
        this.performerData = clientData.performerData
        this.action = name
    }

    /**
     * Constructor for EventProcessed.
     *
     * @param schema schema id
     * @param stream stream name
     * @param name event name
     * @param customData custom data
     * @param clientData agent, mediawiki, page, performer data
     * @param sample sample configuration
     * @param interactionData contextual data of the interaction
     *
     *
     * Although 'setInteractionData()' sets the 'action' property for the event,
     * because 'action' is a nonnull property for both 'EventProcessed' and the
     * 'InteractionData' data object, removing the redundant setting of 'action'
     * triggers NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR in spotbugs so
     * leaving it as is for the time being rather than suppressing the error.
     */
    constructor(
        schema: String?,
        stream: String?,
        name: String?,
        customData: MutableMap<String, String>?,
        clientData: ClientData,
        sample: SampleConfig?,
        interactionData: InteractionData
    ) : super(schema!!, stream, name) {
        this.clientData = clientData
        this.agentData = clientData.agentData
        this.pageData = clientData.pageData
        this.mediawikiData = clientData.mediawikiData
        this.performerData = clientData.performerData
        this.customData = customData
        this.sample = sample
        this.interactionData = interactionData
        this.action = interactionData.action
    }

    public override fun setClientData(@Nonnull clientData: ClientData) {
        setAgentData(clientData.agentData)
        setPageData(clientData.pageData)
        setMediawikiData(clientData.mediawikiData)
        setPerformerData(clientData.performerData)
        this.clientData = clientData
    }

    public override fun setInteractionData(@Nonnull interactionData: InteractionData) {
        this.action = interactionData.action
        this.actionContext = interactionData.actionContext
        this.actionSource = interactionData.actionSource
        this.actionSubtype = interactionData.actionSubtype
        this.elementId = interactionData.elementId
        this.elementFriendlyName = interactionData.elementFriendlyName
        this.funnelEntryToken = interactionData.funnelEntryToken
        this.funnelEventSequencePosition = interactionData.funnelEventSequencePosition
    }

    companion object {
        fun fromEvent(event: Event): EventProcessed {
            return EventProcessed(
                event.schema,
                event.stream,
                event.name,
                event.customData,
                event.clientData,
                event.sample,
                event.interactionData
            )
        }
    }
}
