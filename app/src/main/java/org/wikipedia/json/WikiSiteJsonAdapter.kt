package org.wikipedia.json

import com.google.gson.JsonParseException
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import org.wikipedia.dataclient.WikiSite
import java.io.IOException

class WikiSiteJsonAdapter : JsonAdapter<WikiSite>() {
    companion object {
        private const val DOMAIN = "domain"
        private const val LANGUAGE_CODE = "languageCode"
    }

    @Throws(IOException::class)
    override fun fromJson(reader: JsonReader): WikiSite {
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

    @Throws(IOException::class)
    override fun toJson(writer: JsonWriter, value: WikiSite?) {
        writer.beginObject()
        writer.name(DOMAIN)
        writer.value(value?.url())
        writer.name(LANGUAGE_CODE)
        writer.value(value?.languageCode())
        writer.endObject()
    }
}
