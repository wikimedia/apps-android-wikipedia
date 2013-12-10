package org.wikimedia.wikipedia.history;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import org.wikimedia.wikipedia.WikipediaApp;
import org.wikimedia.wikipedia.data.DBOpenHelper;
import org.wikimedia.wikipedia.data.SQLiteContentProvider;
import org.wikimedia.wikipedia.pageimages.PageImage;

import java.util.HashMap;

public class HistoryEntryContentProvider extends SQLiteContentProvider<HistoryEntry> {
    private static final int MATCH_WITH_PAGEIMAGES =  64;

    public HistoryEntryContentProvider() {
        super(HistoryEntry.persistanceHelper);
    }

    @Override
    public boolean onCreate() {
        boolean ret = super.onCreate();
        uriMatcher.addURI(getAuthority(),
                persistanceHelper.getTableName() + "/" + PageImage.persistanceHelper.getTableName(),
                MATCH_WITH_PAGEIMAGES);
        return ret;
    }

    @Override
    protected DBOpenHelper getDbOpenHelper() {
        return ((WikipediaApp)getContext().getApplicationContext()).getDbOpenHelper();
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        int uriType = uriMatcher.match(uri);

        SQLiteDatabase db = getDbOpenHelper().getReadableDatabase();
        Cursor cursor;

        switch (uriType) {
            case MATCH_WITH_PAGEIMAGES:
                queryBuilder.setTables(
                        String.format("%1$s LEFT OUTER JOIN %2$s ON (%1$s.site = %2$s.site and %1$s.title = %2$s.title)",
                                HistoryEntry.persistanceHelper.getTableName(), PageImage.persistanceHelper.getTableName()
                                )
                );
                cursor = queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            default:
                return super.query(uri, projection, selection, selectionArgs, sortOrder);
        }

        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }
}
