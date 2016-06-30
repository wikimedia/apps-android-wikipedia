package org.wikipedia.server;

import android.support.annotation.Nullable;

/** Protection settings for a page */
public class Protection {
    @SuppressWarnings("MismatchedReadAndWriteOfArray") private String[] edit = new String[]{};

    // TODO should send them all, but callers need to be updated, too, (future patch)
    @Nullable
    public String getFirstAllowedEditorRole() {
        if (edit.length > 0) {
            return edit[0];
        }
        return null;
    }
}