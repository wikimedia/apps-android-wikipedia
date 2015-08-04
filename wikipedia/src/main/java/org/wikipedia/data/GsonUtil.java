package org.wikipedia.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public final class GsonUtil {
    private static final Gson DEFAULT_GSON = new GsonBuilder().create();

    public static Gson getDefaultGson() {
        return DEFAULT_GSON;
    }

    private GsonUtil() { }
}
