package org.wikipedia.json

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import org.wikipedia.page.Namespace
import java.io.IOException
import kotlin.Throws

class NamespaceJsonAdapter : JsonAdapter<Namespace>() {
    @Throws(IOException::class)
    override fun fromJson(reader: JsonReader): Namespace {
        return if (reader.peek() == JsonReader.Token.STRING) {
            // Prior to 3210ce44, we marshaled Namespace as the name string of the enum, instead of
            // the code number. This introduces a backwards-compatible check for the string value.
            // TODO: remove after April 2017, when all older namespaces have been deserialized.
            Namespace.valueOf(reader.nextString())
        } else Namespace.of(reader.nextInt())
    }

    @Throws(IOException::class)
    override fun toJson(writer: JsonWriter, value: Namespace?) {
        writer.value(value?.code())
    }
}
