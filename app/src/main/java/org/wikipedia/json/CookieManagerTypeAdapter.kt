package org.wikipedia.json

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import okhttp3.Cookie
import okhttp3.HttpUrl
import org.wikipedia.dataclient.SharedPreferenceCookieManager
import org.wikipedia.dataclient.WikiSite
import java.io.IOException
import java.util.*

class CookieManagerTypeAdapter : TypeAdapter<SharedPreferenceCookieManager>() {
    @Throws(IOException::class)
    override fun write(out: JsonWriter, cookies: SharedPreferenceCookieManager) {
        val map = cookies.cookieJar
        out.beginObject()
        for (key in map.keys) {
            out.name(key).beginArray()
            for (cookie in map[key]!!) {
                out.value(cookie.toString())
            }
            out.endArray()
        }
        out.endObject()
    }

    @Throws(IOException::class)
    override fun read(`in`: JsonReader): SharedPreferenceCookieManager {
        val map: MutableMap<String, List<Cookie>> = HashMap()
        `in`.beginObject()
        while (`in`.hasNext()) {
            val key = `in`.nextName()
            val list: MutableList<Cookie> = ArrayList()
            map[key] = list
            `in`.beginArray()
            val url = HttpUrl.parse(WikiSite.DEFAULT_SCHEME + "://" + key)
            while (`in`.hasNext()) {
                val str = `in`.nextString()
                if (url != null) {
                    list.add(parse.parse(url, str))
                }
            }
            `in`.endArray()
        }
        `in`.endObject()
        return SharedPreferenceCookieManager(map)
    }
}