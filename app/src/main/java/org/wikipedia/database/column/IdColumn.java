package org.wikipedia.database.column;

import android.provider.BaseColumns;

public class IdColumn extends LongColumn {
    public IdColumn() {
        super(BaseColumns._ID, "integer primary key autoincrement");
    }
}