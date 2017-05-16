package org.wikipedia.database.contract;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.util.ArraySet;

import org.wikipedia.database.DbUtil;
import org.wikipedia.database.column.CodeEnumColumn;
import org.wikipedia.database.column.CsvColumn;
import org.wikipedia.database.column.IdColumn;
import org.wikipedia.database.column.IntColumn;
import org.wikipedia.database.column.LongColumn;
import org.wikipedia.database.column.NamespaceColumn;
import org.wikipedia.database.column.StrColumn;
import org.wikipedia.database.http.HttpColumns;
import org.wikipedia.database.http.HttpStatus;
import org.wikipedia.readinglist.page.ReadingListPageRow;
import org.wikipedia.readinglist.page.database.disk.DiskColumns;
import org.wikipedia.readinglist.page.database.disk.DiskStatus;

import java.util.Collection;
import java.util.Set;

@SuppressWarnings("checkstyle:interfaceistype")
public final class ReadingListPageContract {
    public static final String TABLE_PAGE = "readinglistpage";
    public static final String TABLE_HTTP = "readinglistpagehttp";
    public static final String TABLE_DISK = "readinglistpagedisk";
    private static final String PATH = "readinglist";

    public interface PageCol {
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

        // TODO: should null (autoselect system language) be allowed? It might be more meaningful to
        //       force clients to store the nonnull active system language instead. This
        //       implementation matches that used by other tables. A change made here should be made
        //       elsewhere too.
        StrColumn LANG = new StrColumn(TABLE_PAGE, "lang", "text");

        NamespaceColumn NAMESPACE = new NamespaceColumn(TABLE_PAGE, "namespace");
        StrColumn TITLE = new StrColumn(TABLE_PAGE, "title", "text not null");
        // TODO: use page ID not title. Page IDs are a concise and canonical way to uniquely refer
        //       to a page but there are many places where we do not preserve the ID.
        //IntColumn PAGE_ID = new IntColumn(TABLE_PAGE, "pageId", "integer not null");
        IntColumn DISK_PAGE_REV = new IntColumn(TABLE_PAGE, "diskPageRev", "integer");
        LongColumn MTIME = new LongColumn(TABLE_PAGE, "mtime", "integer not null");
        LongColumn ATIME = new LongColumn(TABLE_PAGE, "atime", "integer not null");
        StrColumn THUMBNAIL_URL = new StrColumn(TABLE_PAGE, "thumbnailUrl", "text");
        StrColumn DESCRIPTION = new StrColumn(TABLE_PAGE, "description", "text");

        // The cumulative size in bytes for an offline page and all page resources downloaded by
        // SavedPageSyncService. Null or 0 if DiskStatus.ONLINE, not yet downloaded, or not yet
        // downloaded since these columns were added. Outdated if the saved page cache size is later
        // exceeded and resources are evicted. Written to by SavedPageSyncService.
        // Android appears to present the user with logical size in app settings so it is the
        // preferred metric to display and physical size will likely never be used. Since quantities
        // are aggregated across files, neither can be derived from the other.
        // wc -c /data/data/org.wikipedia.dev/files/okhttp-cache/*.[0-9]|tail -n1
        // stat -c %s /data/data/org.wikipedia.dev/files/okhttp-cache/*.[0-9]
        LongColumn PHYSICAL_SIZE = new LongColumn(TABLE_PAGE, "physicalSize", "integer");
        // du -c /data/data/org.wikipedia.dev/files/okhttp-cache/*.[0-9]|tail -n1
        // Block size: stat -c %B /data/data/org.wikipedia.dev/files/okhttp-cache
        LongColumn LOGICAL_SIZE = new LongColumn(TABLE_PAGE, "logicalSize", "integer");

        // Example:
        // 1 Download the Obama article.
        //   - Physical size recorded by us is 5 729 692 bytes.
        //   - Logical size recorded by us is 6 754 304 bytes (6 596 kibibytes).
        // 2 Terminate the app and check the sizes (note: journal size is never included):
        //   - Physical: wc -c /data/data/org.wikipedia.dev/files/okhttp-cache/*.[0-9]|tail -n1 => 5 729 692 bytes.
        //   - Logical: du -c /data/data/org.wikipedia.dev/files/okhttp-cache/*.[0-9]|tail -n1 => 6 596 kibibytes.
        //   - The size of "data" is about 6 868 kibibytes (6.7070313 mebibytes):
        //     - Calculate the size of all data: du -c /data/data/org.wikipedia.dev|tail -n1 => 13 736 kibibytes.
        //     - Subtract the size of the cache: du -c /data/data/org.wikipedia.dev/cache|tail -n1 => 6 868 kibibytes.
        // 3 Open settings: data size is 6.71 mebibytes.
        // 4 Dump the database records: sqlite3 /data/data/org.wikipedia.dev/databases/wikipedia.db '.dump readinglistpage'

        String[] SELECTION = DbUtil.qualifiedNames(KEY);
        String[] ALL = DbUtil.qualifiedNames(ID, KEY, LIST_KEYS, SITE, LANG, NAMESPACE, TITLE,
                DISK_PAGE_REV, MTIME, ATIME, THUMBNAIL_URL, DESCRIPTION, PHYSICAL_SIZE, LOGICAL_SIZE);
        String[] CONTENT = DbUtil.qualifiedNames(KEY, LIST_KEYS, SITE, LANG, NAMESPACE, TITLE,
                DISK_PAGE_REV, MTIME, ATIME, THUMBNAIL_URL, DESCRIPTION, PHYSICAL_SIZE, LOGICAL_SIZE);
    }

    public static final HttpColumns<ReadingListPageRow> HTTP_COLS = new HttpColumns<>(TABLE_HTTP);
    public interface HttpCol {
        IdColumn ID = HTTP_COLS.id();
        StrColumn KEY = HTTP_COLS.key();
        CodeEnumColumn<HttpStatus> STATUS = HTTP_COLS.status();
        LongColumn TIMESTAMP = HTTP_COLS.timestamp();
        LongColumn TRANSACTION_ID = HTTP_COLS.transactionId();

        String[] SELECTION = HTTP_COLS.selection();
        String[] CONTENT = HTTP_COLS.content();
    }

    public static final DiskColumns<ReadingListPageRow> DISK_COLS = new DiskColumns<>(TABLE_DISK);
    public static class DiskCol {
        public static final IdColumn ID = DISK_COLS.id();
        public static final StrColumn KEY = DISK_COLS.key();
        public static final CodeEnumColumn<DiskStatus> STATUS = DISK_COLS.status();
        public static final LongColumn TIMESTAMP = DISK_COLS.timestamp();
        public static final LongColumn TRANSACTION_ID = DISK_COLS.transactionId();
        public static final StrColumn FILENAME = new StrColumn(TABLE_DISK, "filename", "text");

        public static final String[] SELECTION = DISK_COLS.selection();
        public static final String[] CONTENT;
        static {
            CONTENT = new String[DISK_COLS.content().length + 1];
            System.arraycopy(DISK_COLS.content(), 0, CONTENT, 0, DISK_COLS.content().length);
            CONTENT[DISK_COLS.content().length] = FILENAME.qualifiedName();
        }
    }

    public interface Page extends PageCol {
        String TABLES = TABLE_PAGE;
        String PATH = ReadingListPageContract.PATH + "/page";
        Uri URI = Uri.withAppendedPath(AppContentProviderContract.AUTHORITY_BASE, PATH);
        String[] PROJECTION = null;

        String ORDER_MRU = ":atimeCol desc".replaceAll(":atimeCol", ATIME.qualifiedName());
        String ORDER_ALPHABETICAL = ":titleCol asc".replaceAll(":titleCol", TITLE.qualifiedName());
    }

    public interface Http extends HttpCol {
        String TABLES = TABLE_HTTP;

        // HACK: Http has no real dependency on Option. However, HttpWithOption is a composite of
        //       Option and Http and observers expect to be notified when _either_ change. Making
        //       this path hierarchical allows HttpWithOption to also be hierarchical but needlessly
        //       notifies Http clients when Option changes. More here:
        //       - http://chalup.github.io/blog/2014/09/14/contentprovider-series-uris/
        //       - https://gist.github.com/chalup/4201307da02b9cfe4f40
        String PATH = Page.PATH + "/http";

        Uri URI = Uri.withAppendedPath(AppContentProviderContract.AUTHORITY_BASE, PATH);
        String[] PROJECTION = null;
    }

    public static final class Disk extends DiskCol {
        public static final String TABLES = TABLE_DISK;

        // HACK: Http has no real dependency on Option. However, HttpWithOption is a composite of
        //       Option and Http and observers expect to be notified when _either_ change. Making
        //       this path hierarchical allows HttpWithOption to also be hierarchical but needlessly
        //       notifies Http clients when Option changes. More here:
        //       - http://chalup.github.io/blog/2014/09/14/contentprovider-series-uris/
        //       - https://gist.github.com/chalup/4201307da02b9cfe4f40
        public static final String PATH = Page.PATH + "/disk";

        public static final Uri URI = Uri.withAppendedPath(AppContentProviderContract.AUTHORITY_BASE, PATH);
        public static final String[] PROJECTION = null;

        private Disk() { }
    }

    public static final class HttpWithPage implements Page {
        public static final String TABLES = ":httpTbl left join :tbl on (:tbl.keyCol = :httpTbl.keyCol)"
                .replaceAll(":tbl.keyCol", KEY.qualifiedName())
                .replaceAll(":httpTbl.keyCol", HttpCol.KEY.qualifiedName())
                .replaceAll(":httpTbl", TABLE_HTTP)
                .replaceAll(":tbl", TABLE_PAGE);

        public static final String PATH = Http.PATH + "/with_http";
        public static final Uri URI = Uri.withAppendedPath(AppContentProviderContract.AUTHORITY_BASE, PATH);

        public static final StrColumn HTTP_KEY = HttpCol.KEY;
        public static final CodeEnumColumn<HttpStatus> HTTP_STATUS = HttpCol.STATUS;
        public static final LongColumn HTTP_TIMESTAMP = HttpCol.TIMESTAMP;
        public static final LongColumn HTTP_TRANSACTION_ID = HttpCol.TRANSACTION_ID;

        public static final String[] PROJECTION;
        static {
            PROJECTION = new String[ALL.length + HttpCol.CONTENT.length];
            System.arraycopy(ALL, 0, PROJECTION, 0, ALL.length);
            System.arraycopy(HttpCol.CONTENT, 0, PROJECTION, ALL.length, HttpCol.CONTENT.length);
        }

        private HttpWithPage() { }
    }

    public static final class DiskWithPage implements Page {
        public static final String TABLES = ":diskTbl left join :tbl on (:tbl.keyCol = :diskTbl.keyCol)"
                .replaceAll(":tbl.keyCol", KEY.qualifiedName())
                .replaceAll(":diskTbl.keyCol", DiskCol.KEY.qualifiedName())
                .replaceAll(":diskTbl", TABLE_DISK)
                .replaceAll(":tbl", TABLE_PAGE);

        public static final String PATH = Disk.PATH + "/with_disk";
        public static final Uri URI = Uri.withAppendedPath(AppContentProviderContract.AUTHORITY_BASE, PATH);

        public static final StrColumn DISK_KEY = DiskCol.KEY;
        public static final CodeEnumColumn<DiskStatus> DISK_STATUS = DiskCol.STATUS;
        public static final LongColumn DISK_TIMESTAMP = DiskCol.TIMESTAMP;
        public static final LongColumn DISK_TRANSACTION_ID = DiskCol.TRANSACTION_ID;
        public static final StrColumn DISK_FILENAME = DiskCol.FILENAME;

        public static final String[] PROJECTION;
        static {
            PROJECTION = new String[ALL.length + DiskCol.CONTENT.length];
            System.arraycopy(ALL, 0, PROJECTION, 0, ALL.length);
            System.arraycopy(DiskCol.CONTENT, 0, PROJECTION, ALL.length, DiskCol.CONTENT.length);
        }

        private DiskWithPage() { }
    }

    public static final class PageWithDisk implements Page {
        public static final String TABLES = ":tbl join :diskTbl on (:tbl.keyCol = :diskTbl.keyCol)"
                .replaceAll(":tbl.keyCol", KEY.qualifiedName())
                .replaceAll(":diskTbl.keyCol", DiskCol.KEY.qualifiedName())
                .replaceAll(":diskTbl", TABLE_DISK)
                .replaceAll(":tbl", TABLE_PAGE);

        public static final String PATH = Disk.PATH + "/with_page";
        public static final Uri URI = Uri.withAppendedPath(AppContentProviderContract.AUTHORITY_BASE, PATH);

        public static final StrColumn DISK_KEY = DiskCol.KEY;
        public static final CodeEnumColumn<DiskStatus> DISK_STATUS = DiskCol.STATUS;
        public static final LongColumn DISK_TIMESTAMP = DiskCol.TIMESTAMP;
        public static final LongColumn DISK_TRANSACTION_ID = DiskCol.TRANSACTION_ID;
        public static final StrColumn DISK_FILENAME = DiskCol.FILENAME;

        public static final String[] PROJECTION;
        static {
            PROJECTION = new String[ALL.length + DiskCol.CONTENT.length];
            System.arraycopy(ALL, 0, PROJECTION, 0, ALL.length);
            System.arraycopy(DiskCol.CONTENT, 0, PROJECTION, ALL.length, DiskCol.CONTENT.length);
        }

        private PageWithDisk() { }
    }

    private ReadingListPageContract() { }
}
