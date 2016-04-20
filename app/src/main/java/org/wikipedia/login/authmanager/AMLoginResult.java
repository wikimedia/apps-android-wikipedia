package org.wikipedia.login.authmanager;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.login.User;

public class AMLoginResult {
    @NonNull private final String status;
    @Nullable private final User user;
    @Nullable private final String message;

    public AMLoginResult(@NonNull String status, @Nullable User user, @Nullable String message) {
        this.status = status;
        this.user = user;
        this.message = message;
    }

    @NonNull
    public String getStatus() {
        return status;
    }

    public boolean pass() {
        return "PASS".equals(status);
    }

    public boolean fail() {
        return "FAIL".equals(status);
    }

    @Nullable
    public User getUser() {
        return user;
    }

    @Nullable
    public String getMessage() {
        return message;
    }
}
