package org.wikipedia.savedpages;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import org.wikipedia.WikipediaApp;
import org.wikipedia.data.DBOpenHelper;
import org.wikipedia.data.SQLiteContentProvider;
import org.wikipedia.pageimages.PageImage;

public class SavedPageContentProvider extends SQLiteContentProvider<SavedPage> {
    private static final int MATCH_WITH_PAGEIMAGES =  64;

    /** column index of pageimages.imageName in the MATCH_WITH_PAGEIMAGES case */
    public static final int COL_INDEX_IMAGE = 4;

    public SavedPageContentProvider() {
        super(SavedPage.PERSISTENCE_HELPER);
    }

    @Override
    public boolean onCreate() {
        boolean ret = super.onCreate();
        getUriMatcher().addURI(getAuthority(),
                          getTableName() + "/" + PageImage.PERSISTENCE_HELPER.getTableName(),
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

        int uriType = getUriMatcher().match(uri);

        SQLiteDatabase db = getDbOpenHelper().getReadableDatabase();
        Cursor cursor;

        switch (uriType) {
            case MATCH_WITH_PAGEIMAGES:
                queryBuilder.setTables(
                        String.format("%1$s LEFT OUTER JOIN %2$s ON (%1$s.site = %2$s.site and %1$s.title = %2$s.title)",
                                SavedPage.PERSISTENCE_HELPER.getTableName(), PageImage.PERSISTENCE_HELPER

                                .getTableName()
                                )
                );
                String[] actualProjection = new String[] {
                        "savedpages._id",
                        "savedpages.site",
                        "savedpages.title",
                        "savedpages.timestamp",
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
