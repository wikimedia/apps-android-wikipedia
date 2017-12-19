package org.wikipedia.database.contract;

import android.net.Uri;

import org.wikipedia.database.DbUtil;
import org.wikipedia.database.column.IdColumn;
import org.wikipedia.database.column.IntColumn;
import org.wikipedia.database.column.LongColumn;
import org.wikipedia.database.column.StrColumn;

@SuppressWarnings("checkstyle:interfaceistype")
public interface ReadingListContract {
    String TABLE = "localreadinglist";
    Uri URI = Uri.withAppendedPath(AppContentProviderContract.AUTHORITY_BASE, "/locallist");

    interface Col {
        IdColumn ID = new IdColumn(TABLE);
        StrColumn TITLE = new StrColumn(TABLE, "readingListTitle", "text not null");
        LongColumn MTIME = new LongColumn(TABLE, "readingListMtime", "integer not null");
        LongColumn ATIME = new LongColumn(TABLE, "readingListAtime", "integer not null");
        StrColumn DESCRIPTION = new StrColumn(TABLE, "readingListDescription", "text");
        LongColumn SIZEBYTES = new LongColumn(TABLE, "readingListSizeBytes", "integer not null");
        IntColumn DIRTY = new IntColumn(TABLE, "readingListDirty", "integer not null");
        LongColumn REMOTEID = new LongColumn(TABLE, "readingListRemoteId", "integer not null");

        String[] SELECTION = DbUtil.qualifiedNames(TITLE);
        String[] ALL = DbUtil.qualifiedNames(ID, TITLE, MTIME, ATIME, DESCRIPTION, SIZEBYTES, DIRTY, REMOTEID);
    }
}
