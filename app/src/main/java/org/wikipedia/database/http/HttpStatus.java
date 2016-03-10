package org.wikipedia.database.http;

import android.support.annotation.NonNull;
import android.util.SparseArray;

public enum HttpStatus {
    /** Row exists and no synchronization is requested. */
    SYNCHRONIZED(0),

    /** Row exists remotely and should be updated locally. */
    OUTDATED(1),

    /** Row exists remotely and should be modified. */
    MODIFIED(2),

    /** Row does not exist remotely and should be added. */
    ADDED(3),

    /** Row exists remotely and should be deleted. */
    DELETED(4);

    private static final SparseArray<HttpStatus> CODE_TO_ENUM = codeToEnum();

    private final int code;

    @NonNull
    public static HttpStatus of(int code) {
        HttpStatus status = CODE_TO_ENUM.get(code);
        if (status == null) {
            throw new IllegalArgumentException("code=" + code);
        }
        return status;
    }

    public int code() {
        return code;
    }

    public boolean synced() {
        return this == SYNCHRONIZED;
    }

    HttpStatus(int code) {
        this.code = code;
    }

    private static SparseArray<HttpStatus> codeToEnum() {
        SparseArray<HttpStatus> ret = new SparseArray<>();
        for (HttpStatus value : values()) {
            ret.put(value.code(), value);
        }
        return ret;
    }
}