package org.wikipedia.data;

import com.google.gson.Gson;

public final class GsonMarshaller {
    public static String marshal(Object object) {
        return marshal(GsonUtil.getDefaultGson(), object);
    }

    public static String marshal(Gson gson, Object object) {
        return gson.toJson(object);
    }

    private GsonMarshaller() { }
}