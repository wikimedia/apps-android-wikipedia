package org.wikipedia.json;

import android.net.Uri;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.wikipedia.page.Namespace;

public final class GsonUtil {
    private static final String DATE_FORMAT = "MMM dd, yyyy HH:mm:ss";
    private static final Gson DEFAULT_GSON = new GsonBuilder()
            .setDateFormat(DATE_FORMAT)
            .registerTypeAdapter(Uri.class, new UriTypeAdapter())
            .registerTypeHierarchyAdapter(Namespace.class, new NamespaceTypeAdapter().nullSafe())
            .create();

    public static Gson getDefaultGson() {
        return DEFAULT_GSON;
    }

    private GsonUtil() { }
}