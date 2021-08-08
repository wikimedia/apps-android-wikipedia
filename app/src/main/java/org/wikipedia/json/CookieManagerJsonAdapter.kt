package org.wikipedia.json

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.wikipedia.dataclient.SharedPreferenceCookieManager
import org.wikipedia.dataclient.WikiSite
import java.io.IOException

class CookieManagerJsonAdapter : JsonAdapter<SharedPreferenceCookieManager>() {
    @Throws(IOException::class)
    override fun fromJson(reader: JsonReader): SharedPreferenceCookieManager {
        val map = mutableMapOf<String, List<Cookie>>()
        reader.beginObject()
        while (reader.hasNext()) {
            val key = reader.nextName()
            val list = mutableListOf<Cookie>()
            map[key] = list
            reader.beginArray()
            val url = (WikiSite.DEFAULT_SCHEME + "://" + key).toHttpUrlOrNull()
            while (reader.hasNext()) {
                url?.let {
                    Cookie.parse(it, reader.nextString())?.run { list.add(this) }
                }
            }
            reader.endArray()
        }
        reader.endObject()
        return SharedPreferenceCookieManager(map)
    }

    override fun toJson(writer: JsonWriter, value: SharedPreferenceCookieManager?) {
        val map = value?.cookieJar ?: emptyMap()
        writer.beginObject()
        for (key in map.keys) {
            writer.name(key).beginArray()
            map[key]?.forEach { cookie ->
                writer.value(cookie.toString())
            }
            writer.endArray()
        }
        writer.endObject()
    }
}
