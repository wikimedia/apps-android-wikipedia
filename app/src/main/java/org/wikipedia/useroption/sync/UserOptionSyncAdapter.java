package org.wikipedia.useroption.sync;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;

import org.wikipedia.auth.AccountUtil;
import org.wikipedia.database.http.HttpStatus;
import org.wikipedia.useroption.UserOption;
import org.wikipedia.useroption.database.UserOptionDao;
import org.wikipedia.useroption.database.UserOptionRow;
import org.wikipedia.useroption.dataclient.UserInfo;
import org.wikipedia.useroption.dataclient.UserOptionDataClientSingleton;
import org.wikipedia.util.log.L;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import retrofit.RetrofitError;

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
        } catch (RetrofitError e) {
            L.d(e);
            ++syncResult.stats.numIoExceptions;
        }
    }

    private void download() {
        UserInfo info = UserOptionDataClientSingleton.instance().get();
        Collection<UserOption> options = info.userjsOptions();
        L.i("downloaded " + options.size() + " option(s)");
        UserOptionDao.instance().reconcileOptions(options);
    }

    private void upload() {
        List<UserOptionRow> options = new ArrayList<>(UserOptionDao.instance().startTransaction());
        L.i("uploading " + options.size() + " option(s)");
        while (!options.isEmpty()) {
            UserOptionRow option = options.get(0);

            try {
                if (option.status() == HttpStatus.DELETED) {
                    UserOptionDataClientSingleton.instance().delete(option);
                } else {
                    UserOptionDataClientSingleton.instance().post(option);
                }
            } catch (RetrofitError e) {
                UserOptionDao.instance().failTransaction(options);
                throw e;
            }

            UserOptionDao.instance().completeTransaction(option);
            options.remove(0);
        }
    }
}