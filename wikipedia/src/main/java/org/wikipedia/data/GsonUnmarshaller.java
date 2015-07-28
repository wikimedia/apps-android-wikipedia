package org.wikipedia.data;

import android.support.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.Collection;

public final class GsonUnmarshaller {
    private static final Gson DEFAULT_GSON = new Gson();

    /** @return Unmarshalled object. */
    public static <T> T unmarshal(Class<T> clazz, @NonNull String json) {
        return unmarshal(DEFAULT_GSON, clazz, json);
    }

    /** @return Unmarshalled collection of objects. */
    public static <T extends Collection<?>> T unmarshal(TypeToken<T> typeToken, @NonNull String json) {
        return unmarshal(DEFAULT_GSON, typeToken, json);
    }

    /** @return Unmarshalled object. */
    public static <T> T unmarshal(@NonNull Gson gson, Class<T> clazz, @NonNull String json) {
        return gson.fromJson(json, clazz);
    }

    /** @return Unmarshalled collection of objects. */
    public static <T extends Collection<?>> T unmarshal(@NonNull Gson gson, TypeToken<T> typeToken, @NonNull String json) {
        // From the manual: "Fairly hideous...  Unfortunately, no way to get around this in Java".
        return gson.fromJson(json, typeToken.getType());
    }

    private GsonUnmarshaller() { }
}