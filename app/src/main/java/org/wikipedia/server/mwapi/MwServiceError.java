package org.wikipedia.server.mwapi;

import android.support.annotation.Nullable;

import org.wikipedia.server.ServiceError;

/**
 * Gson POJO for a MediaWiki API error.
 */
public class MwServiceError implements ServiceError {
    @SuppressWarnings("unused") private String code;
    @SuppressWarnings("unused") private String info;
    @SuppressWarnings("unused") private String docref;

    @Override @Nullable public String getTitle() {
        return code;
    }

    @Override @Nullable public String getDetails() {
        return info;
    }

    public String getDocRef() {
        return docref;
    }

    public boolean badToken() {
        return "badtoken".equals(code);
    }

    @Override
    public String toString() {
        return "MwServiceError{"
                + "code='" + code + '\''
                + ", info='" + info + '\''
                + ", docref='" + docref + '\''
                + '}';
    }
}
