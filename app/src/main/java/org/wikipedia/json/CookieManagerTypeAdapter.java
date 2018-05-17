package org.wikipedia.json;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import org.wikipedia.dataclient.SharedPreferenceCookieManager;
import org.wikipedia.dataclient.WikiSite;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Cookie;
import okhttp3.HttpUrl;

public class CookieManagerTypeAdapter extends TypeAdapter<SharedPreferenceCookieManager> {
    @Override public void write(JsonWriter out, SharedPreferenceCookieManager cookies) throws IOException {
        Map<String, List<Cookie>> map = cookies.getCookieJar();
        out.beginObject();
        for (String key : map.keySet()) {
            out.name(key).beginArray();
            for (Cookie cookie : map.get(key)) {
                out.value(cookie.toString());
            }
            out.endArray();
        }
        out.endObject();
    }

    @Override public SharedPreferenceCookieManager read(JsonReader in) throws IOException {
        Map<String, List<Cookie>> map = new HashMap<>();
        in.beginObject();
        while (in.hasNext()) {
            String key = in.nextName();
            List<Cookie> list = new ArrayList<>();
            map.put(key, list);
            in.beginArray();
            HttpUrl url = HttpUrl.parse(WikiSite.DEFAULT_SCHEME + "://" + key);
            while (in.hasNext()) {
                String str = in.nextString();
                if (url != null) {
                    list.add(Cookie.parse(url, str));
                }
            }
            in.endArray();
        }
        in.endObject();
        return new SharedPreferenceCookieManager(map);
    }
}
