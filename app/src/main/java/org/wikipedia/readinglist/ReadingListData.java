package org.wikipedia.readinglist;

import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.wikipedia.WikipediaApp;
import org.wikipedia.concurrency.CallbackTask;
import org.wikipedia.database.DatabaseClient;
import org.wikipedia.database.contract.ReadingListContract;
import org.wikipedia.readinglist.database.ReadingListRow;
import org.wikipedia.readinglist.page.ReadingListPage;
import org.wikipedia.readinglist.page.database.ReadingListPageDao;

import java.util.ArrayList;
import java.util.List;

public final class ReadingListData {
    private static final ReadingListData INSTANCE = new ReadingListData();

    public static ReadingListData instance() {
        return INSTANCE;
    }

    public void queryMruLists(@Nullable final String searchQuery,
                              @NonNull CallbackTask.Callback<List<ReadingList>> callback) {
        CallbackTask.execute(new CallbackTask.Task<List<ReadingList>>() {
            @Override public List<ReadingList> execute() {
                return queryMruLists(searchQuery);
            }
        }, callback);
    }

    public List<ReadingList> queryMruLists(@Nullable String searchQuery) {
        List<ReadingList> rows = new ArrayList<>();
        Cursor cursor = lists(searchQuery);
        try {
            while (cursor.moveToNext()) {
                rows.add(ReadingList.fromCursor(cursor));
            }
        } finally {
            cursor.close();
        }
        return rows;
    }

    @NonNull public Cursor lists(@Nullable String searchQuery) {
        Uri uri = ReadingListContract.ListWithPagesAndDisk.URI;
        String selection = null;
        String[] selectionArgs = null;
        String searchStr = searchQuery;
        if (!TextUtils.isEmpty(searchStr)) {
            String titleCol = ReadingListContract.List.TITLE.qualifiedName();
            searchStr = searchStr.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
            selection = "UPPER(" + titleCol + ") LIKE UPPER(?) ESCAPE '\\'";
            selectionArgs = new String[]{"%" + searchStr + "%"};
        }
        String order = ReadingListContract.ListWithPagesAndDisk.ORDER_KEY + ','
                + ReadingListContract.ListWithPagesAndDisk.ORDER_MRU;
        return listClient().select(uri, selection, selectionArgs, order);
    }

    public void addListAsync(@NonNull final ReadingList list) {
        CallbackTask.execute(new CallbackTask.Task<Void>() {
            @Override public Void execute() {
                addList(list);
                return null;
            }
        });
    }

    public synchronized void addList(@NonNull ReadingList list) {
        listClient().persist(list);
        for (ReadingListPage page : list.getPages()) {
            page.addListKey(list.key());
            ReadingListPageDao.instance().upsert(page);
            ReadingListPageDao.instance().markOutdated(page);
        }
    }

    public void removeListAsync(@NonNull final ReadingList list) {
        CallbackTask.execute(new CallbackTask.Task<Void>() {
            @Override public Void execute() {
                removeList(list);
                return null;
            }
        });
    }

    public void makeListMostRecent(@NonNull final ReadingList list) {
        long now = System.currentTimeMillis();
        list.atime(now);

        CallbackTask.execute(new CallbackTask.Task<Void>() {
            @Override public Void execute() {
                saveListInfo(list);
                return null;
            }
        });
    }

    public void saveListInfoAsync(@NonNull final ReadingList list) {
        CallbackTask.execute(new CallbackTask.Task<Void>() {
            @Override public Void execute() {
                saveListInfo(list);
                return null;
            }
        });
    }

    public void addTitleToList(@NonNull final ReadingList list,
                               @NonNull final ReadingListPage page,
                               boolean undo) {
        list.add(page);
        page.addListKey(list.key());

        if (!undo) {
            setPageOffline(page, true);
        }

        saveListInfo(list, page);
    }

    public void removeTitleFromList(@NonNull final ReadingList list,
                                    @NonNull final ReadingListPage page) {
        list.remove(page);
        page.removeListKey(list.key());
        saveListInfo(list, page);
    }

    public void listContainsTitleAsync(@NonNull final ReadingList list,
                                       @NonNull final ReadingListPage page,
                                       @NonNull CallbackTask.Callback<Boolean> callback) {
        CallbackTask.execute(new CallbackTask.Task<Boolean>() {
            @Override public Boolean execute() {
                return listContainsTitle(list.key(), page.key());
            }
        }, callback);
    }

    public void anyListContainsTitleAsync(@NonNull final String key,
                                          @NonNull CallbackTask.Callback<ReadingListPage> callback) {
        CallbackTask.execute(new CallbackTask.Task<ReadingListPage>() {
            @Override public ReadingListPage execute() {
                return findPageInAnyList(key);
            }
        }, callback);
    }

    public void titlesNotInListAsync(@NonNull final String listKey, @NonNull final List<String> keys,
                                     @NonNull CallbackTask.Callback<List<String>> callback) {
        CallbackTask.execute(new CallbackTask.Task<List<String>>() {
            @Override public List<String> execute() throws Throwable {
                return titlesNotInList(listKey, keys);
            }
        }, callback);
    }

    public synchronized void saveListInfo(@NonNull ReadingList list) {
        listClient().persist(list);
    }

    public synchronized void renameAndSaveListInfo(@NonNull ReadingList list, String title) {
        List<ReadingListPage> pages = list.getPages();
        String oldKey = list.key();
        listClient().delete(list, listClient().getPrimaryKeySelectionArgs(list));
        list.setTitle(title);
        listClient().persist(list);
        for (ReadingListPage page : pages) {
            page.removeListKey(oldKey);
            page.addListKey(list.key());
            ReadingListPageDao.instance().upsert(page);
        }
    }

    public synchronized void setPageOffline(@NonNull ReadingListPage page, boolean offline) {
        page.setOffline(offline);
        ReadingListPageDao.instance().upsert(page);
    }

    public synchronized void removeList(@NonNull ReadingList list) {
        listClient().delete(list, listClient().getPrimaryKeySelectionArgs(list));
        for (ReadingListPage page : list.getPages()) {
            page.removeListKey(list.key());
            ReadingListPageDao.instance().upsert(page);
        }
    }


    private synchronized List<String> titlesNotInList(@NonNull String listKey, @NonNull List<String> keys) {
        Cursor cursor = ReadingListPageDao.instance().pages(keys);
        List<String> result = new ArrayList<>();
        try {
            while (cursor.moveToNext()) {
                ReadingListPage page = ReadingListPage.fromCursor(cursor);
                if (!page.listKeys().contains(listKey)) {
                    result.add(page.key());
                }
            }
            return result;
        } finally {
            cursor.close();
        }
    }


    @Nullable
    public synchronized ReadingListPage findPageInAnyList(String key) {
        Cursor cursor = ReadingListPageDao.instance().page(key);
        try {
            if (cursor.getCount() != 0) {
                cursor.moveToFirst();
                ReadingListPage page = ReadingListPage.fromCursor(cursor);
                // If a page doesn't belong to any lists, it's likely queued for deletion.
                return page.listKeys().size() > 0 ? page : null;
            }
        } finally {
            cursor.close();
        }
        return null;
    }

    private synchronized void saveListInfo(@NonNull ReadingList list, @NonNull ReadingListPage page) {
        listClient().persist(list);
        ReadingListPageDao.instance().upsert(page);
    }

    private synchronized boolean listContainsTitle(@NonNull String listKey, @NonNull String key) {
        Cursor cursor = ReadingListPageDao.instance().page(key);
        try {
            if (cursor.moveToFirst()) {
                ReadingListPage page = ReadingListPage.fromCursor(cursor);
                if (page.listKeys().contains(listKey)) {
                    return true;
                }
            }
            return false;
        } finally {
            cursor.close();
        }
    }

    private DatabaseClient<ReadingListRow> listClient() {
        return client(ReadingListRow.class);
    }

    private <T> DatabaseClient<T> client(Class<T> clazz) {
        return WikipediaApp.getInstance().getDatabaseClient(clazz);
    }
}
