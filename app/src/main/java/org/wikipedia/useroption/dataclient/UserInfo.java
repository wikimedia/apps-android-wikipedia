package org.wikipedia.useroption.dataclient;

import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import org.wikipedia.useroption.UserOption;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class UserInfo {
    @SerializedName("name")
    private String username;
    private int id;

    // Object type is any JSON type.
    @NonNull private Map<String, ?> options;

    public int id() {
        return id;
    }

    @NonNull
    public String username() {
        return username;
    }

    @NonNull
    public Collection<UserOption> userjsOptions() {
        Collection<UserOption> ret = new ArrayList<>();
        for (Map.Entry<String, ?> entry : options.entrySet()) {
            if (entry.getKey().startsWith("userjs-")) {
                ret.add(new UserOption(entry.getKey(), (String) entry.getValue()));
            }
        }
        return ret;
    }

    // Auto-generated
    @Override
    public String toString() {
        return "UserInfo{"
                + "username='" + username + '\''
                + ", id=" + id
                + ", options=" + options
                + '}';
    }
}