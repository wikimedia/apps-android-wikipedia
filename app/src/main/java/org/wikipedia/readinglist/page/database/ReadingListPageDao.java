package org.wikipedia.readinglist.page.database;

import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.WikipediaApp;
import org.wikipedia.concurrency.CallbackTask;
import org.wikipedia.concurrency.CallbackTask.Task;
import org.wikipedia.database.BaseDao;
import org.wikipedia.database.async.AsyncConstant;
import org.wikipedia.database.contract.ReadingListPageContract;
import org.wikipedia.database.http.HttpRowDao;
import org.wikipedia.readinglist.page.ReadingListPage;
import org.wikipedia.readinglist.page.ReadingListPageRow;
import org.wikipedia.readinglist.page.database.disk.DiskRowDao;
import org.wikipedia.readinglist.page.database.disk.DiskStatus;
import org.wikipedia.readinglist.page.database.disk.ReadingListPageDiskRow;

import java.util.ArrayList;
import java.util.Collection;

public final class ReadingListPageDao extends BaseDao<ReadingListPageRow> {

    private static final ReadingListPageDao INSTANCE = new ReadingListPageDao();

    @NonNull private final HttpRowDao<ReadingListPageRow, ReadingListPageHttpRow> httpDao;
    @NonNull private final DiskRowDao<ReadingListPageRow, ReadingListPageDiskRow> diskDao;

    public static ReadingListPageDao instance() {
        return INSTANCE;
    }

    @NonNull public Cursor page(@NonNull String key) {
        Uri uri = ReadingListPageContract.PageWithDisk.URI;
        String selection = Sql.SELECT_ROWS_WITH_KEY;
        String[] selectionArgs = new String[] {key};
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

    @Nullable public ReadingListPage findPage(@NonNull String key) {
        Cursor cursor = ReadingListPageDao.instance().page(key);
        try {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                return ReadingListPage.fromCursor(cursor);
            }
        } finally {
            cursor.close();
        }
        return null;
    }

    public void upsertAsync(@NonNull final ReadingListPage row) {
        CallbackTask.execute(new Task<Void>() {
            @Override public Void execute() {
                upsert(row);
                return null;
            }
        });
    }

    public synchronized void upsert(@NonNull ReadingListPage row) {
        if (row.listKeys().isEmpty()) {
            httpDao.markDeleted(new ReadingListPageHttpRow(row));
            diskDao.markDeleted(new ReadingListPageDiskRow(row));
            delete(row);
        } else {
            httpDao.markUpserted(new ReadingListPageHttpRow(row));
            if (row.diskStatus() == DiskStatus.OUTDATED) {
                diskDao.markOutdated(new ReadingListPageDiskRow(row));
            } else if (row.diskStatus() == DiskStatus.ONLINE || row.diskStatus() == DiskStatus.UNSAVED) {
                diskDao.markOnline(new ReadingListPageDiskRow(row));
            }
            super.upsert(row);
        }
    }

    public synchronized void markOutdated(@NonNull ReadingListPage row) {
        diskDao.markOutdated(new ReadingListPageDiskRow(row));
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

    public synchronized void failDiskTransaction(@NonNull Collection<ReadingListPageDiskRow> rows) {
        diskDao.failTransaction(rows);
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

    private static class Sql {
        private static final String SELECT_ROWS_WITH_KEY = ":keyCol == ?"
            .replaceAll(":keyCol", ReadingListPageContract.Page.KEY.qualifiedName());

        private static final String SELECT_ROWS_WITH_LIST_KEY = "',' || :listKeyCol || ',' like '%,' || ? || ',%'"
             .replaceAll(":listKeyCol", ReadingListPageContract.Page.LIST_KEYS.qualifiedName());

        private static String SELECT_ROWS_PENDING_DISK_TRANSACTION = ":transactionIdCol == :noTransactionId"
            .replaceAll(":transactionIdCol", ReadingListPageContract.DiskWithPage.DISK_TRANSACTION_ID.qualifiedName())
            .replaceAll(":noTransactionId", String.valueOf(AsyncConstant.NO_TRANSACTION_ID));
    }
}
