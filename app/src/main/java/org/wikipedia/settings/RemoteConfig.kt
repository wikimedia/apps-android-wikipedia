package org.wikipedia.settings

import org.json.JSONException
import org.json.JSONObject

class RemoteConfig {
    private var curConfig: JSONObject? = null

    // If there's no pref set, just give back the empty JSON Object
    val config: JSONObject
        get() {
            if (curConfig == null) {
                curConfig = try {
                    // If there's no pref set, just give back the empty JSON Object
                    JSONObject(Prefs.remoteConfigJson)
                } catch (e: JSONException) {
                    throw RuntimeException(e)
                }
            }
            return curConfig!!
        }

    fun updateConfig(newConfig: JSONObject) {
        Prefs.remoteConfigJson = newConfig.toString()
        curConfig = newConfig
    }
}
