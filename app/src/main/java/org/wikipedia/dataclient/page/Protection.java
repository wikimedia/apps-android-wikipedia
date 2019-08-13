package org.wikipedia.dataclient.page;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.Set;

public class Protection {
    @Nullable private Set<String> edit;
    @Nullable private Set<String> move;

    @NonNull
    public String getFirstAllowedEditorRole() {
        return edit == null || edit.isEmpty() ? "" : edit.iterator().next();
    }

    @NonNull
    public Set<String> getEditRoles() {
        return edit != null ? Collections.unmodifiableSet(edit) : Collections.emptySet();
    }
}
