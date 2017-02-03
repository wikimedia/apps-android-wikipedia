package org.wikipedia.login;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.util.ArraySet;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class User {
    @Nullable private static UserInfoStorage STORAGE = new UserInfoStorage();
    @Nullable private static User CURRENT_USER;

    public static void setUser(@NonNull User user) {
        CURRENT_USER = user;
        if (STORAGE != null) {
            STORAGE.setUser(user);
        }
    }

    @Nullable public static User getUser() {
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
    @VisibleForTesting public static void disableStorage() {
        STORAGE = null;
    }

    @NonNull private final String username;
    @NonNull private final String password;
    @NonNull private Map<String, Integer> ids;
    @NonNull private final Set<String> groups;

    public User(@NonNull String username, @NonNull String password) {
        this(username, password, null, null);
    }

    public User(@NonNull User other, @Nullable Map<String, Integer> ids, @Nullable Set<String> groups) {
        this(other.username, other.password, ids, groups);
    }

    public User(@NonNull String username, @NonNull String password, @Nullable Map<String, Integer> ids,
                @Nullable Set<String> groups) {
        this.username = username;
        this.password = password;

        if (ids != null) {
            this.ids = new HashMap<>(ids);
        } else {
            this.ids = Collections.emptyMap();
        }

        if (groups != null) {
            this.groups = Collections.unmodifiableSet(new ArraySet<>(groups));
        } else {
            this.groups = Collections.emptySet();
        }
    }

    @NonNull public String getUsername() {
        return username;
    }

    @NonNull public String getPassword() {
        return password;
    }

    public boolean hasIdForLang(@NonNull String code) {
        return ids.containsKey(code);
    }

    public void putIdForLanguage(@NonNull String code, int id) {
        ids.put(code, id);
    }

    public int getIdForLanguage(@NonNull String code) {
        Integer id = ids.get(code);
        return id == null ? 0 : id;
    }

    @NonNull Map<String, Integer> getIdMap() {
        return ids;
    }

    public boolean isAllowed(@NonNull Set<String> allowedGroups) {
        return !allowedGroups.isEmpty() && !Collections.disjoint(allowedGroups, groups);
    }

    @NonNull Set<String> getGroupMemberships() {
        return groups;
    }
}
