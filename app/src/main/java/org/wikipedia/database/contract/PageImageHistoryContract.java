package org.wikipedia.database.contract;

import android.net.Uri;
import android.provider.BaseColumns;

import org.wikipedia.database.DbUtil;
import org.wikipedia.database.column.LongColumn;
import org.wikipedia.database.column.StrColumn;

@SuppressWarnings("checkstyle:interfaceistype")
public interface PageImageHistoryContract {
    String TABLE = "pageimages";

    interface Col {
        LongColumn ID = new LongColumn(TABLE, BaseColumns._ID, "integer primary key");
        StrColumn SITE = new StrColumn(TABLE, "site", "string");
        StrColumn LANG = new StrColumn(TABLE, "lang", "text");
        StrColumn API_TITLE = new StrColumn(TABLE, "title", "string");
        StrColumn DISPLAY_TITLE = new StrColumn(TABLE, "displayTitle", "string"); // the "correct" title, e.g. iPhone
        StrColumn NAMESPACE = new StrColumn(TABLE, "namespace", "string");
        StrColumn IMAGE_NAME = new StrColumn(TABLE, "imageName", "string");

        String[] SELECTION = DbUtil.qualifiedNames(SITE, LANG, NAMESPACE, API_TITLE);
    }

    interface Image extends Col {
        String TABLES = TABLE;
        String PATH = "history/page/image";
        Uri URI = Uri.withAppendedPath(AppContentProviderContract.AUTHORITY_BASE, PATH);
        String[] PROJECTION = null;
    }
}
