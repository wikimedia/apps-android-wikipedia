package org.wikipedia.model;

import android.support.annotation.NonNull;
import android.util.SparseArray;

public class EnumCodeMap<T extends Enum<T> & EnumCode> {
    @NonNull private final SparseArray<T> map;

    public EnumCodeMap(@NonNull Class<T> enumeration) {
        map = codeToEnumMap(enumeration);
    }

    @NonNull public T get(int code) {
        T status = map.get(code);
        if (status == null) {
            throw new IllegalArgumentException("code=" + code);
        }
        return status;
    }

    @NonNull private SparseArray<T> codeToEnumMap(@NonNull Class<T> enumeration) {
        SparseArray<T> ret = new SparseArray<>();
        for (T value : enumeration.getEnumConstants()) {
            ret.put(value.code(), value);
        }
        return ret;
    }

    public int size() {
        return map.size();
    }
}
