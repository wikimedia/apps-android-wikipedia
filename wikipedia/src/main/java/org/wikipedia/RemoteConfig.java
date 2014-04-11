package org.wikipedia;

import android.content.*;
import org.json.*;

/**
 * Store for config values that are retreived from a server,
 * and refreshed periodically.
 */
public class RemoteConfig {

    private final SharedPreferences prefs;

    private JSONObject curConfig;

    public RemoteConfig(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    public void updateConfig(JSONObject newConfig) {
        prefs.edit()
                .putString(WikipediaApp.PREFERENCE_REMOTE_CONFIG, newConfig.toString())
                .commit();
        curConfig = newConfig;
    }

    public JSONObject getConfig() {
        if (curConfig == null) {
            try {
                // If there's no pref set, just give back the empty JSON Object
                curConfig = new JSONObject(prefs.getString(WikipediaApp.PREFERENCE_REMOTE_CONFIG, "{}"));
            } catch (JSONException e) {
                // This shouldn't be happening, and if it does I'd like a crash report
                throw new RuntimeException(e);
            }
        }

        return curConfig;
    }
}
