package org.wikipedia.dataclient.wikidata

import android.location.Location
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.wikipedia.dataclient.mwapi.MwResponse
import org.wikipedia.dataclient.mwapi.MwServiceError

@JsonClass(generateAdapter = true)
class Claims(
    errors: List<MwServiceError> = emptyList(),
    @Json(name = "servedby") servedBy: String = "",
    val claims: Map<String, List<Claim>> = emptyMap()
) : MwResponse(errors, servedBy) {
    @JsonClass(generateAdapter = true)
    class Claim(
        internal val type: String? = null, internal val id: String? = null,
        internal val rank: String? = null, @Json(name = "mainsnak") val mainSnak: MainSnak? = null
    )

    @JsonClass(generateAdapter = true)
    class MainSnak(
        @Json(name = "snaktype") internal val snakType: String? = null,
        @Json(name = "datatype") internal val dataType: String? = null,
        internal val property: String? = null,
        internal val hash: String? = null,
        @Json(name = "datavalue") val dataValue: DataValue? = null
    ) {
        val dataValueAsString: String
            get() = when (dataValue) {
                is StringValue -> dataValue.value
                is EntityIdValue -> dataValue.value.id
                is TimeValue -> dataValue.value.time
                is MonolingualTextValue -> dataValue.value.text
                is GlobeCoordinateValue -> dataValue.value.location.toString()
                else -> ""
            }
    }

    sealed class DataValue(type: Type) {
        enum class Type(val value: String) {
            STRING("string"),
            WIKIBASE_ENTITY_ID("wikibase-entityid"),
            TIME("time"),
            MONOLINGUAL_TEXT("monolingualtext"),
            GLOBE_COORDINATE("globecoordinate")
        }
    }

    @JsonClass(generateAdapter = true)
    class StringValue(val value: String = "") : DataValue(Type.STRING)

    @JsonClass(generateAdapter = true)
    class EntityIdValue(val value: EntityId) : DataValue(Type.WIKIBASE_ENTITY_ID)

    @JsonClass(generateAdapter = true)
    class EntityId(val id: String = "")

    @JsonClass(generateAdapter = true)
    class TimeValue(val value: Time) : DataValue(Type.TIME)

    @JsonClass(generateAdapter = true)
    class Time(
        internal val timezone: Int = 0, internal val before: Int = 0, internal val after: Int = 0,
        internal val precision: Int = 0, val time: String = ""
    )

    @JsonClass(generateAdapter = true)
    class MonolingualTextValue(val value: MonolingualText) : DataValue(Type.MONOLINGUAL_TEXT)

    @JsonClass(generateAdapter = true)
    class MonolingualText(internal val language: String? = null, val text: String = "")

    @JsonClass(generateAdapter = true)
    class GlobeCoordinateValue(val value: GlobeCoordinate) : DataValue(Type.GLOBE_COORDINATE)

    @JsonClass(generateAdapter = true)
    class GlobeCoordinate(
        internal val latitude: Double = 0.0, internal val longitude: Double = 0.0,
        internal val altitude: Double = 0.0, internal val precision: Double = 0.0
    ) {
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
