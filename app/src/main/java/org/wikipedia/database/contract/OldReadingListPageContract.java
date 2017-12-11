package org.wikipedia.database.contract;

import android.support.annotation.NonNull;
import android.support.v4.util.ArraySet;

import org.wikipedia.database.column.CsvColumn;
import org.wikipedia.database.column.IdColumn;
import org.wikipedia.database.column.IntColumn;
import org.wikipedia.database.column.LongColumn;
import org.wikipedia.database.column.NamespaceColumn;
import org.wikipedia.database.column.StrColumn;

import java.util.Collection;
import java.util.Set;

@SuppressWarnings("checkstyle:interfaceistype")
public interface OldReadingListPageContract {
    String TABLE_PAGE = "readinglistpage";
    String TABLE_HTTP = "readinglistpagehttp";
    String TABLE_DISK = "readinglistpagedisk";
    String PATH = "readinglist";

    int STATUS_ONLINE = 0;
    int STATUS_SAVED = 1;
    int STATUS_OUTDATED = 2;
    int STATUS_UNSAVED = 3;
    int STATUS_DELETED = 4;

    interface Col {
        IdColumn ID = new IdColumn(TABLE_PAGE);
        StrColumn KEY = new StrColumn(TABLE_PAGE, "key", "text not null unique");
        CsvColumn<Set<String>> LIST_KEYS = new CsvColumn<Set<String>>(TABLE_PAGE, "listKeys",
                "text not null") {
            @NonNull @Override protected Set<String> val(@NonNull Collection<String> strs) {
                return new ArraySet<>(strs);
            }
            @NonNull @Override protected Collection<String> put(@NonNull Set<String> row) {
                return row;
            }
        };
        StrColumn SITE = new StrColumn(TABLE_PAGE, "site", "text not null");
        StrColumn LANG = new StrColumn(TABLE_PAGE, "lang", "text");
        NamespaceColumn NAMESPACE = new NamespaceColumn(TABLE_PAGE, "namespace");
        StrColumn TITLE = new StrColumn(TABLE_PAGE, "title", "text not null");
        IntColumn DISK_PAGE_REV = new IntColumn(TABLE_PAGE, "diskPageRev", "integer");
        LongColumn MTIME = new LongColumn(TABLE_PAGE, "mtime", "integer not null");
        LongColumn ATIME = new LongColumn(TABLE_PAGE, "atime", "integer not null");
        StrColumn THUMBNAIL_URL = new StrColumn(TABLE_PAGE, "thumbnailUrl", "text");
        StrColumn DESCRIPTION = new StrColumn(TABLE_PAGE, "description", "text");
        LongColumn PHYSICAL_SIZE = new LongColumn(TABLE_PAGE, "physicalSize", "integer");
        LongColumn LOGICAL_SIZE = new LongColumn(TABLE_PAGE, "logicalSize", "integer");

        StrColumn DISK_KEY = new StrColumn(TABLE_DISK, "diskKey", "text not null unique");
        LongColumn DISK_STATUS = new LongColumn(TABLE_DISK, "diskStatus", "integer");
    }
}
