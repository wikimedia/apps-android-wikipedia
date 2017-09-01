package org.wikipedia.theme;

import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.bridge.CommunicationBridge;
import org.wikipedia.settings.Prefs;

// todo: move turnOn() somewhere else and rmeove bridge dependency
public class DarkModeSwitch {
    private final CommunicationBridge bridge;

    /**
     * @param bridge The bridge used to communicate with the WebView
     */
    public DarkModeSwitch(CommunicationBridge bridge) {
        this.bridge = bridge;
    }

    /**
     * Returns the JSON used as payload for JS messages to turn dark mode on/off
     * @return JSON used as payload for JS messages to turn dark mode on or off
     */
    private JSONObject getPayload() {
        JSONObject payload = new JSONObject();
        try {
            payload.put("dimImages", Prefs.shouldDimDarkModeImages());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return payload;
    }

    /**
     * Update dark mode and image dimming styles
     */
    public void turnOn() {
        bridge.sendMessage("toggleDarkMode", getPayload());
    }

    /**
     * Update image dimming style only
     */
    public void toggleDimImages() {
        bridge.sendMessage("toggleDimImages", getPayload());
    }
}
