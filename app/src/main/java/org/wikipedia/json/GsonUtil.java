package org.wikipedia.json;

import android.net.Uri;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.wikipedia.page.Namespace;

public final class GsonUtil {
    private static final String DATE_FORMAT = "MMM dd, yyyy HH:mm:ss";
    private static final Gson DEFAULT_GSON = new GsonBuilder()
            .setDateFormat(DATE_FORMAT)
            .registerTypeHierarchyAdapter(Uri.class, new UriTypeAdapter().nullSafe())
            .registerTypeHierarchyAdapter(Namespace.class, new NamespaceTypeAdapter().nullSafe())
            .registerTypeAdapterFactory(new RequiredFieldsCheckOnReadTypeAdapterFactory())
            .create();

    public static Gson getDefaultGson() {
        return DEFAULT_GSON;
    }

    private GsonUtil() { }
}