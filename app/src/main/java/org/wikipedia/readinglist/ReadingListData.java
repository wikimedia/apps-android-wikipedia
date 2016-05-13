package org.wikipedia.readinglist;

import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

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

    public void queryMruLists(@NonNull CallbackTask.Callback<List<ReadingList>> callback) {
        CallbackTask.execute(new CallbackTask.Task<List<ReadingList>>() {
            @Override public List<ReadingList> execute() {
                List<ReadingList> rows = new ArrayList<>();
                Cursor cursor = lists();
                try {
                    while (cursor.moveToNext()) {
                        rows.add(ReadingList.fromCursor(cursor));
                    }
                } finally {
                    cursor.close();
                }
                return rows;
            }
        }, callback);
    }

    @NonNull public Cursor lists() {
        Uri uri = ReadingListContract.ListWithPagesAndDisk.URI;
        final String selection = null;
        final String[] selectionArgs = null;
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
                               @NonNull final ReadingListPage page) {
        list.add(page);
        page.addListKey(list.key());
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

    public synchronized void removeList(@NonNull ReadingList list) {
        listClient().delete(list, listClient().getPrimaryKeySelectionArgs(list));
        for (ReadingListPage page : list.getPages()) {
            page.removeListKey(list.key());
            ReadingListPageDao.instance().upsert(page);
        }
    }

    @Nullable
    private synchronized ReadingListPage findPageInAnyList(String key) {
        Cursor cursor = ReadingListPageDao.instance().page(key);
        try {
            if (cursor.getCount() != 0) {
                cursor.moveToFirst();
                return ReadingListPage.fromCursor(cursor);
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
        Cursor cursor = ReadingListPageDao.instance().pages(listKey);
        try {
            while (cursor.moveToNext()) {
                ReadingListPage page = ReadingListPage.fromCursor(cursor);
                if (page.key().equals(key)) {
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