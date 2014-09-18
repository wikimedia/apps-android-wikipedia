package org.wikipedia.beta.editing;

public class EditingException extends Exception {
    private final String code;
    private final String info;

    public EditingException(String code, String info) {
        this.code = code;
        this.info = info;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return info;
    }
}
