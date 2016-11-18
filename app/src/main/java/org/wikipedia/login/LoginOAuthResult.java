package org.wikipedia.login;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

class LoginOAuthResult extends LoginResult {

    LoginOAuthResult(@NonNull String status, @Nullable String message) {
        super(status, null, message);
    }
}
