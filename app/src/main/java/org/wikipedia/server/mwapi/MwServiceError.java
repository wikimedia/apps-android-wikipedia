package org.wikipedia.server.mwapi;

import org.wikipedia.server.ServiceError;

/**
 * Gson POJO for a MediaWiki API error.
 */
public class MwServiceError implements ServiceError {
    private String code;
    private String info;
    private String docref;

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
