package org.wikipedia.login;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.settings.Prefs;

class UserInfoStorage {

    void setUser(@NonNull User user) {
        Prefs.setLoginUsername(user.getUsername());
        Prefs.setLoginPassword(user.getPassword());
        Prefs.setLoginUserId(user.getUserID());
        Prefs.setLoginUserIdLang(user.getUserIDLang());
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
                    Prefs.getLoginUserIdLang(),
                    Prefs.getLoginGroups()
            );
        }
        return null;
    }

    void clearUser() {
        Prefs.removeLoginUsername();
        Prefs.removeLoginPassword();
        Prefs.removeLoginUserId();
        Prefs.removeLoginUserIdLang();
        Prefs.removeLoginGroups();
    }
}
