package org.wikipedia.dataclient.wikidata

import android.location.Location
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import org.wikipedia.dataclient.mwapi.MwResponse
import org.wikipedia.json.GsonUtil

class Claims : MwResponse() {

    val claims: Map<String, List<Claim>> = emptyMap()

    class Claim {

        private val type: String? = null
        private val id: String? = null
        private val rank: String? = null

        @SerializedName("mainsnak")
        val mainSnak: MainSnak? = null
    }

    class MainSnak {

        @SerializedName("snaktype")
        private val snakType: String? = null

        @SerializedName("datatype")
        private val dataType: String? = null
        private val property: String? = null
        private val hash: String? = null

        @SerializedName("datavalue")
        val dataValue: DataValue? = null
    }

    class DataValue {

        private val value: JsonElement? = null
        private val type: String? = null

        fun value(): String {
            if (value != null) {
                if ("string" == type && value.isJsonPrimitive) {
                    return value.asString
                } else if ("wikibase-entityid" == type && value.isJsonObject) {
                    return GsonUtil.getDefaultGson().fromJson(value, EntityIdValue::class.java).id
                } else if ("time" == type && value.isJsonObject) {
                    return GsonUtil.getDefaultGson().fromJson(value, TimeValue::class.java).time
                } else if ("monolingualtext" == type && value.isJsonObject) {
                    return GsonUtil.getDefaultGson()
                        .fromJson(value, MonolingualTextValue::class.java).text
                } else if ("globecoordinate" == type && value.isJsonObject) {
                    return GsonUtil.getDefaultGson()
                        .fromJson(value, GlobeCoordinateValue::class.java).location.toString()
                }
            }
            return ""
        }
    }

    class EntityIdValue {

        val id: String = ""
    }

    class TimeValue {

        private val timezone = 0
        private val before = 0
        private val after = 0
        private val precision = 0
        val time: String = ""
    }

    class MonolingualTextValue {

        private val language: String? = null
        val text: String = ""
    }

    class GlobeCoordinateValue {

        private val latitude = 0.0
        private val longitude = 0.0
        private val altitude = 0.0
        private val precision = 0.0
        val location: Location
            get() {
                val loc = Location("")
                loc.latitude = latitude
                loc.longitude = longitude
                loc.altitude = altitude
                return loc
            }
    }
}
