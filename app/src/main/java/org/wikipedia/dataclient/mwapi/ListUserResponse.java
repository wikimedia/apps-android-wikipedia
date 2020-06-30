package org.wikipedia.dataclient.mwapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArraySet;

import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@SuppressWarnings("unused")
public class ListUserResponse {
    @SerializedName("name") @Nullable private String name;
    private long userid;
    @Nullable private List<String> groups;
    private boolean missing;
    private boolean cancreate;
    @Nullable private List<MwServiceError> cancreateerror;

    @NonNull public String name() {
        return StringUtils.defaultString(name);
    }

    public boolean canCreate() {
        return cancreate;
    }

    public boolean isBlocked() {
        return getError().contains("block");
    }

    @NonNull public String getError() {
        return cancreateerror != null && !cancreateerror.isEmpty() ? cancreateerror.get(0).getTitle() : "";
    }

    @NonNull public Set<String> getGroups() {
        return groups != null ? new ArraySet<>(groups) : Collections.emptySet();
    }
}
