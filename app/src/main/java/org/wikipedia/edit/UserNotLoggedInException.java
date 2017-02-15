package org.wikipedia.edit;

// https://www.mediawiki.org/wiki/API:Assert
public class UserNotLoggedInException extends RuntimeException {
    public UserNotLoggedInException() {
        super("User not logged in.");
    }
}
