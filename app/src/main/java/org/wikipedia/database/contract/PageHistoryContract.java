package org.wikipedia.database.contract;

import android.net.Uri;
import android.provider.BaseColumns;

import org.wikipedia.database.DbUtil;
import org.wikipedia.database.column.DateColumn;
import org.wikipedia.database.column.IntColumn;
import org.wikipedia.database.column.LongColumn;
import org.wikipedia.database.column.StrColumn;

@SuppressWarnings("checkstyle:interfaceistype")
public final class PageHistoryContract {
    public static final String TABLE = "history";
    private static final String PATH = "history";

    public interface Col {
        LongColumn ID = new LongColumn(TABLE, BaseColumns._ID, "integer primary key");
        StrColumn SITE = new StrColumn(TABLE, "site", "string");
        StrColumn LANG = new StrColumn(TABLE, "lang", "text");
        StrColumn API_TITLE = new StrColumn(TABLE, "title", "string");
        StrColumn DISPLAY_TITLE = new StrColumn(TABLE, "displayTitle", "string");
        StrColumn NAMESPACE = new StrColumn(TABLE, "namespace", "string");
        DateColumn TIMESTAMP = new DateColumn(TABLE, "timestamp", "integer");
        IntColumn SOURCE = new IntColumn(TABLE, "source", "integer");
        IntColumn TIME_SPENT = new IntColumn(TABLE, "timeSpent", "integer"); // seconds

        String[] SELECTION = DbUtil.qualifiedNames(SITE, LANG, NAMESPACE, API_TITLE);
    }

    public interface Page extends Col {
        String TABLES = TABLE;
        String PATH = PageHistoryContract.PATH + "/page";
        Uri URI = Uri.withAppendedPath(AppContentProviderContract.AUTHORITY_BASE, PATH);
        String[] PROJECTION = null;
        String ORDER_MRU = TIMESTAMP.qualifiedName() + " desc";
    }

    public interface PageWithImage extends Page {
        String TABLES = (":tbl LEFT OUTER JOIN :pageImagesTbl "
                      + "ON (:tbl.site = :pageImagesTbl.site "
                      + "AND :tbl.apiTitle = :pageImagesTbl.apiTitle "
                      + "AND :tbl.lang = :pageImagesTbl.lang )")
                .replaceAll(":tbl.site", SITE.qualifiedName())
                .replaceAll(":pageImagesTbl.site", PageImageHistoryContract.Col.SITE.qualifiedName())
                .replaceAll(":tbl.apiTitle", API_TITLE.qualifiedName())
                .replaceAll(":pageImagesTbl.apiTitle", PageImageHistoryContract.Col.API_TITLE.qualifiedName())
                .replaceAll(":tbl.lang", LANG.qualifiedName())
                .replaceAll(":pageImagesTbl.lang", PageImageHistoryContract.Col.LANG.qualifiedName())
                .replaceAll(":tbl", PageHistoryContract.TABLE)
                .replaceAll(":pageImagesTbl", PageImageHistoryContract.TABLE);

        String PATH = Page.PATH + "/with_image";
        Uri URI = Uri.withAppendedPath(AppContentProviderContract.AUTHORITY_BASE, PATH);

        StrColumn IMAGE_NAME = PageImageHistoryContract.Col.IMAGE_NAME;

        String[] PROJECTION = DbUtil.qualifiedNames(ID, SITE, LANG, API_TITLE, DISPLAY_TITLE, NAMESPACE, TIMESTAMP,
                SOURCE, TIME_SPENT, IMAGE_NAME);
    }

    private PageHistoryContract() { }
}
