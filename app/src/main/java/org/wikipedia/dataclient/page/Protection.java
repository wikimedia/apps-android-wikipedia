package org.wikipedia.dataclient.page;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Protection settings for a page */
public class Protection {
    @Nullable private String edit;
    @Nullable private String type;
    @Nullable private String level;
    @Nullable private String expiry;

    public String getFirstAllowedEditorRole() {
        return StringUtils.defaultString(edit);
    }

    @NonNull
    public Set<String> getEditRoles() {
        Set<String> roleSet = new HashSet<>();
        if (edit != null) {
            roleSet.add(edit);
        }
        return Collections.unmodifiableSet(roleSet);
    }

    @NonNull
    public String getType() {
        return StringUtils.defaultString(type);
    }

    @NonNull
    public String getLevel() {
        return StringUtils.defaultString(level);
    }

    @NonNull
    public String getExpiry() {
        return StringUtils.defaultString(expiry);
    }
}
