package org.wikipedia.json;

import androidx.annotation.VisibleForTesting;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public final class GsonUtil {
    private static final String DATE_FORMAT = "MMM dd, yyyy HH:mm:ss";

    private static final GsonBuilder DEFAULT_GSON_BUILDER = new GsonBuilder()
            .setDateFormat(DATE_FORMAT)
            .registerTypeAdapterFactory(new RequiredFieldsCheckOnReadTypeAdapterFactory());

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
