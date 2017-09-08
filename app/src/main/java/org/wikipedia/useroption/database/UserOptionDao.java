package org.wikipedia.useroption.database;

import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.WikipediaApp;
import org.wikipedia.concurrency.SaneAsyncTask;
import org.wikipedia.database.BaseDao;
import org.wikipedia.database.async.AsyncConstant;
import org.wikipedia.database.contract.UserOptionContract;
import org.wikipedia.database.http.HttpRowDao;
import org.wikipedia.useroption.UserOption;
import org.wikipedia.useroption.sync.UserOptionContentResolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class UserOptionDao extends BaseDao<UserOption> {
    public interface Callback<T> {
        void success(@Nullable T item);
    }

    @NonNull private static final List<String> SYNCED_OPTIONS = Collections.emptyList();

    @NonNull private static UserOptionDao INSTANCE = new UserOptionDao();

    @NonNull private final HttpRowDao<UserOption, UserOptionRow> httpDao;

    public static UserOptionDao instance() {
        return INSTANCE;
    }

    public synchronized void reconcileTransaction(@NonNull Collection<UserOption> rows) {
        for (UserOption row : rows) {
            if (SYNCED_OPTIONS.contains(row.key())) {
                httpDao.completeTransaction(new UserOptionRow(row));
                upsert(row);
            }
        }

        // TODO: the user option sync adapter downloads all options from the service and should
        //       delete rows deleted and synchronized rows present in the database but not in the
        //       service. Alternatively, delete anything older than the service's oldest timestamp.
    }

    @NonNull public synchronized Collection<UserOptionRow> startTransaction() {
        Collection<UserOptionRow> rows = queryPendingTransactions();
        httpDao.startTransaction(rows);
        return rows;
    }

    public synchronized void completeTransaction(@NonNull UserOptionRow row) {
        httpDao.completeTransaction(row);
    }

    public synchronized void failTransaction(@NonNull Collection<UserOptionRow> rows) {
        httpDao.failTransaction(rows);
    }

    @Override
    public synchronized void clear() {
        httpDao.clear();
        super.clear();
    }

    private synchronized void markUpserted(@NonNull UserOption row) {
        httpDao.markUpserted(new UserOptionRow(row));
        upsert(row);
    }

    @NonNull private Collection<UserOptionRow> queryPendingTransactions() {
        String selection = Sql.SELECT_ROWS_PENDING_TRANSACTION;
        final String[] selectionArgs = null;
        final String order = null;
        Cursor cursor = client().select(UserOptionContract.HttpWithOption.URI, selection,
                selectionArgs, order);

        Collection<UserOptionRow> rows = new ArrayList<>();
        try {
            while (cursor.moveToNext()) {
                rows.add(UserOptionRow.fromCursor(cursor));
            }
        } finally {
            cursor.close();
        }
        return rows;
    }

    private UserOptionDao() {
        super(WikipediaApp.getInstance().getDatabaseClient(UserOption.class));
        httpDao = new HttpRowDao<>(WikipediaApp.getInstance().getDatabaseClient(UserOptionRow.class));
    }

    private static class Sql {
        private static String SELECT_ROWS_PENDING_TRANSACTION =
            ":transactionIdCol == :noTransactionId"
            .replaceAll(":transactionIdCol", UserOptionContract.HttpWithOption.HTTP_TRANSACTION_ID.qualifiedName())
            .replaceAll(":noTransactionId", String.valueOf(AsyncConstant.NO_TRANSACTION_ID));
    }

    // TODO: replace AsyncTasks with SQLBrite.

    private class UpsertTask extends SaneAsyncTask<Void> {
        @NonNull private final UserOption row;

        UpsertTask(@NonNull UserOption row) {
            this.row = row;
        }

        @Override
        public Void performTask() throws Throwable {
            markUpserted(row);
            return null;
        }

        @Override
        public void onFinish(Void result) {
            super.onFinish(result);
            UserOptionContentResolver.requestManualUpload();
        }
    }

    private class QueryTask extends SaneAsyncTask<UserOption> {
        private final String key;

        QueryTask(String key) {
            this.key = key;
        }

        @Override
        public UserOption performTask() throws Throwable {
            return queryPrimaryKey(new UserOption(key));
        }
    }
}
