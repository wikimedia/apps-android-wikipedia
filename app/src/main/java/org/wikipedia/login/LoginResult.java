package org.wikipedia.login;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.dataclient.WikiSite;

import java.util.Collections;
import java.util.Set;

public class LoginResult {
    @NonNull private final WikiSite site;
    @NonNull private final String status;
    @Nullable private final String userName;
    @Nullable private final String password;
    @Nullable private final String message;

    private int userId;
    @NonNull private Set<String> groups = Collections.emptySet();

    LoginResult(@NonNull WikiSite site, @NonNull String status, @Nullable String userName,
                @Nullable String password, @Nullable String message) {
        this.site = site;
        this.status = status;
        this.userName = userName;
        this.password = password;
        this.message = message;
    }

    @NonNull public WikiSite getSite() {
        return site;
    }

    @NonNull public String getStatus() {
        return status;
    }

    public boolean pass() {
        return "PASS".equals(status);
    }

    public boolean fail() {
        return "FAIL".equals(status);
    }

    @Nullable public String getUserName() {
        return userName;
    }

    @Nullable public String getPassword() {
        return password;
    }

    @Nullable public String getMessage() {
        return message;
    }

    public void setUserId(int id) {
        this.userId = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setGroups(@NonNull Set<String> groups) {
        this.groups = groups;
    }

    @NonNull public Set<String> getGroups() {
        return groups;
    }
}
