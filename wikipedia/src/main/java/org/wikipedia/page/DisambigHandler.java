package org.wikipedia.page;

import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.bridge.CommunicationBridge;

/**
 * Handles the #disambig links coming from a {@link org.wikipedia.page.PageViewFragment}.
 * Automatically goes to the disambiguation page for the selected item.
 */
public class DisambigHandler implements CommunicationBridge.JSEventListener {

    private LinkHandler linkHandler;

    public DisambigHandler(LinkHandler linkHandler, CommunicationBridge bridge) {
        this.linkHandler = linkHandler;
        bridge.addListener("disambigClicked", this);
    }

    // message from JS bridge:
    @Override
    public void onMessage(String messageType, JSONObject messagePayload) {
        try {
            linkHandler.onUrlClick(messagePayload.getString("title"));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

}
