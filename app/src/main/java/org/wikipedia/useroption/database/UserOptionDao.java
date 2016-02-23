package org.wikipedia.useroption.database;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.WikipediaApp;
import org.wikipedia.concurrency.SaneAsyncTask;
import org.wikipedia.database.sync.SyncRowDao;
import org.wikipedia.theme.Theme;
import org.wikipedia.useroption.UserOption;
import org.wikipedia.useroption.sync.UserOptionContentResolver;

import java.util.Collection;

public final class UserOptionDao extends SyncRowDao<UserOptionRow> {
    public interface Callback<T> {
        void success(@Nullable T item);
    }

    private static final String THEME_KEY = "userjs-app-pref-theme";
    private static final String FONT_SIZE_KEY = "userjs-app-pref-font-size";

    private static UserOptionDao INSTANCE = new UserOptionDao();

    public static UserOptionDao instance() {
        return INSTANCE;
    }

    public void theme(@NonNull Theme theme) {
        new UpsertTask(new UserOptionRow(THEME_KEY, String.valueOf(theme.isLight()))).execute();
    }

    public void theme(final Callback<Theme> callback) {
        new QueryTask(THEME_KEY) {
            @Override
            public void onFinish(UserOptionRow result) {
                callback.success(result == null
                        ? null
                        : Boolean.valueOf(result.val()) ? Theme.LIGHT : Theme.DARK);
            }
        }.execute();
    }

    public void fontSize(int size) {
        new UpsertTask(new UserOptionRow(FONT_SIZE_KEY, String.valueOf(size))).execute();
    }

    public void fontSize(final Callback<Integer> callback) {
        new QueryTask(FONT_SIZE_KEY) {
            @Override
            public void onFinish(UserOptionRow result) {
                super.onFinish(result);
                callback.success(result == null ? null : Integer.valueOf(result.val()));
            }
        }.execute();
    }

    @Nullable private UserOptionRow queryItem(@NonNull String key) {
        return queryItem(new UserOptionRow(key));
    }

    public void reconcileOptions(@NonNull Collection<UserOption> options) {
        for (UserOption option : options) {
            reconcile(new UserOptionRow(option));
        }
    }

    private UserOptionDao() {
        super(WikipediaApp.getInstance().getDatabaseClient(UserOptionRow.class));
    }

    // TODO: replace AsyncTasks with SQLBrite.

    private class UpsertTask extends SaneAsyncTask<Void> {
        @NonNull private final UserOptionRow row;

        UpsertTask(@NonNull UserOptionRow row) {
            this.row = row;
        }

        @Override
        public Void performTask() throws Throwable {
            upsert(row);
            return null;
        }

        @Override
        public void onFinish(Void result) {
            super.onFinish(result);
            UserOptionContentResolver.requestManualUpload();
        }
    }

    private class QueryTask extends SaneAsyncTask<UserOptionRow> {
        private final String key;

        QueryTask(String key) {
            this.key = key;
        }

        @Override
        public UserOptionRow performTask() throws Throwable {
            return queryItem(key);
        }
    }
}