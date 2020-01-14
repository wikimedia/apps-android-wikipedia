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
}
