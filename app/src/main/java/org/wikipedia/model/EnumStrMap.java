package org.wikipedia.model;

import android.support.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

public class EnumStrMap<T extends Enum<T> & EnumStr> {
    @NonNull private final Map<String, T> map;

    public EnumStrMap(@NonNull Class<T> enumeration) {
        map = strToEnumMap(enumeration);
    }

    @NonNull public T get(@NonNull String str) {
        T status = map.get(str);
        if (status == null) {
            throw new IllegalArgumentException("str=" + str);
        }
        return status;
    }

    @NonNull private Map<String, T> strToEnumMap(@NonNull Class<T> enumeration) {
        Map<String, T> ret = new HashMap<>();
        for (T value : enumeration.getEnumConstants()) {
            ret.put(value.str(), value);
        }
        return ret;
    }
}