package org.wikipedia.beta.nearby;

public class NearbyFetchException extends Exception {
    private final String code;
    private final String info;

    public NearbyFetchException(String code, String info) {
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
