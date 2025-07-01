package org.wikimedia.metricsplatform.event;

public class EventProcessedSerializer {
    /*
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Instant.class, new InstantConverter())
            .create();

    @Override
    public JsonElement serialize(EventProcessed src, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonElement jsonElement = gson.toJsonTree(src);
        JsonObject jsonObject = (JsonObject) jsonElement;

         * Custom data can be passed into the EventProcessed constructor as a map
         * of key-value pairs. EventProcessed inherits from Event which contains a
         * customData property that would be serialized by default. To validate
         * successfully against the corresponding schema, custom data must be sent
         * as top-level properties with the event.

        if (src.customData != null) {
            int customDataAdded = 0;
            int customDataCount = src.customData.size();
            for (Map.Entry<String, Object> entry : src.customData.entrySet()) {
                if (entry.getValue() instanceof Number)
                    jsonObject.addProperty(entry.getKey(), (Number) entry.getValue());
                if (entry.getValue() instanceof String)
                    jsonObject.addProperty(entry.getKey(), entry.getValue().toString());
                if (entry.getValue() instanceof Boolean)
                    jsonObject.addProperty(entry.getKey(), (Boolean) entry.getValue());

                if (jsonObject.has(entry.getKey()))
                    customDataAdded++;
            }

            if (customDataAdded != customDataCount) {
                log.log(INFO, "Only " + customDataAdded + " custom data key-value pairs were serialized " +
                        "but there are " + customDataCount + " total custom data items for this event.");
            }

            jsonObject.remove("custom_data");
            jsonObject.remove("client_data");
        }

         * Removing a few properties here because it would require adding annotations on every Event and EventProcessed
         * property just to exclude few properties from serialization.
         *
         * The "name" property is an Event property that eventually gets submitted as "action" with an event, but it is
         * not included in the Metrics Platform base interactions schemas. Because the InteractionData data object can
         * be a null parameter in the MetricsClient::submitMetricsEvent method, the "name" property is needed when
         * clients send events without InteractionData (see @ToDo add link to MP schemas once they are merged).
         *
         * Once Metrics Platform core interactions schemas are updated to include the "sample" property, the line to
         * remove it can be deleted here.
        jsonObject.remove("name");
        jsonObject.remove("sample");

         * Remove the top level data objects from EventProcessed which are
         * inherited from its superclass Event. The values in "client_data"
         * and "interaction_data" are set as top level properties in
         * EventProcessed's constructor.
        jsonObject.remove("client_data");
        jsonObject.remove("interaction_data");
        return jsonObject;
    }
    */
}
