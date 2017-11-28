package org.wikipedia.theme;

import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.WikipediaApp;
import org.wikipedia.bridge.CommunicationBridge;
import org.wikipedia.settings.Prefs;

public final class ThemeBridgeAdapter {
    private static JSONObject getPayload() {
        JSONObject payload = new JSONObject();
        try {
            payload.put("theme", WikipediaApp.getInstance().getCurrentTheme().getMarshallingId());
            payload.put("dimImages", Prefs.shouldDimDarkModeImages());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return payload;
    }

    public static void setTheme(@NonNull CommunicationBridge bridge) {
        bridge.sendMessage("setTheme", getPayload());
    }

    public static void toggleDimImages(@NonNull CommunicationBridge bridge) {
        bridge.sendMessage("toggleDimImages", getPayload());
    }

    private ThemeBridgeAdapter() {
    }
}
