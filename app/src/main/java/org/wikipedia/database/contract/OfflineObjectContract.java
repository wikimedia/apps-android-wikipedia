package org.wikipedia.database.contract;

import android.net.Uri;

import org.wikipedia.database.DbUtil;
import org.wikipedia.database.column.IdColumn;
import org.wikipedia.database.column.IntColumn;
import org.wikipedia.database.column.StrColumn;

@SuppressWarnings("checkstyle:interfaceistype")
public interface OfflineObjectContract {
    String TABLE = "offlineobject";
    Uri URI = Uri.withAppendedPath(AppContentProviderContract.AUTHORITY_BASE, "/offlineobject");

    interface Col {
        IdColumn ID = new IdColumn(TABLE);
        StrColumn URL = new StrColumn(TABLE, "url", "string not null");
        StrColumn LANG = new StrColumn(TABLE, "lang", "string");
        StrColumn PATH = new StrColumn(TABLE, "path", "string not null");
        StrColumn USEDBY = new StrColumn(TABLE, "usedby", "string");
        IntColumn STATUS = new IntColumn(TABLE, "status", "integer not null");

        String[] SELECTION = DbUtil.qualifiedNames(URL);
        String[] ALL = DbUtil.qualifiedNames(ID, URL, LANG, PATH, USEDBY, STATUS);
    }
}
