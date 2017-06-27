package org.wikipedia.readinglist.page.database.disk;

import android.support.annotation.NonNull;

import org.wikipedia.model.CodeEnum;
import org.wikipedia.model.EnumCode;
import org.wikipedia.model.EnumCodeMap;

public enum DiskStatus implements EnumCode {
    /** Only cache when explicitly requested but do not pre-cache or save to disk. No transaction
     * pending. */
    ONLINE(0),

    /** Saved to disk. No transaction pending. */
    SAVED(1),

    /** Download or re-download pending. When complete, status is {@link #SAVED}. */
    OUTDATED(2),

    /** Possibly downloaded previously and delete from disk pending. When complete, status is
     * {@link #ONLINE}. */
    UNSAVED(3),

    /** Possibly downloaded previously and delete from disk pending. When complete, row is
     * removed. */
    DELETED(4);

    public static final CodeEnum<DiskStatus> CODE_ENUM = new CodeEnum<DiskStatus>() {
        @NonNull @Override public DiskStatus enumeration(int code) {
            return of(code);
        }
    };

    private static final EnumCodeMap<DiskStatus> MAP = new EnumCodeMap<>(DiskStatus.class);

    private final int code;

    public static DiskStatus of(int code) {
        return MAP.get(code);
    }

    public boolean savedOrSaving() {
        return this == SAVED || this == OUTDATED;
    }

    public boolean saving() {
        return this == OUTDATED;
    }

    @Override public int code() {
        return code;
    }

    DiskStatus(int code) {
        this.code = code;
    }
}
