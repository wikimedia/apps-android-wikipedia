package org.wikipedia.data;

import com.google.gson.Gson;

public final class GsonMarshaller {
    private static final Gson DEFAULT_GSON = new Gson();

    public static String marshal(Object object) {
        return DEFAULT_GSON.toJson(object);
    }

    private GsonMarshaller() { }
}