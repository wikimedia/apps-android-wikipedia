package org.wikipedia.database.contract;

import android.net.Uri;
import android.provider.BaseColumns;

import org.wikipedia.database.DbUtil;
import org.wikipedia.database.column.DateColumn;
import org.wikipedia.database.column.IntColumn;
import org.wikipedia.database.column.LongColumn;
import org.wikipedia.database.column.StrColumn;
import org.wikipedia.pageimages.PageImage;
import org.wikipedia.pageimages.PageImageDatabaseTable;

@SuppressWarnings("checkstyle:interfaceistype")
public final class PageHistoryContract {
    public static final String TABLE = "history";
    private static final String PATH = "history";

    public interface Col {
        LongColumn ID = new LongColumn(TABLE, BaseColumns._ID, "integer primary key");
        StrColumn SITE = new StrColumn(TABLE, "site", "string");
        StrColumn LANG = new StrColumn(TABLE, "lang", "text");
        StrColumn TITLE = new StrColumn(TABLE, "title", "string");
        StrColumn NAMESPACE = new StrColumn(TABLE, "namespace", "string");
        DateColumn TIMESTAMP = new DateColumn(TABLE, "timestamp", "integer");
        IntColumn SOURCE = new IntColumn(TABLE, "source", "integer");

        String[] SELECTION = DbUtil.qualifiedNames(SITE, LANG, NAMESPACE, TITLE);
    }

    public interface Page extends Col {
        String TABLES = TABLE;
        String PATH = PageHistoryContract.PATH + "/page";
        Uri URI = Uri.withAppendedPath(AppContentProviderContract.AUTHORITY_BASE, PATH);
        String[] PROJECTION = null;
        String ORDER_MRU = TIMESTAMP.qualifiedName() + " desc";
    }

    public interface PageWithImage extends Page {
        String TABLES = (":tbl left outer join :pageImagesTbl "
                + "on (:tbl.site = :pageImagesTbl.:site and :tbl.title = :pageImagesTbl.:title)")
                .replaceAll(":tbl.site", SITE.qualifiedName())
                .replaceAll("(:pageImagesTbl.):site", "$1" + PageImageDatabaseTable.Col.SITE.getName())
                .replaceAll(":tbl.title", TITLE.qualifiedName())
                .replaceAll("(:pageImagesTbl.):title", "$1" + PageImageDatabaseTable.Col.TITLE.getName())
                .replaceAll(":tbl", PageHistoryContract.TABLE)
                .replaceAll(":pageImagesTbl", PageImage.DATABASE_TABLE.getTableName());

        String PATH = Page.PATH + "/with_image";
        Uri URI = Uri.withAppendedPath(AppContentProviderContract.AUTHORITY_BASE, PATH);

        StrColumn IMAGE_NAME = PageImageDatabaseTable.Col.IMAGE_NAME;

        String[] PROJECTION = DbUtil.qualifiedNames(ID, SITE, LANG, TITLE, NAMESPACE, TIMESTAMP,
                SOURCE, IMAGE_NAME);
    }

    private PageHistoryContract() { }
}
