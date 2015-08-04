package org.wikipedia.login;

public class User {
    private final String username;
    private final String password;
    private final int userID;

    public User(String username, String password, int userID) {
        this.username = username;
        this.password = password;
        this.userID = userID;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public int getUserID() {
        return userID;
    }
}
