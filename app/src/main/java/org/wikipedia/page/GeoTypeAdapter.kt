package org.wikipedia.page

import android.location.Location
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import org.wikipedia.util.log.L
import java.io.IOException

class GeoTypeAdapter : TypeAdapter<Location>() {
    @Throws(IOException::class)
    override fun write(out: JsonWriter, value: Location) {
        out.beginObject()
        out.name(GeoUnmarshaller.LATITUDE).value(value.latitude)
        out.name(GeoUnmarshaller.LONGITUDE).value(value.longitude)
        out.endObject()
    }

    @Throws(IOException::class)
    override fun read(input: JsonReader): Location {
        val ret = Location("")
        input.beginObject()
        while (input.hasNext()) {
            when (val name = input.nextName()) {
                GeoUnmarshaller.LATITUDE -> ret.latitude = input.nextDouble()
                GeoUnmarshaller.LONGITUDE -> ret.longitude = input.nextDouble()
                else -> L.d("name=$name")
            }
        }
        input.endObject()
        return ret
    }
}
