package org.wikipedia.database.contract;

import android.content.ContentResolver;
import android.net.Uri;

import org.wikipedia.BuildConfig;
import org.wikipedia.database.DbUtil;
import org.wikipedia.database.column.IdColumn;
import org.wikipedia.database.column.StrColumn;
import org.wikipedia.database.http.HttpColumns;

@SuppressWarnings("checkstyle:interfaceistype")
public interface UserOptionContract {
    String TABLE = "useroption";

    interface Col {
        IdColumn ID = new IdColumn(TABLE);
        StrColumn KEY = new StrColumn(TABLE, "key", "text not null unique");
        StrColumn VAL = new StrColumn(TABLE, "val", "text");
        HttpColumns HTTP = new HttpColumns(TABLE, "sync");

        String[] SELECTION = DbUtil.qualifiedNames(KEY);
    }

    interface Option extends Col {
        String TABLES = TABLE;
        String PATH = null;
        String AUTHORITY = BuildConfig.USER_OPTION_AUTHORITY;
        Uri URI = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(BuildConfig.USER_OPTION_AUTHORITY)
                .build();
        String[] PROJECTION = null;
    }
}