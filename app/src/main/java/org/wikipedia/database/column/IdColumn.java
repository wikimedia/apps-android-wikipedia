package org.wikipedia.database.column;

import android.provider.BaseColumns;
import android.support.annotation.NonNull;

public class IdColumn extends LongColumn {
    public IdColumn(@NonNull String tbl) {
        super(tbl, BaseColumns._ID, "integer primary key autoincrement");
    }
}
