package org.wikipedia.model;

import android.support.annotation.NonNull;

public interface CodeEnum<T> {
    @NonNull T enumeration(int code);
}
