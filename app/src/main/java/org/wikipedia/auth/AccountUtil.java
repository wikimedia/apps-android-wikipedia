package org.wikipedia.auth;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.gson.reflect.TypeToken;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.json.GsonMarshaller;
import org.wikipedia.json.GsonUnmarshaller;
import org.wikipedia.login.LoginResult;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.log.L;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class AccountUtil {

    public static void updateAccount(@Nullable AccountAuthenticatorResponse response,
                                             LoginResult result) {
        if (createAccount(result.getUserName(), result.getPassword())) {
            if (response != null) {
                Bundle bundle = new Bundle();
                bundle.putString(AccountManager.KEY_ACCOUNT_NAME, result.getUserName());
                bundle.putString(AccountManager.KEY_ACCOUNT_TYPE, accountType());
                response.onResult(bundle);
            }
        } else {
            if (response != null) {
                response.onError(AccountManager.ERROR_CODE_REMOTE_EXCEPTION, "");
            }
            L.d("account creation failure");
            return;
        }

        setPassword(result.getPassword());
        putUserIdForLanguage(result.getSite().languageCode(), result.getUserId());
        setGroups(result.getGroups());
    }

    public static boolean isLoggedIn() {
        return account() != null;
    }

    @Nullable public static String getUserName() {
        Account account = account();
        return account == null ? null : account.name;
    }

    @Nullable public static String getPassword() {
        Account account = account();
        return account == null ? null : accountManager().getPassword(account);
    }

    public static int getUserIdForLanguage(@NonNull String code) {
        Map<String, Integer> map = getUserIds();
        Integer id = map.get(code);
        return id == null ? 0 : id;
    }

    public static void putUserIdForLanguage(@NonNull String code, int id) {
        Map<String, Integer> ids = new HashMap<>();
        ids.putAll(getUserIds());
        ids.put(code, id);
        setUserIds(ids);
    }

    @NonNull public static Set<String> getGroups() {
        Account account = account();
        if (account == null) {
            return Collections.emptySet();
        }
        String setStr = accountManager().getUserData(account, app().getString(R.string.preference_key_login_groups));
        if (TextUtils.isEmpty(setStr)) {
            return Collections.emptySet();
        }
        return GsonUnmarshaller.unmarshal(new TypeToken<HashSet<String>>(){}, setStr);
    }

    public static void setGroups(@NonNull Set<String> groups) {
        Account account = account();
        if (account == null) {
            return;
        }
        accountManager().setUserData(account,
                app().getString(R.string.preference_key_login_groups),
                GsonMarshaller.marshal(groups));
    }

    public static boolean isMemberOf(@NonNull Set<String> groups) {
        return !groups.isEmpty() && !Collections.disjoint(groups, getGroups());
    }

    public static void removeAccount() {
        Account account = account();
        if (account != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
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
        try {
            Account[] accounts = accountManager().getAccountsByType(accountType());
            if (accounts.length > 0) {
                return accounts[0];
            }
        } catch (SecurityException e) {
            L.logRemoteErrorIfProd(e);
        }
        return null;
    }

    public static void migrateAccountFromSharedPrefs() {
        if (Prefs.hasLoginUsername() || Prefs.hasLoginPassword()) {
            if (!TextUtils.isEmpty(Prefs.getLoginUsername())
                    && !TextUtils.isEmpty(Prefs.getLoginPassword())) {
                createAccount(Prefs.getLoginUsername(), Prefs.getLoginPassword());
            }
            setUserIds(Prefs.getLoginUserIds());
            if (Prefs.getLoginGroups() != null) {
                setGroups(Prefs.getLoginGroups());
            }

            Prefs.removeLoginUsername();
            Prefs.removeLoginPassword();
            Prefs.removeLoginUserIds();
            Prefs.removeLoginGroups();
        }
    }

    @NonNull
    public static String accountType() {
        return app().getString(R.string.account_type);
    }

    private static boolean createAccount(@NonNull String userName, @NonNull String password) {
        Account account = account();
        if (account == null || TextUtils.isEmpty(account.name) || !account.name.equals(userName)) {
            removeAccount();
            account = new Account(userName, accountType());
            return accountManager().addAccountExplicitly(account, password, null);
        }
        return true;
    }

    private static void setPassword(@NonNull String password) {
        Account account = account();
        if (account != null) {
            accountManager().setPassword(account, password);
        }
    }

    @NonNull private static Map<String, Integer> getUserIds() {
        Account account = account();
        if (account == null) {
            return Collections.emptyMap();
        }
        String mapStr = accountManager().getUserData(account, app().getString(R.string.preference_key_login_user_id_map));
        if (TextUtils.isEmpty(mapStr)) {
            return Collections.emptyMap();
        }
        return GsonUnmarshaller.unmarshal(new TypeToken<HashMap<String, Integer>>(){}, mapStr);
    }

    private static void setUserIds(@NonNull Map<String, Integer> ids) {
        Account account = account();
        if (account == null) {
            return;
        }
        accountManager().setUserData(account,
                app().getString(R.string.preference_key_login_user_id_map),
                GsonMarshaller.marshal(ids));
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
