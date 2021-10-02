package org.wikipedia.dataclient.wikidata

import android.location.Location
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.wikipedia.dataclient.mwapi.MwResponse
import org.wikipedia.json.JsonUtil

@Serializable
class Claims : MwResponse() {

    val claims: Map<String, List<Claim>> = emptyMap()

    @Serializable
    class Claim {

        private val type: String? = null
        private val id: String? = null
        private val rank: String? = null

        @SerialName("mainsnak")
        val mainSnak: MainSnak? = null
    }

    @Serializable
    class MainSnak {

        @SerialName("snaktype")
        private val snakType: String? = null

        @SerialName("datatype")
        private val dataType: String? = null
        private val property: String? = null
        private val hash: String? = null

        @SerialName("datavalue")
        val dataValue: DataValue? = null
    }

    @Serializable
    class DataValue {

        private val value: JsonElement? = null
        private val type: String? = null

        fun value(): String {
            if (value != null) {
                if ("string" == type) {
                    return value.toString()
                } else if ("wikibase-entityid" == type) {
                    return JsonUtil.json.decodeFromJsonElement<EntityIdValue>(value).id
                } else if ("time" == type) {
                    return JsonUtil.json.decodeFromJsonElement<TimeValue>(value).time
                } else if ("monolingualtext" == type) {
                    return JsonUtil.json.decodeFromJsonElement<MonolingualTextValue>(value).text
                } else if ("globecoordinate" == type) {
                    return JsonUtil.json.decodeFromJsonElement<GlobeCoordinateValue>(value).location.toString()
                }
            }
            return ""
        }
    }

    @Serializable
    class EntityIdValue {

        val id: String = ""
    }

    @Serializable
    class TimeValue {

        private val timezone = 0
        private val before = 0
        private val after = 0
        private val precision = 0
        val time: String = ""
    }

    @Serializable
    class MonolingualTextValue {

        private val language: String? = null
        val text: String = ""
    }

    @Serializable
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
