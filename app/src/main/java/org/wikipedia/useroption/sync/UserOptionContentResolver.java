package org.wikipedia.useroption.sync;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;

import org.wikipedia.BuildConfig;
import org.wikipedia.auth.AccountUtil;
import org.wikipedia.database.contract.UserOptionContract;
import org.wikipedia.util.log.L;

public final class UserOptionContentResolver {
    public static void requestManualUpload() {
        requestManualSync(true);
    }

    public static void requestManualSync() {
        requestManualSync(false);
    }

    public static void requestManualSync(boolean uploadOnly) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_UPLOAD, uploadOnly);

        requestSync(bundle);
    }

    public static void registerAppSyncObserver(@NonNull Context context) {
        Uri uri = UserOptionContract.Option.URI;
        UserOptionContentObserver observer = new UserOptionContentObserver();
        context.getContentResolver().registerContentObserver(uri, true, observer);
    }

    private static void requestSync(@NonNull Bundle bundle) {
        Account account = AccountUtil.account();
        if (account == null) {
            L.i("no account");
            return;
        }

        ContentResolver.requestSync(account, BuildConfig.USER_OPTION_AUTHORITY, bundle);
    }

    private UserOptionContentResolver() { }

    private static class UserOptionContentObserver extends ContentObserver {
        UserOptionContentObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);

            // persist changed options here
        }
    }
}
