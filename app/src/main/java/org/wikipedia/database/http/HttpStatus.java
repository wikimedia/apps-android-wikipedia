package org.wikipedia.database.http;

import android.support.annotation.NonNull;

import org.wikipedia.model.EnumCode;
import org.wikipedia.model.EnumCodeMap;

public enum HttpStatus implements EnumCode {
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

    private static final EnumCodeMap<HttpStatus> MAP = new EnumCodeMap<>(HttpStatus.class);

    private final int code;

    @NonNull
    public static HttpStatus of(int code) {
        return MAP.get(code);
    }

    @Override
    public int code() {
        return code;
    }

    public boolean synced() {
        return this == SYNCHRONIZED;
    }

    HttpStatus(int code) {
        this.code = code;
    }
}