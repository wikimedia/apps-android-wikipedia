package org.wikipedia.auth;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.login.User;
import org.wikipedia.useroption.sync.UserOptionContentResolver;
import org.wikipedia.util.ApiUtil;
import org.wikipedia.util.log.L;

public final class AccountUtil {
    public static void createAccountForLoggedInUser() {
        User user = app().getUserInfoStorage().getUser();
        if (user != null && account() == null) {
            createAccount(null, user.getUsername(), user.getPassword());
        }
    }

    public static void createAccount(@Nullable AccountAuthenticatorResponse response,
                                     String username, String password) {

        Account account = new Account(username, accountType());
        boolean created = accountManager().addAccountExplicitly(account, password, null);

        L.i("account creation " + (created ? "successful" : "failure"));

        if (created) {
            if (response != null) {
                Bundle bundle = new Bundle();
                bundle.putString(AccountManager.KEY_ACCOUNT_NAME, username);
                bundle.putString(AccountManager.KEY_ACCOUNT_TYPE, accountType());

                response.onResult(bundle);
            }

            UserOptionContentResolver.requestManualSync();
        } else {
            if (response != null) {
                response.onError(AccountManager.ERROR_CODE_REMOTE_EXCEPTION, "");
            }
            L.d("account creation failure");
        }
    }

    public static void logOutIfAccountRemoved() {
        User user = app().getUserInfoStorage().getUser();
        if (user != null && account() == null) {
            app().logOut();
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    public static void removeAccount() {
        Account account = account();
        if (account != null) {
            if (ApiUtil.hasLollipopMr1()) {
                accountManager().removeAccountExplicitly(account);
            } else {
                //noinspection deprecation
                accountManager().removeAccount(account, null, null);
            }
        }
    }

    public static boolean supported(@NonNull Account account) {
        return account.equals(AccountUtil.account());
    }

    @Nullable
    public static Account account() {
        User user = app().getUserInfoStorage().getUser();
        return user == null ? null : account(user.getUsername());
    }

    @Nullable
    private static Account account(@NonNull String username) {
        Account[] accounts = accountManager().getAccountsByType(accountType());

        for (Account account : accounts) {
            if (username.equalsIgnoreCase(account.name)) {
                return account;
            }
        }

        return null;
    }

    @NonNull
    public static String accountType() {
        return app().getString(R.string.account_type);
    }

    private static AccountManager accountManager() {
        return AccountManager.get(app());
    }

    @NonNull
    private static WikipediaApp app() {
        return WikipediaApp.getInstance();
    }

    private AccountUtil() { }
}
