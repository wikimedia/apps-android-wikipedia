package org.wikipedia.login;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.dataclient.WikiSite;

class LoginOAuthResult extends LoginResult {

    LoginOAuthResult(@NonNull WikiSite site, @NonNull String status, @Nullable String userName,
                     @Nullable String password, @Nullable String message) {
        super(site, status, userName, password, message);
    }
}
