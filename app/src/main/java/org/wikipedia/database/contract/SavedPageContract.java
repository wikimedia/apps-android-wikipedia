package org.wikipedia.database.contract;

import android.net.Uri;
import android.provider.BaseColumns;

import org.wikipedia.database.DbUtil;
import org.wikipedia.database.column.DateColumn;
import org.wikipedia.database.column.LongColumn;
import org.wikipedia.database.column.StrColumn;

@SuppressWarnings("checkstyle:interfaceistype")
public final class SavedPageContract {
    public static final String TABLE = "savedpages";
    private static final String PATH = "saved";

    public interface Col {
        LongColumn ID = new LongColumn(TABLE, BaseColumns._ID, "integer primary key");
        StrColumn SITE = new StrColumn(TABLE, "site", "string");
        StrColumn LANG = new StrColumn(TABLE, "lang", "text");
        StrColumn TITLE = new StrColumn(TABLE, "title", "string");
        StrColumn NAMESPACE = new StrColumn(TABLE, "namespace", "string");
        DateColumn TIMESTAMP = new DateColumn(TABLE, "timestamp", "integer");

        String[] SELECTION = DbUtil.qualifiedNames(SITE, LANG, NAMESPACE, TITLE);
    }

    public interface Page extends Col {
        String TABLES = TABLE;
        String PATH = SavedPageContract.PATH + "/page";
        Uri URI = Uri.withAppendedPath(AppContentProviderContract.AUTHORITY_BASE, PATH);
        String[] PROJECTION = null;
        String ORDER_ALPHABETICAL = TITLE.qualifiedName() + " asc";
    }

    public interface PageWithImage extends Page {
        String TABLES = (":tbl left outer join :pageImagesTbl "
                + "on (:tbl.site = :pageImagesTbl.site and :tbl.title = :pageImagesTbl.title)")
                .replaceAll(":tbl.site", SITE.qualifiedName())
                .replaceAll(":pageImagesTbl.site", PageImageHistoryContract.Col.SITE.qualifiedName())
                .replaceAll(":tbl.title", TITLE.qualifiedName())
                .replaceAll(":pageImagesTbl.title", PageImageHistoryContract.Col.TITLE.qualifiedName())
                .replaceAll(":tbl", SavedPageContract.TABLE)
                .replaceAll(":pageImagesTbl", PageImageHistoryContract.TABLE);

        String PATH = Page.PATH + "/with_image";
        Uri URI = Uri.withAppendedPath(AppContentProviderContract.AUTHORITY_BASE, PATH);

        StrColumn IMAGE_NAME = PageImageHistoryContract.Col.IMAGE_NAME;

        String[] PROJECTION = DbUtil.qualifiedNames(ID, SITE, LANG, TITLE, NAMESPACE, TIMESTAMP,
                IMAGE_NAME);
    }

    private SavedPageContract() { }
}