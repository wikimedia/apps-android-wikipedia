package org.wikipedia.login;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class LoginResult {
    @NonNull private final String status;
    @Nullable private final User user;
    @Nullable private final String message;

    public LoginResult(@NonNull String status, @Nullable User user, @Nullable String message) {
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
