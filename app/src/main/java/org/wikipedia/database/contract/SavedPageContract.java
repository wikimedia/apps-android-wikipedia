package org.wikipedia.database.contract;

import android.net.Uri;
import android.provider.BaseColumns;

import org.wikipedia.database.DbUtil;
import org.wikipedia.database.column.DateColumn;
import org.wikipedia.database.column.LongColumn;
import org.wikipedia.database.column.StrColumn;

@Deprecated @SuppressWarnings("checkstyle:interfaceistype")
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
        String PATH = SavedPageContract.PATH + "/page";
        Uri URI = Uri.withAppendedPath(AppContentProviderContract.AUTHORITY_BASE, PATH);
    }

    private SavedPageContract() { }
}
