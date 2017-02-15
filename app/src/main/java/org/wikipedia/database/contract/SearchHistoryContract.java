package org.wikipedia.database.contract;

import android.net.Uri;
import android.provider.BaseColumns;

import org.wikipedia.database.DbUtil;
import org.wikipedia.database.column.DateColumn;
import org.wikipedia.database.column.LongColumn;
import org.wikipedia.database.column.StrColumn;

@SuppressWarnings("checkstyle:interfaceistype")
public interface SearchHistoryContract {
    String TABLE = "recentsearches";

    interface Col {
        LongColumn ID = new LongColumn(TABLE, BaseColumns._ID, "integer primary key");
        StrColumn TEXT = new StrColumn(TABLE, "text", "string");
        DateColumn TIMESTAMP = new DateColumn(TABLE, "timestamp", "integer");

        String[] SELECTION = DbUtil.qualifiedNames(TEXT);
    }

    interface Query extends Col {
        String TABLES = TABLE;
        String PATH = "history/query";
        Uri URI = Uri.withAppendedPath(AppContentProviderContract.AUTHORITY_BASE, PATH);
        String[] PROJECTION = null;
        String ORDER_MRU = TIMESTAMP.qualifiedName() + " desc";
    }
}
