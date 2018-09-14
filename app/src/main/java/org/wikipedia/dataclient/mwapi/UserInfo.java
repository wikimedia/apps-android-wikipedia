package org.wikipedia.dataclient.mwapi;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public class UserInfo {
    private String name;
    private int id;

    // Object type is any JSON type.
    @Nullable private Map<String, ?> options;

    public int id() {
        return id;
    }

    @NonNull public Map<String, String> userjsOptions() {
        Map<String, String> map = new HashMap<>();
        if (options != null) {
            for (Map.Entry<String, ?> entry : options.entrySet()) {
                if (entry.getKey().startsWith("userjs-")) {
                    // T161866 entry.valueOf() should always return a String but doesn't
                    map.put(entry.getKey(), entry.getValue() == null ? "" : String.valueOf(entry.getValue()));
                }
            }
        }
        return map;
    }
}
