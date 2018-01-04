package org.wikipedia.readinglist.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.WikipediaApp;
import org.wikipedia.database.contract.ReadingListContract;
import org.wikipedia.database.contract.ReadingListPageContract;
import org.wikipedia.page.PageTitle;
import org.wikipedia.savedpages.SavedPageSyncService;
import org.wikipedia.util.log.L;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ReadingListDbHelper {
    private static ReadingListDbHelper INSTANCE;

    public static ReadingListDbHelper instance() {
        if (INSTANCE == null) {
            INSTANCE = new ReadingListDbHelper();
        }
        return INSTANCE;
    }

    public List<ReadingList> getAllLists() {
        List<ReadingList> lists = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.query(ReadingListContract.TABLE, null, null, null, null, null, null)) {
            while (cursor.moveToNext()) {
                ReadingList list = ReadingList.DATABASE_TABLE.fromCursor(cursor);
                lists.add(list);
            }
        }
        for (ReadingList list : lists) {
            populateListPages(db, list);
        }
        return lists;
    }

    public List<ReadingList> getAllListsWithoutContents() {
        List<ReadingList> lists = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.query(ReadingListContract.TABLE, null, null, null, null, null, null)) {
            while (cursor.moveToNext()) {
                ReadingList list = ReadingList.DATABASE_TABLE.fromCursor(cursor);
                lists.add(list);
            }
        }
        return lists;
    }

    @NonNull
    public ReadingList createList(@NonNull String title, @Nullable String description) {
        SQLiteDatabase db = getWritableDatabase();
        return createList(db, title, description);
    }

    @NonNull
    public ReadingList createList(@NonNull SQLiteDatabase db, @NonNull String title, @Nullable String description) {
        db.beginTransaction();
        try {
            ReadingList protoList = new ReadingList(title, description);
            long id = db.insertOrThrow(ReadingListContract.TABLE, null,
                    ReadingList.DATABASE_TABLE.toContentValues(protoList));
            db.setTransactionSuccessful();
            protoList.id(id);
            return protoList;
        } finally {
            db.endTransaction();
        }
    }

    public void updateList(@NonNull ReadingList list) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            // implicitly update the last-access time of the list
            list.touch();
            int result = db.update(ReadingListContract.TABLE, ReadingList.DATABASE_TABLE.toContentValues(list),
                    ReadingListContract.Col.ID.getName() + " = ?", new String[]{Long.toString(list.id())});
            if (result != 1) {
                L.w("Failed to update db entry for list " + list.title());
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void deleteList(@NonNull ReadingList list) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            int result = db.delete(ReadingListContract.TABLE,
                    ReadingListContract.Col.ID.getName() + " = ?", new String[]{Long.toString(list.id())});
            if (result != 1) {
                L.w("Failed to delete db entry for list " + list.title());
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void addPageToList(@NonNull ReadingList list, @NonNull PageTitle title) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            addPageToList(db, list, title);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        SavedPageSyncService.enqueue();
    }

    public void addPagesToList(@NonNull ReadingList list, @NonNull List<ReadingListPage> pages) {
        SQLiteDatabase db = getWritableDatabase();
        addPagesToList(db, list, pages);
    }

    public void addPagesToList(@NonNull SQLiteDatabase db, @NonNull ReadingList list, @NonNull List<ReadingListPage> pages) {
        db.beginTransaction();
        try {
            for (ReadingListPage page : pages) {
                insertPageInDb(db, list, page);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        SavedPageSyncService.enqueue();
    }

    public int addPagesToListIfNotExist(@NonNull ReadingList list, @NonNull List<PageTitle> titles) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        int numAdded = 0;
        try {
            for (PageTitle title : titles) {
                if (pageExistsInList(db, list, title)) {
                    continue;
                }
                addPageToList(db, list, title);
                numAdded++;
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        SavedPageSyncService.enqueue();
        return numAdded;
    }

    public void markPageForDeletion(@NonNull ReadingListPage page) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            page.status(ReadingListPage.STATUS_QUEUE_FOR_DELETE);
            updatePageInDb(db, page);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        SavedPageSyncService.enqueue();
    }

    public void markPagesForDeletion(@NonNull List<ReadingListPage> pages) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            for (ReadingListPage page : pages) {
                page.status(ReadingListPage.STATUS_QUEUE_FOR_DELETE);
                updatePageInDb(db, page);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        SavedPageSyncService.enqueue();
    }

    public void markPageForOffline(@NonNull ReadingListPage page, boolean offline) {
        if (page.offline() == offline) {
            return;
        }
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            page.offline(offline);
            updatePageInDb(db, page);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        SavedPageSyncService.enqueue();
    }

    public void markPagesForOffline(@NonNull List<ReadingListPage> pages, boolean offline) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            for (ReadingListPage page : pages) {
                if (page.offline() == offline) {
                    continue;
                }
                page.offline(offline);
                updatePageInDb(db, page);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        SavedPageSyncService.enqueue();
    }

    public void updatePage(@NonNull ReadingListPage page) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            updatePageInDb(db, page);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private void insertPageInDb(SQLiteDatabase db, @NonNull ReadingList list, @NonNull ReadingListPage page) {
        page.listId(list.id());
        long id = db.insertOrThrow(ReadingListPageContract.TABLE, null,
                ReadingListPage.DATABASE_TABLE.toContentValues(page));
        page.id(id);
    }

    private void updatePageInDb(SQLiteDatabase db, @NonNull ReadingListPage page) {
        int result = db.update(ReadingListPageContract.TABLE, ReadingListPage.DATABASE_TABLE.toContentValues(page),
                ReadingListPageContract.Col.ID.getName() + " = ?", new String[]{Long.toString(page.id())});
        if (result != 1) {
            L.w("Failed to update db entry for page " + page.title());
        }
    }

    private void deletePageFromDb(SQLiteDatabase db, @NonNull ReadingListPage page) {
        int result = db.delete(ReadingListPageContract.TABLE,
                ReadingListPageContract.Col.ID.getName() + " = ?", new String[]{Long.toString(page.id())});
        if (result != 1) {
            L.w("Failed to delete db entry for page " + page.title());
        }
    }

    @Nullable
    public ReadingListPage getRandomPage() {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.query(ReadingListPageContract.TABLE, null,
                        ReadingListPageContract.Col.STATUS.getName() + " != ?",
                new String[]{Integer.toString(ReadingListPage.STATUS_QUEUE_FOR_DELETE)},
                null, null, null)) {
            if (cursor.moveToFirst()) {
                cursor.move(new Random().nextInt(cursor.getCount()));
                return ReadingListPage.DATABASE_TABLE.fromCursor(cursor);
            }
        }
        return null;
    }

    @Nullable
    public ReadingListPage findPageInAnyList(@NonNull PageTitle title) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.query(ReadingListPageContract.TABLE, null,
                ReadingListPageContract.Col.SITE.getName() + " = ? AND "
                        + ReadingListPageContract.Col.LANG.getName() + " = ? AND "
                        + ReadingListPageContract.Col.NAMESPACE.getName() + " = ? AND "
                        + ReadingListPageContract.Col.TITLE.getName() + " = ? AND "
                        + ReadingListPageContract.Col.STATUS.getName() + " != ?",
                new String[]{title.getWikiSite().authority(), title.getWikiSite().languageCode(),
                        Integer.toString(title.namespace().code()), title.getDisplayText(),
                        Integer.toString(ReadingListPage.STATUS_QUEUE_FOR_DELETE)},
                null, null, null)) {
            if (cursor.moveToFirst()) {
                return ReadingListPage.DATABASE_TABLE.fromCursor(cursor);
            }
        }
        return null;
    }

    public boolean pageExistsInList(@NonNull ReadingList list, @NonNull PageTitle title) {
        SQLiteDatabase db = getReadableDatabase();
        return pageExistsInList(db, list, title);
    }

    @NonNull
    public List<ReadingListPage> getAllPageOccurrences(@NonNull PageTitle title) {
        List<ReadingListPage> pages = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.query(ReadingListPageContract.TABLE, null,
                ReadingListPageContract.Col.SITE.getName() + " = ? AND "
                        + ReadingListPageContract.Col.LANG.getName() + " = ? AND "
                        + ReadingListPageContract.Col.NAMESPACE.getName() + " = ? AND "
                        + ReadingListPageContract.Col.TITLE.getName() + " = ? AND "
                        + ReadingListPageContract.Col.STATUS.getName() + " != ?",
                new String[]{title.getWikiSite().authority(), title.getWikiSite().languageCode(),
                        Integer.toString(title.namespace().code()), title.getDisplayText(),
                        Integer.toString(ReadingListPage.STATUS_QUEUE_FOR_DELETE)},
                null, null, null)) {
            while (cursor.moveToNext()) {
                pages.add(ReadingListPage.DATABASE_TABLE.fromCursor(cursor));
            }
        }
        return pages;
    }

    @NonNull
    public List<ReadingList> getListsFromPageOccurrences(@NonNull List<ReadingListPage> pages) {
        List<ReadingList> lists = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        List<Long> listIds = new ArrayList<>();
        for (ReadingListPage page : pages) {
            if (!listIds.contains(page.listId())) {
                listIds.add(page.listId());
            }
        }
        for (long listId : listIds) {
            try (Cursor cursor = db.query(ReadingListContract.TABLE, null,
                    ReadingListContract.Col.ID.getName() + " = ?", new String[]{Long.toString(listId)},
                    null, null, null)) {
                if (cursor.moveToFirst()) {
                    lists.add(ReadingList.DATABASE_TABLE.fromCursor(cursor));
                }
            }
        }
        for (ReadingList list : lists) {
            for (ReadingListPage page : pages) {
                if (list.id() == page.listId()) {
                    list.pages().add(page);
                }
            }
        }
        return lists;
    }

    @NonNull
    public List<ReadingListPage> getAllPagesToBeSaved() {
        List<ReadingListPage> pages = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.query(ReadingListPageContract.TABLE, null,
                ReadingListPageContract.Col.STATUS.getName() + " = ? AND "
                + ReadingListPageContract.Col.OFFLINE.getName() + " = ?",
                new String[]{Integer.toString(ReadingListPage.STATUS_QUEUE_FOR_SAVE), Integer.toString(1)},
                null, null, null)) {
            while (cursor.moveToNext()) {
                pages.add(ReadingListPage.DATABASE_TABLE.fromCursor(cursor));
            }
        }
        return pages;
    }

    public List<ReadingListPage> getAllPagesToBeUnsaved() {
        List<ReadingListPage> pages = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.query(ReadingListPageContract.TABLE, null,
                ReadingListPageContract.Col.STATUS.getName() + " = ? AND "
                + ReadingListPageContract.Col.OFFLINE.getName() + " = ?",
                new String[]{Integer.toString(ReadingListPage.STATUS_SAVED), Integer.toString(0)},
                null, null, null)) {
            while (cursor.moveToNext()) {
                pages.add(ReadingListPage.DATABASE_TABLE.fromCursor(cursor));
            }
        }
        return pages;
    }

    @NonNull
    public List<ReadingListPage> getAllPagesToBeDeleted() {
        List<ReadingListPage> pages = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.query(ReadingListPageContract.TABLE, null,
                ReadingListPageContract.Col.STATUS.getName() + " = ?",
                new String[]{Integer.toString(ReadingListPage.STATUS_QUEUE_FOR_DELETE)},
                null, null, null)) {
            while (cursor.moveToNext()) {
                pages.add(ReadingListPage.DATABASE_TABLE.fromCursor(cursor));
            }
        }
        return pages;
    }

    public void resetUnsavedPageStatus() {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues contentValues = new ContentValues();
            contentValues.put(ReadingListPageContract.Col.STATUS.getName(), ReadingListPage.STATUS_QUEUE_FOR_SAVE);
            int result = db.update(ReadingListPageContract.TABLE, contentValues,
                    ReadingListPageContract.Col.STATUS.getName() + " = ? AND "
                            + ReadingListPageContract.Col.OFFLINE.getName() + " = ?",
                    new String[]{Integer.toString(ReadingListPage.STATUS_SAVED), Integer.toString(0)});
            L.d("Updated " + result + " pages in db.");
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void purgeDeletedPages() {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            int result = db.delete(ReadingListPageContract.TABLE,
                    ReadingListPageContract.Col.STATUS.getName() + " = ?",
                    new String[]{Integer.toString(ReadingListPage.STATUS_QUEUE_FOR_DELETE)});
            L.d("Deleted " + result + " pages from db.");
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Nullable
    public ReadingList getFullListById(long id) {
        SQLiteDatabase db = getReadableDatabase();
        ReadingList list = null;
        try (Cursor cursor = db.query(ReadingListContract.TABLE, null,
                ReadingListContract.Col.ID.getName() + " = ?", new String[]{Long.toString(id)},
                null, null, null)) {
            if (cursor.moveToFirst()) {
                list = ReadingList.DATABASE_TABLE.fromCursor(cursor);
            }
        }
        if (list == null) {
            return null;
        }
        populateListPages(db, list);
        return list;
    }

    private void populateListPages(SQLiteDatabase db, @NonNull ReadingList list) {
        try (Cursor cursor = db.query(ReadingListPageContract.TABLE, null,
                ReadingListPageContract.Col.LISTID.getName() + " = ? AND "
                + ReadingListPageContract.Col.STATUS.getName() + " != ?",
                new String[]{Long.toString(list.id()), Integer.toString(ReadingListPage.STATUS_QUEUE_FOR_DELETE)},
                null, null, null)) {
            while (cursor.moveToNext()) {
                list.pages().add(ReadingListPage.DATABASE_TABLE.fromCursor(cursor));
            }
        }
    }

    private boolean pageExistsInList(SQLiteDatabase db, @NonNull ReadingList list, @NonNull PageTitle title) {
        try (Cursor cursor = db.query(ReadingListPageContract.TABLE, null,
                ReadingListPageContract.Col.SITE.getName() + " = ? AND "
                        + ReadingListPageContract.Col.LANG.getName() + " = ? AND "
                        + ReadingListPageContract.Col.NAMESPACE.getName() + " = ? AND "
                        + ReadingListPageContract.Col.TITLE.getName() + " = ? AND "
                        + ReadingListPageContract.Col.LISTID.getName() + " = ? AND "
                        + ReadingListPageContract.Col.STATUS.getName() + " != ?",
                new String[]{title.getWikiSite().authority(), title.getWikiSite().languageCode(),
                        Integer.toString(title.namespace().code()), title.getDisplayText(),
                        Long.toString(list.id()),
                        Integer.toString(ReadingListPage.STATUS_QUEUE_FOR_DELETE)},
                null, null, null)) {
            if (cursor.getCount() > 0) {
                return true;
            }
        }
        return false;
    }

    private void addPageToList(SQLiteDatabase db, @NonNull ReadingList list, @NonNull PageTitle title) {
        ReadingListPage protoPage = new ReadingListPage(title);
        insertPageInDb(db, list, protoPage);
    }


    private SQLiteDatabase getReadableDatabase() {
        return WikipediaApp.getInstance().getDatabase().getReadableDatabase();
    }

    private SQLiteDatabase getWritableDatabase() {
        return WikipediaApp.getInstance().getDatabase().getWritableDatabase();
    }
}
