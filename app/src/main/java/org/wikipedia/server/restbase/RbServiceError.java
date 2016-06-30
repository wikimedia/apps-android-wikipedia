package org.wikipedia.server.restbase;

import org.wikipedia.server.ServiceError;

/**
 * Gson POJO for a RESTBase API error.
 */
public class RbServiceError implements ServiceError {
    @SuppressWarnings("unused") private String type;
    @SuppressWarnings("unused") private String title;
    @SuppressWarnings("unused") private String detail;
    @SuppressWarnings("unused") private String method;
    @SuppressWarnings("unused") private String uri;

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getDetails() {
        return detail;
    }

    @Override
    public String toString() {
        return "RbServiceError{"
                + "title='" + title + '\''
                + ", detail='" + detail + '\''
                + ", method='" + method + '\''
                + ", type='" + type + '\''
                + ", uri='" + uri + '\''
                + '}';
    }
}
