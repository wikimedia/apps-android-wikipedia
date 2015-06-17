package org.wikipedia;

import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.settings.Prefs;

/**
 * Store for config values that are retreived from a server,
 * and refreshed periodically.
 */
public class RemoteConfig {
    private JSONObject curConfig;

    public void updateConfig(JSONObject newConfig) {
        Prefs.setRemoteConfigJson(newConfig.toString());
        curConfig = newConfig;
    }

    public JSONObject getConfig() {
        if (curConfig == null) {
            try {
                // If there's no pref set, just give back the empty JSON Object
                curConfig = new JSONObject(Prefs.getRemoteConfigJson());
            } catch (JSONException e) {
                // This shouldn't be happening, and if it does I'd like a crash report
                throw new RuntimeException(e);
            }
        }

        return curConfig;
    }
}
