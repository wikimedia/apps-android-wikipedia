package org.wikipedia.json;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public final class GsonUnmarshaller {
    /** @return Unmarshalled object. */
    public static <T> T unmarshal(Class<T> clazz, @Nullable String json) {
        return unmarshal(GsonUtil.getDefaultGson(), clazz, json);
    }

    /** @return Unmarshalled collection of objects. */
    public static <T> T unmarshal(TypeToken<T> typeToken, @Nullable String json) {
        return unmarshal(GsonUtil.getDefaultGson(), typeToken, json);
    }

    /** @return Unmarshalled object. */
    public static <T> T unmarshal(@NonNull Gson gson, Class<T> clazz, @Nullable String json) {
        return gson.fromJson(json, clazz);
    }

    /** @return Unmarshalled collection of objects. */
    public static <T> T unmarshal(@NonNull Gson gson, TypeToken<T> typeToken, @Nullable String json) {
        // From the manual: "Fairly hideous...  Unfortunately, no way to get around this in Java".
        return gson.fromJson(json, typeToken.getType());
    }

    private GsonUnmarshaller() { }
}
