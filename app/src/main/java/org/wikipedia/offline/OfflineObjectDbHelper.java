package org.wikipedia.offline;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteQueryBuilder;

import org.wikipedia.database.AppDatabase;
import org.wikipedia.database.contract.OfflineObjectContract;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.page.PageTitle;
import org.wikipedia.readinglist.database.ReadingListDbHelper;
import org.wikipedia.readinglist.database.ReadingListPage;
import org.wikipedia.util.log.L;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
        SupportSQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.query(SupportSQLiteQueryBuilder.builder(OfflineObjectContract.TABLE)
                .selection(TextUtils.isEmpty(lang) ? OfflineObjectContract.Col.URL.getName() + " = ?"
                                : OfflineObjectContract.Col.URL.getName() + " = ? AND " + OfflineObjectContract.Col.LANG.getName() + " = ?",
                        TextUtils.isEmpty(lang) ? new String[]{url} : new String[]{url, lang})
                .create())) {
            if (cursor.moveToFirst()) {
                return OfflineObjectTable.DATABASE_TABLE.fromCursor(cursor);
            }
        }
        // Couldn't find an exact match, so...
        // If we're trying to load an image from Commons, try to look for any other resolution.
        if (url.contains("/commons/thumb/")) {
            String[] parts = url.split("/");
            if (parts.length > 2) {
                String fileName = parts[parts.length - 2].replaceAll("'", "%27");
                try (Cursor cursor = db.query(SupportSQLiteQueryBuilder.builder(OfflineObjectContract.TABLE)
                        .selection(OfflineObjectContract.Col.URL.getName() + " LIKE '%/' || ? || '/%'",
                                new String[]{fileName})
                        .create())) {
                    if (cursor.moveToFirst()) {
                        return OfflineObjectTable.DATABASE_TABLE.fromCursor(cursor);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    L.logRemoteErrorIfProd(e);
                }
            }
        }
        return null;
    }

    public void addObject(@NonNull String url, @NonNull String lang, @NonNull String path, @NonNull String pageTitle) {
        SupportSQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            // first find this item if it already exists in the db
            OfflineObject obj = null;
            try (Cursor cursor = db.query(SupportSQLiteQueryBuilder.builder(OfflineObjectContract.TABLE)
                    .selection(OfflineObjectContract.Col.URL.getName() + " = ? AND "
                            + OfflineObjectContract.Col.LANG.getName() + " = ?", new String[]{url, lang})
                    .create())) {
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
            ReadingListPage page = ReadingListDbHelper.INSTANCE
                    .findPageInAnyList(new PageTitle(pageTitle, WikiSite.forLanguageCode(lang)));
            if (page != null && !obj.getUsedBy().contains(page.getId())) {
                obj.getUsedBy().add(page.getId());
            }

            if (doInsert) {
                db.insert(OfflineObjectContract.TABLE, SQLiteDatabase.CONFLICT_REPLACE,
                        OfflineObjectTable.DATABASE_TABLE.toContentValues(obj));
            } else {
                if (!path.equals(obj.getPath())) {
                    L.w("Existing offline object path is inconsistent.");
                }
                if (db.update(OfflineObjectContract.TABLE, SQLiteDatabase.CONFLICT_REPLACE,
                        OfflineObjectTable.DATABASE_TABLE.toContentValues(obj),
                        OfflineObjectContract.Col.URL.getName() + " = ? AND "
                                + OfflineObjectContract.Col.LANG.getName() + " = ?",
                        new String[]{url, lang}) != 1) {
                    L.w("Failed to update db entry for object: " + url);
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void deleteObjectsForPageId(long id) {
        SupportSQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            List<OfflineObject> objects = new ArrayList<>();
            try (Cursor cursor = db.query(SupportSQLiteQueryBuilder.builder(OfflineObjectContract.TABLE)
                    .selection(OfflineObjectContract.Col.USEDBY.getName() + " LIKE '%|" + id + "|%'", null)
                    .create())) {
                while (cursor.moveToNext()) {
                    OfflineObject obj = OfflineObjectTable.DATABASE_TABLE.fromCursor(cursor);
                    if (!obj.getUsedBy().contains(id)) {
                        continue;
                    }
                    obj.getUsedBy().remove(id);
                    objects.add(obj);
                }
            }

            for (OfflineObject obj : objects) {
                if (obj.getUsedBy().isEmpty()) {
                    // the object is now an orphan, so remove it!
                    if (db.delete(OfflineObjectContract.TABLE,
                            OfflineObjectContract.Col.URL.getName() + " = ? AND "
                                    + OfflineObjectContract.Col.LANG.getName() + " = ?",
                            new String[]{obj.getUrl(), obj.getLang()}) != 1) {
                        L.w("Failed to delete item from database.");
                    }
                    deleteFilesForObject(obj);
                } else {
                    if (db.update(OfflineObjectContract.TABLE, SQLiteDatabase.CONFLICT_REPLACE,
                            OfflineObjectTable.DATABASE_TABLE.toContentValues(obj),
                            OfflineObjectContract.Col.URL.getName() + " = ? AND "
                                    + OfflineObjectContract.Col.LANG.getName() + " = ?",
                            new String[]{obj.getUrl(), obj.getLang()}) != 1) {
                        L.w("Failed to update item in database.");
                    }
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public long getTotalBytesForPageId(long id) {
        SupportSQLiteDatabase db = getReadableDatabase();
        List<OfflineObject> objects = new ArrayList<>();
        try (Cursor cursor = db.query(SupportSQLiteQueryBuilder.builder(OfflineObjectContract.TABLE)
                .selection(OfflineObjectContract.Col.USEDBY.getName() + " LIKE '%|" + id + "|%'", null)
                .create())) {
            while (cursor.moveToNext()) {
                OfflineObject obj = OfflineObjectTable.DATABASE_TABLE.fromCursor(cursor);
                if (!obj.getUsedBy().contains(id)) {
                    continue;
                }
                objects.add(obj);
            }
        }

        long totalBytes = 0;
        try {
            for (OfflineObject obj : objects) {
                final File contentsFile = new File(obj.getPath() + ".1");
                totalBytes += contentsFile.length();
            }
        } catch (Exception e) {
            L.w(e);
        }
        return totalBytes;
    }

    public static void deleteFilesForObject(@NonNull OfflineObject obj) {
        try {
            File metadataFile = new File(obj.getPath() + ".0");
            File contentsFile = new File(obj.getPath() + ".1");
            metadataFile.delete();
            contentsFile.delete();
        } catch (Exception e) {
            // ignore
        }
    }

    private SupportSQLiteDatabase getReadableDatabase() {
        return AppDatabase.Companion.getAppDatabase().getReadableDatabase();
    }

    private SupportSQLiteDatabase getWritableDatabase() {
        return AppDatabase.Companion.getAppDatabase().getWritableDatabase();
    }
}
