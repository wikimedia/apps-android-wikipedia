package org.wikipedia;

import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.bridge.CommunicationBridge;
import org.wikipedia.bridge.StyleLoader;

public class NightModeHandler {
    private final CommunicationBridge bridge;
    private final Site site;
    private final WikipediaApp app;

    /**
     * @param bridge The bridge used to communicate with the WebView
     * @param site The site for which to get night mode styles
     */
    public NightModeHandler(CommunicationBridge bridge, Site site) {
        this.bridge = bridge;
        this.site = site;
        this.app = WikipediaApp.getInstance();
    }

    /**
     * Returns the JSON used as payload for JS messages to turn night mode on/off
     *
     * @param hasPageLoaded Specify is the page has already been loaded or not.
     *                      If it has been loaded, inline style inversion is done by JS.
     *                      If not, it is handled by the transforms.
     * @return JSON used as payload for JS messages to turn night mode on or off
     */
    private JSONObject getPayload(boolean hasPageLoaded) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("hasPageLoaded", hasPageLoaded);
            payload.put("nightStyleBundle",
                    app.getStyleLoader().getAvailableBundle(StyleLoader.BUNDLE_NIGHT_MODE, site).toJSON());
        } catch (JSONException e) {
            // This shouldn't happen
            throw new RuntimeException(e);
        }
        return payload;
    }

    /**
     * Turn on Night Mode
     * @param hasPageLoaded Specify is the page has already been loaded or not.
     */
    public void turnOn(boolean hasPageLoaded) {
        bridge.sendMessage("toggleNightMode", getPayload(hasPageLoaded));
    }

    /**
     * Turn off Night Mode
     * @param hasPageLoaded Specify is the page has already been loaded or not.
     */
    public void turnOff(boolean hasPageLoaded) {
        bridge.sendMessage("toggleNightMode", getPayload(hasPageLoaded));
    }
}
