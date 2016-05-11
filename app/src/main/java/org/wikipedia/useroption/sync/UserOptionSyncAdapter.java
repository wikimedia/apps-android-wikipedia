package org.wikipedia.useroption.sync;

import org.wikipedia.auth.AccountUtil;
import org.wikipedia.database.http.HttpStatus;
import org.wikipedia.useroption.UserOption;
import org.wikipedia.useroption.database.UserOptionDao;
import org.wikipedia.useroption.database.UserOptionRow;
import org.wikipedia.useroption.dataclient.UserInfo;
import org.wikipedia.useroption.dataclient.UserOptionDataClientSingleton;
import org.wikipedia.util.log.L;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;

import java.io.IOException;
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

        try {
            upload();
            if (!uploadOnly) {
                download();
            }
        } catch (IOException e) {
            L.d(e);
            ++syncResult.stats.numIoExceptions;
        }
    }

    private void download() throws IOException {
        UserInfo info = UserOptionDataClientSingleton.instance().get();
        Collection<UserOption> options = info.userjsOptions();
        L.i("downloaded " + options.size() + " option(s)");
        UserOptionDao.instance().reconcileTransaction(options);
    }

    private void upload() throws IOException {
        List<UserOptionRow> rows = new ArrayList<>(UserOptionDao.instance().startTransaction());
        L.i("uploading " + rows.size() + " option(s)");
        while (!rows.isEmpty()) {
            UserOptionRow row = rows.get(0);

            try {
                if (row.status() == HttpStatus.DELETED) {
                    UserOptionDataClientSingleton.instance().delete(row.key());
                } else {
                    //noinspection ConstantConditions
                    UserOptionDataClientSingleton.instance().post(row.dat());
                }
            } catch (IOException e) {
                UserOptionDao.instance().failTransaction(rows);
                throw e;
            }

            UserOptionDao.instance().completeTransaction(row);
            rows.remove(0);
        }
    }
}