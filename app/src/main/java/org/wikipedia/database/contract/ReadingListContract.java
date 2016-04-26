package org.wikipedia.database.contract;

import android.net.Uri;

import org.wikipedia.database.DbUtil;
import org.wikipedia.database.column.CodeEnumColumn;
import org.wikipedia.database.column.CsvColumn;
import org.wikipedia.database.column.IdColumn;
import org.wikipedia.database.column.IntColumn;
import org.wikipedia.database.column.LongColumn;
import org.wikipedia.database.column.NamespaceColumn;
import org.wikipedia.database.column.StrColumn;
import org.wikipedia.readinglist.page.database.disk.DiskStatus;

import java.util.Set;

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
        String[] ALL = DbUtil.qualifiedNames(ID, KEY, TITLE, MTIME, ATIME, DESCRIPTION);
    }

    interface List extends Col {
        String TABLES = TABLE;
        String PATH = ReadingListPageContract.Disk.PATH + "/list";
        Uri URI = Uri.withAppendedPath(AppContentProviderContract.AUTHORITY_BASE, PATH);
        String[] PROJECTION = null;

        String ORDER_KEY = KEY.qualifiedName();
        String ORDER_MRU = ":atimeCol desc".replaceAll(":atimeCol", ATIME.qualifiedName());
    }

    final class ListWithPagesAndDisk implements List {
        public static final String PATH = List.PATH + "/with_page";
        public static final Uri URI = Uri.withAppendedPath(AppContentProviderContract.AUTHORITY_BASE, PATH);

        public static final StrColumn PAGE_KEY = ReadingListPageContract.PageCol.KEY;
        public static final CsvColumn<Set<String>> PAGE_LIST_KEYS = ReadingListPageContract.PageCol.LIST_KEYS;
        public static final StrColumn PAGE_LANG = ReadingListPageContract.PageCol.LANG;
        public static final NamespaceColumn PAGE_NAMESPACE = ReadingListPageContract.PageCol.NAMESPACE;
        public static final StrColumn PAGE_TITLE = ReadingListPageContract.PageCol.TITLE;
        public static final IntColumn PAGE_DISK_PAGE_REV = ReadingListPageContract.PageCol.DISK_PAGE_REV;
        public static final LongColumn PAGE_MTIME = ReadingListPageContract.PageCol.MTIME;
        public static final LongColumn PAGE_ATIME = ReadingListPageContract.PageCol.ATIME;
        public static final StrColumn PAGE_THUMBNAIL_URL = ReadingListPageContract.PageCol.THUMBNAIL_URL;
        public static final StrColumn PAGE_DESCRIPTION = ReadingListPageContract.PageCol.DESCRIPTION;

        public static final StrColumn PAGE_DISK_KEY = ReadingListPageContract.DiskCol.KEY;
        public static final CodeEnumColumn<DiskStatus> PAGE_DISK_STATUS = ReadingListPageContract.DiskCol.STATUS;
        public static final LongColumn PAGE_DISK_TIMESTAMP = ReadingListPageContract.DiskCol.TIMESTAMP;
        public static final LongColumn PAGE_DISK_TRANSACTION_ID = ReadingListPageContract.DiskCol.TRANSACTION_ID;
        public static final StrColumn PAGE_DISK_FILENAME = ReadingListPageContract.DiskCol.FILENAME;

        public static final String TABLES = (
                  ":tbl left join :pageTbl on (',' || :pageTbl.listKeysCol || ',') like ('%,' || :tbl.keyCol || ',%') "
                + "left join :diskTbl on :diskTbl.keyCol = :pageTbl.keyCol")
                .replaceAll(":tbl.keyCol", KEY.qualifiedName())
                .replaceAll(":pageTbl.listKeysCol", PAGE_LIST_KEYS.qualifiedName())
                .replaceAll(":diskTbl.keyCol", PAGE_DISK_KEY.qualifiedName())
                .replaceAll(":pageTbl.keyCol", PAGE_KEY.qualifiedName())
                .replaceAll(":tbl", TABLE)
                .replaceAll(":pageTbl", ReadingListPageContract.TABLE_PAGE)
                .replaceAll(":diskTbl", ReadingListPageContract.TABLE_DISK);

        public static final String[] PROJECTION;
        static {
            PROJECTION = new String[ALL.length + ReadingListPageContract.PageCol.CONTENT.length + ReadingListPageContract.DiskCol.CONTENT.length];
            System.arraycopy(ALL, 0, PROJECTION, 0, ALL.length);
            System.arraycopy(ReadingListPageContract.PageCol.CONTENT, 0, PROJECTION, ALL.length,
                    ReadingListPageContract.PageCol.CONTENT.length);
            System.arraycopy(ReadingListPageContract.DiskCol.CONTENT, 0, PROJECTION,
                    ALL.length + ReadingListPageContract.PageCol.CONTENT.length,
                    ReadingListPageContract.DiskCol.CONTENT.length);
        }

        private ListWithPagesAndDisk() { }
    }
}
