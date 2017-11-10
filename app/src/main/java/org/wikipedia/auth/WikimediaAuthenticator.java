package org.wikipedia.auth;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.BuildConfig;
import org.wikipedia.R;
import org.wikipedia.analytics.LoginFunnel;
import org.wikipedia.login.LoginActivity;

public class WikimediaAuthenticator extends AbstractAccountAuthenticator {
    private static final String[] SYNC_AUTHORITIES = {BuildConfig.READING_LISTS_AUTHORITY};

    @NonNull private final Context context;

    public WikimediaAuthenticator(@NonNull Context context) {
        super(context);
        this.context = context;
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        return unsupportedOperation();
    }

    @Override
    public Bundle addAccount(@NonNull AccountAuthenticatorResponse response,
                             @NonNull String accountType, @Nullable String authTokenType,
                             @Nullable String[] requiredFeatures, @Nullable Bundle options)
            throws NetworkErrorException {

        if (!supportedAccountType(accountType) || AccountUtil.account() != null) {
            return unsupportedOperation();
        }

        return addAccount(response);
    }

    @Override
    public Bundle confirmCredentials(@NonNull AccountAuthenticatorResponse response,
                                     @NonNull Account account, @Nullable Bundle options)
            throws NetworkErrorException {
        return unsupportedOperation();
    }

    @Override
    public Bundle getAuthToken(@NonNull AccountAuthenticatorResponse response,
                               @NonNull Account account, @NonNull String authTokenType,
                               @Nullable Bundle options)
            throws NetworkErrorException {
        return unsupportedOperation();
    }

    @Nullable
    @Override
    public String getAuthTokenLabel(@NonNull String authTokenType) {
        return supportedAccountType(authTokenType) ? context.getString(R.string.wikimedia) : null;
    }

    @Nullable
    @Override
    public Bundle updateCredentials(@NonNull AccountAuthenticatorResponse response,
                                    @NonNull Account account, @Nullable String authTokenType,
                                    @Nullable Bundle options)
            throws NetworkErrorException {
        return unsupportedOperation();
    }

    @Nullable
    @Override
    public Bundle hasFeatures(@NonNull AccountAuthenticatorResponse response,
                              @NonNull Account account, @NonNull String[] features)
            throws NetworkErrorException {
        Bundle bundle = new Bundle();
        bundle.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
        return bundle;
    }

    private boolean supportedAccountType(@Nullable String type) {
        return AccountUtil.accountType().equals(type);
    }

    private Bundle addAccount(AccountAuthenticatorResponse response) {
        Intent intent = LoginActivity.newIntent(context, LoginFunnel.SOURCE_SYSTEM);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);

        Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);

        return bundle;
    }

    private Bundle unsupportedOperation() {
        Bundle bundle = new Bundle();
        bundle.putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_UNSUPPORTED_OPERATION);

        // HACK: the docs indicate that this is a required key bit it's not displayed to the user.
        bundle.putString(AccountManager.KEY_ERROR_MESSAGE, "");

        return bundle;
    }

    @Override
    public Bundle getAccountRemovalAllowed(AccountAuthenticatorResponse response,
                                           Account account) throws NetworkErrorException {
        Bundle result = super.getAccountRemovalAllowed(response, account);

        if (result.containsKey(AccountManager.KEY_BOOLEAN_RESULT)
                && !result.containsKey(AccountManager.KEY_INTENT)) {
            boolean allowed = result.getBoolean(AccountManager.KEY_BOOLEAN_RESULT);

            if (allowed) {
                for (String auth : SYNC_AUTHORITIES) {
                    ContentResolver.cancelSync(account, auth);
                }
            }
        }

        return result;
    }
}
