package org.wikipedia.dataclient.mwapi;

import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArraySet;

@SuppressWarnings("unused")
public class ListUserResponse {
    @SerializedName("name") @Nullable private String name;
    private long userid;
    @Nullable private List<String> groups;
    @Nullable private String cancreate;
    @Nullable private List<UserResponseCreateError> cancreateerror;

    @Nullable public String name() {
        return name;
    }

    public boolean canCreate() {
        return cancreate != null;
    }

    @NonNull public Set<String> getGroups() {
        return groups != null ? new ArraySet<>(groups) : Collections.emptySet();
    }

    public static class UserResponseCreateError {
        @Nullable private String message;
        @Nullable private String code;
        @Nullable private String type;

        @NonNull public String message() {
            return StringUtils.defaultString(message);
        }
    }
}
