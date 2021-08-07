package org.wikipedia.json;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.reflect.TypeToken;
import com.squareup.moshi.Moshi;

import java.io.IOException;

public class MoshiUnmarshaller {
    /** @return Unmarshalled object. */
    public static <T> T unmarshal(Class<T> clazz, @Nullable String json) throws IOException {
        return unmarshal(MoshiUtil.getDefaultMoshi(), clazz, json);
    }

    /** @return Unmarshalled collection of objects. */
    public static <T> T unmarshal(TypeToken<T> typeToken, @Nullable String json) throws IOException {
        return unmarshal(MoshiUtil.getDefaultMoshi(), typeToken, json);
    }

    /** @return Unmarshalled object. */
    public static <T> T unmarshal(@NonNull Moshi moshi, Class<T> clazz, @Nullable String json) throws IOException {
        return moshi.adapter(clazz).fromJson(json);
    }

    /** @return Unmarshalled collection of objects. */
    public static <T> T unmarshal(@NonNull Moshi moshi, TypeToken<T> typeToken, @Nullable String json) throws IOException {
        // From the manual: "Fairly hideous...  Unfortunately, no way to get around this in Java".
        return (T) moshi.adapter(typeToken.getType()).fromJson(json);
    }

    private MoshiUnmarshaller() { }
}
