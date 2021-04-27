package org.wikipedia.page

import android.location.Location
import org.json.JSONException
import org.json.JSONObject

object GeoUnmarshaller {
    const val LATITUDE = "lat"
    const val LONGITUDE = "lon"

    @JvmStatic
    fun unmarshal(json: String?): Location? {
        return json?.let {
            val jsonObj = try {
                JSONObject(it)
            } catch (e: JSONException) {
                return null
            }
            unmarshal(jsonObj)
        }
    }

    fun unmarshal(jsonObj: JSONObject): Location {
        return Location("").apply {
            latitude = jsonObj.optDouble(LATITUDE)
            longitude = jsonObj.optDouble(LONGITUDE)
        }
    }
}
