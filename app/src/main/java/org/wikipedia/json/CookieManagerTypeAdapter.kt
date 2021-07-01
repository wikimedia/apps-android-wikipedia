package org.wikipedia.json

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.wikipedia.dataclient.SharedPreferenceCookieManager
import org.wikipedia.dataclient.WikiSite
import java.io.IOException

class CookieManagerTypeAdapter : TypeAdapter<SharedPreferenceCookieManager>() {

    @Throws(IOException::class)
    override fun write(writer: JsonWriter, cookies: SharedPreferenceCookieManager) {
        val map = cookies.cookieJar
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

    @Throws(IOException::class)
    override fun read(reader: JsonReader): SharedPreferenceCookieManager {
        val map = mutableMapOf<String, MutableList<Cookie>>()
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
}
