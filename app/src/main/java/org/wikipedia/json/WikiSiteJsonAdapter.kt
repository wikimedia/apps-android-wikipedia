package org.wikipedia.json

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import org.wikipedia.dataclient.WikiSite
import java.io.IOException

class WikiSiteJsonAdapter : JsonAdapter<WikiSite>() {
    companion object {
        private const val DOMAIN = "domain"
        private const val LANGUAGE_CODE = "languageCode"
        private val NAMES = JsonReader.Options.of(DOMAIN, LANGUAGE_CODE)
    }

    @Throws(IOException::class)
    override fun fromJson(reader: JsonReader): WikiSite {
        var domain: String? = null
        var languageCode: String? = null
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.selectName(NAMES)) {
                0 -> domain = reader.nextString()
                1 -> languageCode = reader.nextString()
                else -> {
                    reader.skipName()
                    reader.skipValue()
                }
            }
        }
        reader.endObject()
        if (domain.isNullOrEmpty()) {
            throw JsonDataException("Missing domain")
        }

        return languageCode?.let { WikiSite(domain, it) } ?: WikiSite(domain)
    }

    @Throws(IOException::class)
    override fun toJson(writer: JsonWriter, value: WikiSite?) {
        writer.beginObject()
        writer.name(DOMAIN)
        writer.value(value?.url())
        writer.name(LANGUAGE_CODE)
        writer.value(value?.languageCode)
        writer.endObject()
    }
}
