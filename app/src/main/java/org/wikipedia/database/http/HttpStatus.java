package org.wikipedia.database.http;

import android.support.annotation.NonNull;

import org.wikipedia.model.CodeEnum;
import org.wikipedia.model.EnumCode;
import org.wikipedia.model.EnumCodeMap;

public enum HttpStatus implements EnumCode {
    /** Row exists and no synchronization is requested. */
    SYNCHRONIZED(0),

    /** Row exists remotely and should be updated locally. When complete, status is
     * {@link #SYNCHRONIZED}. */
    OUTDATED(1),

    /** Row exists remotely and should be modified. When complete, status is
     * {@link #SYNCHRONIZED}. */
    MODIFIED(2),

    /** Row does not exist remotely and should be added. When complete, status is
     * {@link #SYNCHRONIZED}. */
    ADDED(3),

    /** Row exists remotely and should be deleted. When complete, row is removed. */
    DELETED(4);

    public static final CodeEnum<HttpStatus> CODE_ENUM = new CodeEnum<HttpStatus>() {
        @NonNull @Override public HttpStatus enumeration(int code) {
            return of(code);
        }
    };

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
