package org.wikipedia.page

import android.location.Location
import org.json.JSONException
import org.json.JSONObject

object GeoMarshaller {
    @JvmStatic
    fun marshal(obj: Location?): String? {
        return obj?.let {
            JSONObject().runCatching {
                put(GeoUnmarshaller.LATITUDE, it.latitude)
                put(GeoUnmarshaller.LONGITUDE, it.longitude)
            }.onFailure {
                throw RuntimeException(it as JSONException)
            }.toString()
        }
    }
}
