package org.wikipedia.database.column;

import android.support.annotation.NonNull;

import org.wikipedia.page.Namespace;

public class NamespaceColumn extends CodeEnumColumn<Namespace> {
    public NamespaceColumn(@NonNull String tbl, @NonNull String name) {
        super(tbl, name, Namespace.CODE_ENUM);
    }
}
