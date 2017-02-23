package org.wikipedia.dataclient.okhttp.cache;

import android.support.annotation.Nullable;

public final class SaveHeader {
    public static final String FIELD = "X-Save";
    public static final String VAL_ENABLED = "true";
    public static final String VAL_DISABLED = "false";

    public static boolean isSaveEnabled(@Nullable String val) {
        return VAL_ENABLED.equalsIgnoreCase(val);
    }

    private SaveHeader() { }
}
