package org.wikipedia.settings

import org.json.JSONException
import org.json.JSONObject

/**
 * Store for config values that are retrieved from a server,
 * and refreshed periodically.
 */
class RemoteConfig {
    private var curConfig: JSONObject? = null

    fun updateConfig(newConfig: JSONObject) {
        Prefs.setRemoteConfigJson(newConfig.toString())
        curConfig = newConfig
    }

    // If there's no pref set, just give back the empty JSON Object
    val config: JSONObject
        get() {
            if (curConfig == null) {
                curConfig = try {
                    // If there's no pref set, just give back the empty JSON Object
                    JSONObject(Prefs.getRemoteConfigJson())
                } catch (e: JSONException) {
                    throw RuntimeException(e)
                }
            }
            return curConfig!!
        }
}
