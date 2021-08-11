package org.wikipedia.json

import android.net.Uri
import androidx.core.net.toUri
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import java.io.IOException
import kotlin.Throws

class UriJsonAdapter : JsonAdapter<Uri>() {
    @Throws(IOException::class)
    override fun fromJson(reader: JsonReader): Uri {
        return reader.nextString().toUri()
    }

    @Throws(IOException::class)
    override fun toJson(writer: JsonWriter, value: Uri?) {
        writer.value(value?.toString())
    }
}
