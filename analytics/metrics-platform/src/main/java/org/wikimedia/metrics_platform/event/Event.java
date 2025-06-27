package org.wikimedia.metrics_platform.event;

import java.util.Map;

import org.wikimedia.metrics_platform.config.sampling.SampleConfig;
import org.wikimedia.metrics_platform.context.ClientData;
import org.wikimedia.metrics_platform.context.InteractionData;
import org.wikimedia.metrics_platform.utils.Objects;

import com.google.gson.annotations.SerializedName;

public class Event {
    @SerializedName("$schema") protected String schema;
    @SerializedName("name") protected final String name;
    @SerializedName("dt") protected String timestamp;
    @SerializedName("custom_data") protected Map<String, Object> customData;
    protected final Meta meta;
    @SerializedName("client_data") protected ClientData clientData;
    @SerializedName("sample") protected SampleConfig sample;
    @SerializedName("interaction_data") protected InteractionData interactionData;

    public Event(String schema, String stream, String name) {
        this.schema = schema;
        this.meta = new Meta(stream);
        this.name = name;
    }

    @Nullable
    public String getStream() {
        return meta.getStream();
    }

    public void setDomain(String domain) {
        meta.domain = domain;
    }

    @Nonnull
    public ClientData getClientData() {
        clientData = Objects.firstNonNull(clientData, ClientData::new);
        return clientData;
    }

    public void setCustomData(@Nonnull Map<String, Object> customData) {
        this.customData = customData;
    }

    public void setSample(@Nonnull SampleConfig sample) {
        this.sample = sample;
    }

    @Nonnull
    public InteractionData getInteractionData() {
        interactionData = Objects.firstNonNull(interactionData, InteractionData::new);
        return interactionData;
    }

    public void setInteractionData(@Nonnull InteractionData interactionData) {
        this.interactionData = interactionData;
    }

    protected static final class Meta {
        private final String stream;
        private String domain;
    }
}
