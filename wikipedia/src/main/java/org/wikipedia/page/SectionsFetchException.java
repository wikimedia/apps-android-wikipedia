package org.wikipedia.page;

public class SectionsFetchException extends Exception {
    private final String code;
    private final String info;

    public SectionsFetchException(String code, String info) {
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
