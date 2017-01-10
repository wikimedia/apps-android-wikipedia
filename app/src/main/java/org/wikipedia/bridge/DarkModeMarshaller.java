package org.wikipedia.bridge;

import org.json.JSONException;
import org.json.JSONObject;

// todo: move turnOn() somewhere else and rmeove bridge dependency
public class DarkModeMarshaller {
    private final CommunicationBridge bridge;

    /**
     * @param bridge The bridge used to communicate with the WebView
     */
    public DarkModeMarshaller(CommunicationBridge bridge) {
        this.bridge = bridge;
    }

    /**
     * Returns the JSON used as payload for JS messages to turn dark mode on/off
     *
     * @param hasPageLoaded Specify is the page has already been loaded or not.
     *                      If it has been loaded, inline style inversion is done by JS.
     *                      If not, it is handled by the transforms.
     * @return JSON used as payload for JS messages to turn dark mode on or off
     */
    private JSONObject getPayload(boolean hasPageLoaded) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("hasPageLoaded", hasPageLoaded);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return payload;
    }

    /**
     * Turn on dark mode
     * @param hasPageLoaded Specify is the page has already been loaded or not.
     */
    public void turnOn(boolean hasPageLoaded) {
        bridge.sendMessage("toggleDarkMode", getPayload(hasPageLoaded));
    }
}
