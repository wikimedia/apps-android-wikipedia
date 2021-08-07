package org.wikipedia.page

import android.location.Location
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import org.wikipedia.util.log.L

class GeoJsonAdapter : JsonAdapter<Location>() {
    override fun fromJson(reader: JsonReader): Location {
        val ret = Location("")
        reader.beginObject()
        while (reader.hasNext()) {
            when (val name = reader.nextName()) {
                GeoUnmarshaller.LATITUDE -> ret.latitude = reader.nextDouble()
                GeoUnmarshaller.LONGITUDE -> ret.longitude = reader.nextDouble()
                else -> L.d("name=$name")
            }
        }
        reader.endObject()
        return ret
    }

    override fun toJson(writer: JsonWriter, value: Location?) {
        writer.beginObject()
        writer.name(GeoUnmarshaller.LATITUDE).value(value?.latitude)
        writer.name(GeoUnmarshaller.LONGITUDE).value(value?.longitude)
        writer.endObject()
    }
}
