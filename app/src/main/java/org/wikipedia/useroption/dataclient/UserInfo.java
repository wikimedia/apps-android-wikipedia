package org.wikipedia.useroption.dataclient;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.wikipedia.useroption.UserOption;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class UserInfo {
    @SuppressWarnings("unused") @SerializedName("name") private String username;
    @SuppressWarnings("unused") private int id;

    // Object type is any JSON type.
    @SuppressWarnings("unused") @Nullable private Map<String, ?> options;

    public int id() {
        return id;
    }

    @NonNull public Collection<UserOption> userjsOptions() {
        Collection<UserOption> ret = new ArrayList<>();
        if (options != null) {
            for (Map.Entry<String, ?> entry : options.entrySet()) {
                if (entry.getKey().startsWith("userjs-")) {
                    // T161866 entry.valueOf() should always return a String but doesn't
                    ret.add(new UserOption(entry.getKey(),
                            entry.getValue() == null ? null : String.valueOf(entry.getValue())));
                }
            }
        }
        return ret;
    }

    // Auto-generated
    @Override public String toString() {
        return "UserInfo{"
                + "username='" + username + '\''
                + ", id=" + id
                + ", options=" + options
                + '}';
    }
}
