package org.wikipedia.beta.bookmarks;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import org.wikipedia.beta.WikipediaApp;
import org.wikipedia.beta.data.DBOpenHelper;
import org.wikipedia.beta.data.SQLiteContentProvider;
import org.wikipedia.beta.pageimages.PageImage;

public class BookmarkContentProvider extends SQLiteContentProvider<Bookmark> {
    private static final int MATCH_WITH_PAGEIMAGES =  64;

    public BookmarkContentProvider() {
        super(Bookmark.PERSISTANCE_HELPER);
    }

    @Override
    public boolean onCreate() {
        boolean ret = super.onCreate();
        uriMatcher.addURI(getAuthority(),
                persistanceHelper.getTableName() + "/" + PageImage.PERSISTANCE_HELPER.getTableName(),
                MATCH_WITH_PAGEIMAGES);
        return ret;
    }

    @Override
    protected DBOpenHelper getDbOpenHelper() {
        return ((WikipediaApp)getContext().getApplicationContext()).getDbOpenHelper();
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if (projection != null) {
            throw new UnsupportedOperationException("Projection is pre-set, must always be null");
        }
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        int uriType = uriMatcher.match(uri);

        SQLiteDatabase db = getDbOpenHelper().getReadableDatabase();
        Cursor cursor;


        switch (uriType) {
            case MATCH_WITH_PAGEIMAGES:
                queryBuilder.setTables(
                        String.format("%1$s LEFT OUTER JOIN %2$s ON (%1$s.site = %2$s.site and %1$s.title = %2$s.title)",
                                Bookmark.PERSISTANCE_HELPER.getTableName(), PageImage.PERSISTANCE_HELPER.getTableName()
                                )
                );
                String[] actualProjection = new String[] {
                        "bookmarks._id",
                        "bookmarks.site",
                        "bookmarks.title",
                        "bookmarks.timestamp",
                        "pageimages.imageName"
                };
                cursor = queryBuilder.query(db, actualProjection, selection, selectionArgs, null, null, sortOrder);
                break;
            default:
                return super.query(uri, projection, selection, selectionArgs, sortOrder);
        }

        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }
}
