package org.wikipedia.server.mwapi;

import org.wikipedia.server.ServiceError;

import com.google.gson.annotations.Expose;

/**
 * Gson POJO for a MediaWiki API error.
 */
public class MwServiceError implements ServiceError {
    @Expose private String code;
    @Expose private String info;
    @Expose private String docref;

    public String getTitle() {
        return code;
    }

    public String getDetails() {
        return info;
    }

    public String getDocRef() {
        return docref;
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
