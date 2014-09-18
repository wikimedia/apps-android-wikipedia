package org.wikipedia.beta.login;

import android.content.SharedPreferences;

public class UserInfoStorage {
    private static final String PREFERENCE_USERNAME = "username";
    private static final String PREFERENCE_PASSWORD = "password";
    private static final String PREFERENCE_USERID = "userID";

    private final SharedPreferences prefs;
    private User currentUser;

    public UserInfoStorage(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    public boolean isLoggedIn() {
        return getUser() != null;
    }

    public void setUser(User user) {
        prefs.edit()
                .putString(PREFERENCE_USERNAME, user.getUsername())
                .putString(PREFERENCE_PASSWORD, user.getPassword())
                .putInt(PREFERENCE_USERID, user.getUserID())
                .commit();
    }

    public User getUser() {
        if (currentUser == null) {
            if (prefs.contains(PREFERENCE_USERNAME) && prefs.contains(PREFERENCE_PASSWORD)) {
                currentUser = new User(
                        prefs.getString(PREFERENCE_USERNAME, null),
                        prefs.getString(PREFERENCE_PASSWORD, null),
                        prefs.getInt(PREFERENCE_USERID, 0)
                );
            }
        }

        return currentUser;
    }

    public void clearUser() {
        prefs.edit()
                .remove(PREFERENCE_USERNAME)
                .remove(PREFERENCE_PASSWORD)
                .remove(PREFERENCE_USERID)
                .commit();
        currentUser = null;
    }

}
