package org.wikipedia.dataclient.page;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Protection settings for a page */
public class Protection {
    @Nullable private String edit;
    @Nullable private String move;

    @Nullable
    public String getFirstAllowedEditorRole() {
        return edit == null || edit.isEmpty() ? "" : edit;
    }

    @NonNull
    public Set<String> getEditRoles() {
        Set<String> roleSet = new HashSet<>();
        roleSet.add(edit);
        return edit != null ? Collections.unmodifiableSet(roleSet) : Collections.emptySet();
    }
}
