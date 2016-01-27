package org.wikipedia.server.restbase;

import org.wikipedia.server.ServiceError;

/**
 * Gson POJO for a RESTBase API error.
 */
public class RbServiceError implements ServiceError {
    private String type;
    private String title;
    private String detail;
    private String method;
    private String uri;

    public String getTitle() {
        return title;
    }

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
