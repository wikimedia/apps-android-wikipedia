package org.wikipedia.dataclient.page;

import java.util.Collections;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Protection settings for a page */
public class Protection {
    @SuppressWarnings("MismatchedReadAndWriteOfArray") @NonNull private Set<String> edit = Collections.emptySet();

    // TODO should send them all, but callers need to be updated, too, (future patch)
    @Nullable
    public String getFirstAllowedEditorRole() {
        return edit.isEmpty() ? null : edit.iterator().next();
    }

    @NonNull
    public Set<String> getEditRoles() {
        return Collections.unmodifiableSet(edit);
    }
}
