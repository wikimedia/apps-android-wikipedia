package org.wikipedia.json;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;

public final class GsonMarshaller {
    public static String marshal(@Nullable Object object) {
        return marshal(GsonUtil.getDefaultGson(), object);
    }

    public static String marshal(@NonNull Gson gson, @Nullable Object object) {
        return gson.toJson(object);
    }

    private GsonMarshaller() { }
}
