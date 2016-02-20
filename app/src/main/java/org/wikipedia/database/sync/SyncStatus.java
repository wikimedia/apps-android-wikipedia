package org.wikipedia.database.sync;

import android.util.SparseArray;

public enum SyncStatus {
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

    private static final SparseArray<SyncStatus> CODE_TO_ENUM = codeToEnum();

    private final int code;

    public static SyncStatus of(int code) {
        return CODE_TO_ENUM.get(code);
    }

    public int code() {
        return code;
    }

    public boolean synced() {
        return this == SYNCHRONIZED;
    }

    SyncStatus(int code) {
        this.code = code;
    }

    private static SparseArray<SyncStatus> codeToEnum() {
        SparseArray<SyncStatus> ret = new SparseArray<>();
        for (SyncStatus value : values()) {
            ret.put(value.code(), value);
        }
        return ret;
    }
}