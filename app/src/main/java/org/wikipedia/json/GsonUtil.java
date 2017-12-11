package org.wikipedia.json;

import android.net.Uri;
import android.support.annotation.VisibleForTesting;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.page.Namespace;
import org.wikipedia.zero.ZeroConfig;

public final class GsonUtil {
    private static final String DATE_FORMAT = "MMM dd, yyyy HH:mm:ss";

    private static final GsonBuilder DEFAULT_GSON_BUILDER = new GsonBuilder()
            .setDateFormat(DATE_FORMAT)
            .registerTypeHierarchyAdapter(Uri.class, new UriTypeAdapter().nullSafe())
            .registerTypeHierarchyAdapter(Namespace.class, new NamespaceTypeAdapter().nullSafe())
            .registerTypeAdapter(WikiSite.class, new WikiSiteTypeAdapter().nullSafe())
            .registerTypeAdapter(ZeroConfig.class, new ZeroConfigTypeAdapter().nullSafe())
            .registerTypeAdapterFactory(new RequiredFieldsCheckOnReadTypeAdapterFactory())
            .registerTypeAdapterFactory(new PostProcessingTypeAdapter());

    private static final Gson DEFAULT_GSON = DEFAULT_GSON_BUILDER.create();

    public static Gson getDefaultGson() {
        return DEFAULT_GSON;
    }

    @VisibleForTesting
    public static GsonBuilder getDefaultGsonBuilder() {
        return DEFAULT_GSON_BUILDER;
    }

    private GsonUtil() { }
}
