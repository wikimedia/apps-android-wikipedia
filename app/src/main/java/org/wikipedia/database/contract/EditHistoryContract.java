package org.wikipedia.database.contract;

import android.net.Uri;
import android.provider.BaseColumns;

import org.wikipedia.database.DbUtil;
import org.wikipedia.database.column.DateColumn;
import org.wikipedia.database.column.LongColumn;
import org.wikipedia.database.column.StrColumn;

@SuppressWarnings("checkstyle:interfaceistype")
public interface EditHistoryContract {
    String TABLE = "editsummaries";

    interface Col {
        LongColumn ID = new LongColumn(TABLE, BaseColumns._ID, "integer primary key");
        StrColumn SUMMARY = new StrColumn(TABLE, "summary", "string");
        DateColumn LAST_USED = new DateColumn(TABLE, "lastUsed", "integer");

        String[] SELECTION = DbUtil.qualifiedNames(SUMMARY);
    }

    interface Summary extends Col {
        String TABLES = TABLE;
        String PATH = "history/edit/summary";
        Uri URI = Uri.withAppendedPath(AppContentProviderContract.AUTHORITY_BASE, PATH);
        String[] PROJECTION = null;
        String ORDER_MRU = LAST_USED.qualifiedName() + " desc";
    }
}
