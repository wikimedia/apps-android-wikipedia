package org.wikipedia.json

import com.google.gson.JsonParseException
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import org.wikipedia.dataclient.WikiSite
import java.io.IOException

class WikiSiteTypeAdapter : TypeAdapter<WikiSite>() {

    @Throws(IOException::class)
    override fun write(writer: JsonWriter, value: WikiSite) {
        writer.beginObject()
        writer.name(DOMAIN)
        writer.value(value.url())
        writer.name(LANGUAGE_CODE)
        writer.value(value.languageCode())
        writer.endObject()
    }

    @Throws(IOException::class)
    override fun read(reader: JsonReader): WikiSite {
        var domain: String? = null
        var languageCode: String? = null
        reader.beginObject()
        while (reader.hasNext()) {
            val field = reader.nextName()
            val value = reader.nextString()
            when (field) {
                DOMAIN -> domain = value
                LANGUAGE_CODE -> languageCode = value
            }
        }
        reader.endObject()
        if (domain.isNullOrEmpty()) {
            throw JsonParseException("Missing domain")
        }

        return languageCode?.let { WikiSite(domain, it) } ?: WikiSite(domain)
    }

    companion object {
        private const val DOMAIN = "domain"
        private const val LANGUAGE_CODE = "languageCode"
    }
}
