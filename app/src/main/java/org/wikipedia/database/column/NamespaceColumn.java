package org.wikipedia.database.column;

import org.wikipedia.page.Namespace;

import androidx.annotation.NonNull;

public class NamespaceColumn extends CodeEnumColumn<Namespace> {
    public NamespaceColumn(@NonNull String tbl, @NonNull String name) {
        super(tbl, name, Namespace.CODE_ENUM);
    }
}
