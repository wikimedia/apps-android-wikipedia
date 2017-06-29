package org.wikipedia.useroption.sync;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.support.annotation.NonNull;

import org.wikipedia.auth.AccountUtil;
import org.wikipedia.database.http.HttpStatus;
import org.wikipedia.useroption.UserOption;
import org.wikipedia.useroption.database.UserOptionDao;
import org.wikipedia.useroption.database.UserOptionRow;
import org.wikipedia.useroption.dataclient.UserInfo;
import org.wikipedia.useroption.dataclient.UserOptionDataClient;
import org.wikipedia.useroption.dataclient.UserOptionDataClientSingleton;
import org.wikipedia.util.log.L;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class UserOptionSyncAdapter extends AbstractThreadedSyncAdapter {
    public UserOptionSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
                              ContentProviderClient provider, SyncResult syncResult) {
        if (!AccountUtil.supported(account)) {
            L.i("unexpected account=" + account);
            ++syncResult.stats.numAuthExceptions;
            return;
        }

        boolean uploadOnly = extras.getBoolean(ContentResolver.SYNC_EXTRAS_UPLOAD);

        upload();
        if (!uploadOnly) {
            download();
        }
    }

    private synchronized void download() {
        UserOptionDataClientSingleton.instance().get(new UserOptionDataClient.UserInfoCallback() {
            @Override
            public void success(@NonNull UserInfo userInfo) {
                Collection<UserOption> options = userInfo.userjsOptions();
                L.i("downloaded " + options.size() + " option(s)");
                UserOptionDao.instance().reconcileTransaction(options);
            }
        });
    }

    private synchronized void upload() {
        final List<UserOptionRow> rows = new ArrayList<>(UserOptionDao.instance().startTransaction());
        while (!rows.isEmpty()) {
            final UserOptionRow row = rows.remove(0);
            uploadSingle(row, new UserOptionDataClient.UserOptionPostCallback() {
                @Override public void success() {
                    UserOptionDao.instance().completeTransaction(row);
                }
                @Override public void failure(Throwable t) {
                    UserOptionDao.instance().failTransaction(rows);
                }
            });
        }
    }

    private void uploadSingle(@NonNull UserOptionRow row,
                              @NonNull UserOptionDataClient.UserOptionPostCallback callback) {
        if (row.status() == HttpStatus.DELETED) {
            L.i("deleting user option: " + row.key());
            UserOptionDataClientSingleton.instance().delete(row.key(), callback);
        } else if (!row.status().synced()) {
            L.i("uploading user option: " + row.key());
            //noinspection ConstantConditions
            UserOptionDataClientSingleton.instance().post(row.dat(), callback);
        }
    }
}
