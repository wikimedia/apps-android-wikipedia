package org.wikipedia.login;

public class LoginResult {
    private final String code;
    private final User user;

    public LoginResult(String code, User user) {
        this.code = code;
        this.user = user;
    }

    public String getCode() {
        return code;
    }

    public User getUser() {
        return user;
    }
}
