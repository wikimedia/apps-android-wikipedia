package org.wikipedia.login;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

public class User {
    @Nullable private static UserInfoStorage STORAGE = new UserInfoStorage();
    @Nullable private static User CURRENT_USER;

    public static void setUser(@NonNull User user) {
        CURRENT_USER = user;
        if (STORAGE != null) {
            STORAGE.setUser(user);
        }
    }

    @Nullable
    public static User getUser() {
        if (CURRENT_USER == null && STORAGE != null) {
            CURRENT_USER = STORAGE.getUser();
        }
        return CURRENT_USER;
    }

    public static boolean isLoggedIn() {
        return CURRENT_USER != null;
    }

    public static void clearUser() {
        CURRENT_USER = null;
        if (STORAGE != null) {
            STORAGE.clearUser();
        }
    }

    /**
     * Call this before instrumentation tests run
     */
    @VisibleForTesting
    public static void disableStorage() {
        STORAGE = null;
    }

    @NonNull private final String username;
    @NonNull private final String password;
    private final int userID;

    public User(@NonNull String username, @NonNull String password, int userID) {
        this.username = username;
        this.password = password;
        this.userID = userID;
    }

    @NonNull
    public String getUsername() {
        return username;
    }

    @NonNull
    public String getPassword() {
        return password;
    }

    public int getUserID() {
        return userID;
    }
}
