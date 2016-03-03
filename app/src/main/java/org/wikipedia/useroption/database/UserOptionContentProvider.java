package org.wikipedia.useroption.database;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.wikipedia.BuildConfig;
import org.wikipedia.database.DbUtil;
import org.wikipedia.database.SQLiteContentProvider;
import org.wikipedia.useroption.database.UserOptionDatabaseTable.Col;
import org.wikipedia.util.StringUtil;

public class UserOptionContentProvider extends SQLiteContentProvider {
    // This SQL upsert statement preserves the ID of an existing entry on replace. We could specify
    // "on conflict replace" for the key column when creating the table but then stable IDs break on
    // modify.
    private static final String UPSERT_SQL = (
              "insert or replace into :tbl (:cols) "
            + "select old.:idCol, new.:contentCols "
            + "from (select :contentVals) as new "
            + "left join (select :idCol, :selectionCol from :tbl) as old on new.:selectionCol = old.:selectionCol;")
                    .replaceAll(":tbl", BuildConfig.USER_OPTION_TABLE)
                    .replaceAll(":cols", TextUtils.join(",", DbUtil.names(Col.ALL)))
                    .replaceAll(":idCol", Col.ID.getName())
                    .replaceAll("(\\S*):contentCols", TextUtils.join(",", StringUtil.prefix("$1", DbUtil.names(Col.CONTENT))))
                    .replaceAll(":contentVals", TextUtils.join(",", StringUtil.prefix("? as ", DbUtil.names(Col.CONTENT))))
                    .replaceAll("(\\S*):selectionCol", "$1" + Col.SELECTION);

    public UserOptionContentProvider() {
        super(UserOptionRow.DATABASE_TABLE);
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        return upsert(uri, values);
    }

    private Uri upsert(@NonNull Uri uri, @NonNull ContentValues values) {
        int uriType = getUriMatcher().match(uri);
        switch (uriType) {
            case MATCH_ALL:
                SQLiteDatabase db = getDatabase().getWritableDatabase();
                db.execSQL(UPSERT_SQL, UserOptionRow.DATABASE_TABLE.toBindArgs(values));
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        notifyChange(uri, null);
        return uri;
    }
}
