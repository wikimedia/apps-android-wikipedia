package org.wikipedia.offline;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wikipedia.WikipediaApp;
import org.wikipedia.database.contract.OfflineObjectContract;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.page.PageTitle;
import org.wikipedia.readinglist.database.ReadingListDbHelper;
import org.wikipedia.readinglist.database.ReadingListPage;
import org.wikipedia.util.log.L;

public class OfflineObjectDbHelper {
    public static final String OFFLINE_PATH = "offline_files";
    private static OfflineObjectDbHelper INSTANCE;

    public static OfflineObjectDbHelper instance() {
        if (INSTANCE == null) {
            INSTANCE = new OfflineObjectDbHelper();
        }
        return INSTANCE;
    }

    @Nullable public OfflineObject findObject(@NonNull String url, @Nullable String lang) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.query(OfflineObjectContract.TABLE, null,
                TextUtils.isEmpty(lang) ? OfflineObjectContract.Col.URL.getName() + " = ?"
                        : OfflineObjectContract.Col.URL.getName() + " = ? AND " + OfflineObjectContract.Col.LANG.getName() + " = ?",
                TextUtils.isEmpty(lang) ? new String[]{url} : new String[]{url, lang},
                null, null, null)) {
            if (cursor.moveToFirst()) {
                return OfflineObjectTable.DATABASE_TABLE.fromCursor(cursor);
            }
        }
        // Couldn't find an exact match, so...
        // If we're trying to load an image from Commons, try to look for any other resolution.
        if (url.contains("/commons/")) {
            String[] parts = url.split("/");
            if (parts.length > 2) {
                String fileName = parts[parts.length - 2].replaceAll("%", "~%").replaceAll("_", "~_");
                try (Cursor cursor = db.query(OfflineObjectContract.TABLE, null,
                        OfflineObjectContract.Col.URL.getName() + " LIKE '%/" + fileName + "/%' ESCAPE '~'",
                        null, null, null, null)) {
                    if (cursor.moveToFirst()) {
                        return OfflineObjectTable.DATABASE_TABLE.fromCursor(cursor);
                    }
                }
            }
        }
        return null;
    }

    public void addObject(@NonNull String url, @NonNull String lang, @NonNull String path, @NonNull String pageTitle) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            // first find this item if it already exists in the db
            OfflineObject obj = null;
            try (Cursor cursor = db.query(OfflineObjectContract.TABLE, null,
                    OfflineObjectContract.Col.URL.getName() + " = ? AND "
                            + OfflineObjectContract.Col.LANG.getName() + " = ?",
                    new String[]{url, lang},
                    null, null, null)) {
                if (cursor.moveToFirst()) {
                    obj = OfflineObjectTable.DATABASE_TABLE.fromCursor(cursor);
                }
            }
            boolean doInsert = false;
            if (obj == null) {
                obj = new OfflineObject(url, lang, path, 0);
                doInsert = true;
            }

            // try to find the associated title in a reading list, and add its id to the usedBy list.
            ReadingListPage page = ReadingListDbHelper.instance()
                    .findPageInAnyList(new PageTitle(pageTitle, WikiSite.forLanguageCode(lang)));
            if (page != null && !obj.getUsedBy().contains(page.id())) {
                obj.getUsedBy().add(page.id());
            }

            if (doInsert) {
                db.insertOrThrow(OfflineObjectContract.TABLE, null,
                        OfflineObjectTable.DATABASE_TABLE.toContentValues(obj));
            } else {
                if (!path.equals(obj.getPath())) {
                    L.w("Existing offline object path is inconsistent.");
                }
                int result = db.update(OfflineObjectContract.TABLE, OfflineObjectTable.DATABASE_TABLE.toContentValues(obj),
                        OfflineObjectContract.Col.URL.getName() + " = ? AND "
                                + OfflineObjectContract.Col.LANG.getName() + " = ?",
                        new String[]{url, lang});
                if (result != 1) {
                    L.w("Failed to update db entry for object: " + url);
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }



    private SQLiteDatabase getReadableDatabase() {
        return WikipediaApp.getInstance().getDatabase().getReadableDatabase();
    }

    private SQLiteDatabase getWritableDatabase() {
        return WikipediaApp.getInstance().getDatabase().getWritableDatabase();
    }
}
