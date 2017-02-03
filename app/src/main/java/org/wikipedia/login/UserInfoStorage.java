package org.wikipedia.login;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.settings.Prefs;

class UserInfoStorage {

    void setUser(@NonNull User user) {
        Prefs.setLoginUsername(user.getUsername());
        Prefs.setLoginPassword(user.getPassword());
        Prefs.setLoginUserIds(user.getIdMap());
        Prefs.setLoginGroups(user.getGroupMemberships());
    }

    @Nullable
    User getUser() {
        if (Prefs.hasLoginUsername() && Prefs.hasLoginPassword()) {
            //noinspection ConstantConditions
            return new User(
                    Prefs.getLoginUsername(),
                    Prefs.getLoginPassword(),
                    Prefs.getLoginUserIds(),
                    Prefs.getLoginGroups()
            );
        }
        return null;
    }

    void clearUser() {
        Prefs.removeLoginUsername();
        Prefs.removeLoginPassword();
        Prefs.removeLoginUserIds();
        Prefs.removeLoginGroups();
    }
}
