package org.wikipedia.history;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;

import org.wikipedia.database.DbUtil;
import org.wikipedia.database.SQLiteContentProvider;
import org.wikipedia.pageimages.PageImage;
import org.wikipedia.pageimages.PageImageDatabaseTable;
import org.wikipedia.util.StringUtil;

import java.util.Collection;

public class HistoryEntryContentProvider extends SQLiteContentProvider {
    private static final int MATCH_WITH_PAGEIMAGES =  64;

    private static final String SET_TBL_SQL = (
              ":historyTbl left outer join :pageImagesTbl on ("
            + ":historyTbl.:site = :pageImagesTbl.:site "
            + "and :historyTbl.:title = :pageImagesTbl.:title)")
            .replaceAll("(:historyTbl.):site", "$1" + HistoryEntryDatabaseTable.Col.SITE.getName())
            .replaceAll("(:pageImagesTbl.):site", "$1" + PageImageDatabaseTable.Col.SITE.getName())
            .replaceAll("(:historyTbl.):title", "$1" + HistoryEntryDatabaseTable.Col.TITLE.getName())
            .replaceAll("(:pageImagesTbl.):title", "$1" + PageImageDatabaseTable.Col.TITLE.getName())
            .replaceAll(":historyTbl", HistoryEntry.DATABASE_TABLE.getTableName())
            .replaceAll(":pageImagesTbl", PageImage.DATABASE_TABLE.getTableName());
    private static final String[] PROJECTION;
    static {
        Collection<String> historyCols = StringUtil.prefix(HistoryEntry.DATABASE_TABLE.getTableName() + ".",
                DbUtil.names(HistoryEntryDatabaseTable.Col.ALL));
        PROJECTION = historyCols.toArray(new String[historyCols.size() + 1]);
        PROJECTION[historyCols.size()] = PageImage.DATABASE_TABLE.getTableName() + "." + PageImageDatabaseTable.Col.IMAGE_NAME.getName();
    }

    public HistoryEntryContentProvider() {
        super(HistoryEntry.DATABASE_TABLE);
    }

    @Override
    public boolean onCreate() {
        boolean ret = super.onCreate();
        getUriMatcher().addURI(getAuthority(),
                          getTableName() + "/" + PageImage.DATABASE_TABLE.getTableName(),
                          MATCH_WITH_PAGEIMAGES);
        return ret;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if (projection != null) {
            throw new UnsupportedOperationException("Projection is pre-set, must always be null");
        }

        int uriType = getUriMatcher().match(uri);
        switch (uriType) {
            case MATCH_WITH_PAGEIMAGES:
                SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
                queryBuilder.setTables(SET_TBL_SQL);
                SQLiteDatabase db = getDatabase().getReadableDatabase();
                Cursor cursor = queryBuilder.query(db, PROJECTION, selection, selectionArgs, null, null, sortOrder);
                cursor.setNotificationUri(getContentResolver(), uri);
                return cursor;
            default:
                return super.query(uri, projection, selection, selectionArgs, sortOrder);
        }
    }
}