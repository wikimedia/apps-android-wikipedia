package org.wikipedia.database.contract;

import android.net.Uri;

import org.wikipedia.database.DbUtil;
import org.wikipedia.database.column.IdColumn;
import org.wikipedia.database.column.LongColumn;
import org.wikipedia.database.column.StrColumn;

@SuppressWarnings("checkstyle:interfaceistype")
public interface ReadingListContract {
    String TABLE = "readinglist";

    interface Col {
        IdColumn ID = new IdColumn(TABLE);
        StrColumn KEY = new StrColumn(TABLE, "readingListKey", "text not null unique");
        StrColumn TITLE = new StrColumn(TABLE, "readingListTitle", "text not null");
        LongColumn MTIME = new LongColumn(TABLE, "readingListMtime", "integer not null");
        LongColumn ATIME = new LongColumn(TABLE, "readingListAtime", "integer not null");
        StrColumn DESCRIPTION = new StrColumn(TABLE, "readingListDescription", "text");

        String[] SELECTION = DbUtil.qualifiedNames(KEY);
    }

    interface List extends Col {
        String PATH = ReadingListPageContract.Disk.PATH + "/list";
        Uri URI = Uri.withAppendedPath(AppContentProviderContract.AUTHORITY_BASE, PATH);
    }
}