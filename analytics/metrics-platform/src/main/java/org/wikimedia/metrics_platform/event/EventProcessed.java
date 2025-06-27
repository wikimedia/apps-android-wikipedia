package org.wikimedia.metrics_platform.event;

import static org.wikimedia.metrics_platform.utils.Objects.firstNonNull;

import java.util.Map;

import org.wikimedia.metrics_platform.config.sampling.SampleConfig;
import org.wikimedia.metrics_platform.context.AgentData;
import org.wikimedia.metrics_platform.context.ClientData;
import org.wikimedia.metrics_platform.context.InteractionData;
import org.wikimedia.metrics_platform.context.MediawikiData;
import org.wikimedia.metrics_platform.context.PageData;
import org.wikimedia.metrics_platform.context.PerformerData;

import com.google.gson.annotations.SerializedName;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@EqualsAndHashCode
@Getter
@Setter
public class EventProcessed extends Event {
    @SerializedName("agent") private AgentData agentData;
    @SerializedName("page") private PageData pageData;
    @SerializedName("mediawiki") private MediawikiData mediawikiData;
    @SerializedName("performer") private PerformerData performerData;
    @Nonnull
    @SerializedName("action") private String action;
    @SerializedName("action_subtype") private String actionSubtype;
    @SerializedName("action_source") private String actionSource;
    @SerializedName("action_context") private String actionContext;
    @SerializedName("element_id") private String elementId;
    @SerializedName("element_friendly_name") private String elementFriendlyName;
    @SerializedName("funnel_entry_token") private String funnelEntryToken;
    @SerializedName("funnel_event_sequence_position") private Integer funnelEventSequencePosition;

    /**
     * Constructor for EventProcessed.
     *
     * @param schema schema id
     * @param stream stream name
     * @param name event name
     * @param clientData agent, mediawiki, page, performer data
     */
    public EventProcessed(
            String schema,
            String stream,
            @Nonnull String name,
            ClientData clientData
    ) {
        super(schema, stream, name);
        this.clientData = clientData;
        this.agentData = clientData.getAgentData();
        this.pageData = clientData.getPageData();
        this.mediawikiData = clientData.getMediawikiData();
        this.performerData = clientData.getPerformerData();
        this.action = name;
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
     * <p>
     * Although 'setInteractionData()' sets the 'action' property for the event,
     * because 'action' is a nonnull property for both 'EventProcessed' and the
     * 'InteractionData' data object, removing the redundant setting of 'action'
     * triggers NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR in spotbugs so
     * leaving it as is for the time being rather than suppressing the error.
     */
    public EventProcessed(
            String schema,
            String stream,
            String name,
            Map<String, Object> customData,
            ClientData clientData,
            SampleConfig sample,
            InteractionData interactionData
    ) {
        super(schema, stream, name);
        this.clientData = clientData;
        this.agentData = clientData.getAgentData();
        this.pageData = clientData.getPageData();
        this.mediawikiData = clientData.getMediawikiData();
        this.performerData = clientData.getPerformerData();
        this.setCustomData(customData);
        this.sample = sample;
        this.setInteractionData(interactionData);
        this.action = interactionData.getAction();
    }

    @Nonnull
    public static EventProcessed fromEvent(Event event) {
        return new EventProcessed(
                event.getSchema(),
                event.getStream(),
                event.getName(),
                event.getCustomData(),
                event.getClientData(),
                event.getSample(),
                event.getInteractionData()
        );
    }

    @Nonnull
    public AgentData getAgentData() {
        agentData = firstNonNull(agentData, AgentData.NULL_AGENT_DATA);
        return agentData;
    }

    @Nonnull
    public PageData getPageData() {
        pageData = firstNonNull(pageData, PageData.NULL_PAGE_DATA);
        return pageData;
    }

    @Nonnull
    public MediawikiData getMediawikiData() {
        mediawikiData = firstNonNull(mediawikiData, MediawikiData.NULL_MEDIAWIKI_DATA);
        return mediawikiData;
    }

    @Nonnull
    public PerformerData getPerformerData() {
        performerData = firstNonNull(performerData, PerformerData.NULL_PERFORMER_DATA);
        return performerData;
    }

    @Override
    public void setClientData(@Nonnull ClientData clientData) {
        setAgentData(clientData.getAgentData());
        setPageData(clientData.getPageData());
        setMediawikiData(clientData.getMediawikiData());
        setPerformerData(clientData.getPerformerData());
        this.clientData = clientData;
    }

    @Override
    public final void setInteractionData(@Nonnull InteractionData interactionData) {
        this.action = interactionData.getAction();
        this.actionContext = interactionData.getActionContext();
        this.actionSource = interactionData.getActionSource();
        this.actionSubtype = interactionData.getActionSubtype();
        this.elementId = interactionData.getElementId();
        this.elementFriendlyName = interactionData.getElementFriendlyName();
        this.funnelEntryToken = interactionData.getFunnelEntryToken();
        this.funnelEventSequencePosition = interactionData.getFunnelEventSequencePosition();
    }

    public void setAgentData(AgentData agentData) {
        this.agentData = agentData;
    }
    public void setPageData(PageData pageData) {
        this.pageData = pageData;
    }
    public void setMediawikiData(MediawikiData mediawikiData) {
        this.mediawikiData = mediawikiData;
    }
    public void setPerformerData(PerformerData performerData) {
        this.performerData = performerData;
    }
}
