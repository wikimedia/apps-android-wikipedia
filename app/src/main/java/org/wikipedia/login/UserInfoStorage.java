package org.wikipedia.login;

import org.wikipedia.settings.Prefs;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

class UserInfoStorage {

    void setUser(@NonNull User user) {
        Prefs.setLoginUsername(user.getUsername());
        Prefs.setLoginPassword(user.getPassword());
        Prefs.setLoginUserId(user.getUserID());
        Prefs.setLoginGroups(user.getGroupMemberships());
    }

    @Nullable
    User getUser() {
        if (Prefs.hasLoginUsername() && Prefs.hasLoginPassword()) {
            //noinspection ConstantConditions
            return new User(
                    Prefs.getLoginUsername(),
                    Prefs.getLoginPassword(),
                    Prefs.getLoginUserId(),
                    Prefs.getLoginGroups()
            );
        }
        return null;
    }

    void clearUser() {
        Prefs.removeLoginUsername();
        Prefs.removeLoginPassword();
        Prefs.removeLoginUserId();
        Prefs.removeLoginGroups();
    }
}
