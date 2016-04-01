package org.wikipedia.database.column;

import android.provider.BaseColumns;
import android.support.annotation.NonNull;

public class IdColumn extends LongColumn {
    @Deprecated public IdColumn() {
        this(null);
    }

    public IdColumn(@NonNull String tbl) {
        super(tbl, BaseColumns._ID, "integer primary key autoincrement");
    }
}