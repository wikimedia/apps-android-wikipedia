package org.wikipedia.readinglist.page.database;

import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import org.wikipedia.WikipediaApp;
import org.wikipedia.concurrency.CallbackTask;
import org.wikipedia.concurrency.CallbackTask.Task;
import org.wikipedia.database.BaseDao;
import org.wikipedia.database.async.AsyncConstant;
import org.wikipedia.database.contract.ReadingListPageContract;
import org.wikipedia.database.http.HttpRowDao;
import org.wikipedia.page.PageTitle;
import org.wikipedia.readinglist.page.ReadingListPage;
import org.wikipedia.readinglist.page.ReadingListPageRow;
import org.wikipedia.readinglist.page.database.disk.DiskRowDao;
import org.wikipedia.readinglist.page.database.disk.DiskStatus;
import org.wikipedia.readinglist.page.database.disk.ReadingListPageDiskRow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class ReadingListPageDao extends BaseDao<ReadingListPageRow> {

    private static final ReadingListPageDao INSTANCE = new ReadingListPageDao();

    @NonNull private final HttpRowDao<ReadingListPageRow, ReadingListPageHttpRow> httpDao;
    @NonNull private final DiskRowDao<ReadingListPageRow, ReadingListPageDiskRow> diskDao;

    public static ReadingListPageDao instance() {
        return INSTANCE;
    }

    @NonNull public Cursor page(@NonNull String key) {
        return pages(Collections.singletonList(key));
    }

    @NonNull public Cursor pages(@NonNull List<String> keys) {
        Uri uri = ReadingListPageContract.PageWithDisk.URI;
        String selection = Sql.getSelectRowsWithKeysString(keys.size());
        String[] selectionArgs = keys.toArray(new String[keys.size()]);
        String order = ReadingListPageContract.PageWithDisk.ORDER_ALPHABETICAL;
        return client().select(uri, selection, selectionArgs, order);
    }

    @NonNull public Cursor pages(@NonNull String listKey) {
        Uri uri = ReadingListPageContract.PageWithDisk.URI;
        String selection = Sql.SELECT_ROWS_WITH_LIST_KEY;
        String[] selectionArgs = new String[] {listKey};
        String order = ReadingListPageContract.PageWithDisk.ORDER_MRU;
        return client().select(uri, selection, selectionArgs, order);
    }

    @NonNull public Cursor allPages() {
        return client().select(ReadingListPageContract.PageWithDisk.URI, null, null, null);
    }

    public void randomPage(@NonNull CallbackTask.Callback<PageTitle> callback) {
        CallbackTask.execute(new CallbackTask.Task<PageTitle>() {
            @Override public PageTitle execute() {
                Cursor c = client().select(ReadingListPageContract.PageWithDisk.URI, null, null, null);
                PageTitle title = null;
                try {
                    if (c.getCount() > 0) {
                        c.moveToPosition(new Random().nextInt(c.getCount()));
                        title = ReadingListDaoProxy.pageTitle(ReadingListPage.fromCursor(c));
                    }
                } finally {
                    c.close();
                }
                return title;
            }
        }, callback);
    }

    public void deletePageFromLists(@NonNull ReadingListPage page,
                                    @NonNull Collection<String> listKeys) {
        for (String key : listKeys) {
            page.removeListKey(key);
        }
        upsert(page);
    }

    public void upsertAsync(@NonNull final ReadingListPage row) {
        CallbackTask.execute(new Task<Void>() {
            @Override public Void execute() {
                upsert(row);
                return null;
            }
        });
    }

    public synchronized void deleteIfOrphaned(@NonNull ReadingListPageRow row) {
        if (row.listKeys().isEmpty()) {
            delete(row);
        }
    }

    public synchronized void upsert(@NonNull ReadingListPage row) {
        if (row.listKeys().isEmpty()) {
            httpDao.markDeleted(new ReadingListPageHttpRow(row));
            diskDao.markDeleted(new ReadingListPageDiskRow(row));
        } else {
            httpDao.markUpserted(new ReadingListPageHttpRow(row));
            if (row.diskStatus() == DiskStatus.OUTDATED) {
                diskDao.markOutdated(new ReadingListPageDiskRow(row));
            } else if (row.diskStatus() == DiskStatus.ONLINE || row.diskStatus() == DiskStatus.UNSAVED) {
                diskDao.markOnline(new ReadingListPageDiskRow(row));
            }
        }
        super.upsert(row);
    }

    public synchronized void markOutdated(@NonNull ReadingListPage row) {
        markOutdated(new ReadingListPageDiskRow(row));
    }

    public synchronized void markOutdated(@NonNull ReadingListPageDiskRow row) {
        diskDao.markOutdated(row);
    }

    @NonNull public synchronized Collection<ReadingListPageDiskRow> startDiskTransaction() {
        Collection<ReadingListPageDiskRow> rows = queryPendingDiskTransactions();
        diskDao.startTransaction(rows);
        return rows;
    }

    public synchronized void completeDiskTransaction(@NonNull ReadingListPageDiskRow row) {
        diskDao.completeTransaction(row);

        if (row.dat() != null) {
            super.upsert(row.dat());
        }
    }

    public synchronized void failDiskTransaction(@NonNull ReadingListPageDiskRow row) {
        diskDao.failTransaction(row);
    }

    public void clearAsync() {
        CallbackTask.execute(new Task<Void>() {
            @Override public Void execute() {
                clear();
                return null;
            }
        });
    }

    @Override public synchronized void clear() {
        httpDao.clear();
        diskDao.clear();
        super.clear();
    }

    @NonNull private Collection<ReadingListPageDiskRow> queryPendingDiskTransactions() {
        Uri uri = ReadingListPageContract.DiskWithPage.URI;
        String selection = Sql.SELECT_ROWS_PENDING_DISK_TRANSACTION;
        final String[] selectionArgs = null;
        final String order = null;
        Cursor cursor = client().select(uri, selection,
                selectionArgs, order);

        Collection<ReadingListPageDiskRow> rows = new ArrayList<>();
        try {
            while (cursor.moveToNext()) {
                rows.add(ReadingListPageDiskRow.fromCursor(cursor));
            }
        } finally {
            cursor.close();
        }
        return rows;
    }

    // TODO: expose HTTP DAO methods.

    private ReadingListPageDao() {
        super(WikipediaApp.getInstance().getDatabaseClient(ReadingListPageRow.class));
        httpDao = new HttpRowDao<>(WikipediaApp.getInstance().getDatabaseClient(ReadingListPageHttpRow.class));
        diskDao = new DiskRowDao<>(WikipediaApp.getInstance().getDatabaseClient(ReadingListPageDiskRow.class));
    }

    @VisibleForTesting static class Sql {
        @VisibleForTesting static String getSelectRowsWithKeysString(int params) {
            if (params < 0) {
                throw new IllegalArgumentException();
            }
            if (params < 2) {
                return ":keyCol == ?".replaceAll(":keyCol", ReadingListPageContract.Page.KEY.qualifiedName());
            }
            StringBuilder result = new StringBuilder(":keyCol IN (?");
            for (int i = 2; i <= params; i++) {
                result.append(",?");
            }
            result.append(")");
            return result.toString().replaceAll(":keyCol", ReadingListPageContract.Page.KEY.qualifiedName());
        }

        private static final String SELECT_ROWS_WITH_LIST_KEY = "',' || :listKeyCol || ',' like '%,' || ? || ',%'"
            .replaceAll(":listKeyCol", ReadingListPageContract.Page.LIST_KEYS.qualifiedName());

        private static String SELECT_ROWS_PENDING_DISK_TRANSACTION = ":transactionIdCol == :noTransactionId"
            .replaceAll(":transactionIdCol", ReadingListPageContract.DiskWithPage.DISK_TRANSACTION_ID.qualifiedName())
            .replaceAll(":noTransactionId", String.valueOf(AsyncConstant.NO_TRANSACTION_ID));
    }
}
