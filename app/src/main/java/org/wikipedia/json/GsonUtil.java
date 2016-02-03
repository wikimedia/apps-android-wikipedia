package org.wikipedia.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public final class GsonUtil {
    private static final String DATE_FORMAT = "MMM dd, yyyy HH:mm:ss";
    private static final Gson DEFAULT_GSON = new GsonBuilder().setDateFormat(DATE_FORMAT).create();

    public static Gson getDefaultGson() {
        return DEFAULT_GSON;
    }

    private GsonUtil() { }
}
