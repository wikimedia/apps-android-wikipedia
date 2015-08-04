package org.wikipedia.login;

import org.wikipedia.settings.Prefs;

public class UserInfoStorage {
    private User currentUser;

    public boolean isLoggedIn() {
        return getUser() != null;
    }

    public void setUser(User user) {
        Prefs.setLoginUsername(user.getUsername());
        Prefs.setLoginPassword(user.getPassword());
        Prefs.setLoginUserId(user.getUserID());
    }

    public User getUser() {
        if (currentUser == null) {
            if (Prefs.hasLoginUsername() && Prefs.hasLoginPassword()) {
                currentUser = new User(
                        Prefs.getLoginUsername(),
                        Prefs.getLoginPassword(),
                        Prefs.getLoginUserId()
                );
            }
        }

        return currentUser;
    }

    public void clearUser() {
        Prefs.removeLoginUsername();
        Prefs.removeLoginPassword();
        Prefs.removeLoginUserId();
        currentUser = null;
    }

}
