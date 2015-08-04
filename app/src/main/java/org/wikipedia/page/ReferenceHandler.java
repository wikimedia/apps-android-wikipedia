package org.wikipedia.page;

import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.bridge.CommunicationBridge;

/**
 * Handles any reference links coming from a {@link PageFragment}
 */
public abstract class ReferenceHandler implements CommunicationBridge.JSEventListener {

    public ReferenceHandler(CommunicationBridge bridge) {
        bridge.addListener("referenceClicked", this);
    }

    /**
     * Called when a reference link was clicked.
     */
    protected abstract void onReferenceClicked(String refHtml);

    // message from JS bridge:
    @Override
    public void onMessage(String messageType, JSONObject messagePayload) {
        try {
            onReferenceClicked(messagePayload.getString("ref"));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
