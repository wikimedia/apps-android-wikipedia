package org.wikipedia.server.restbase;

import org.wikipedia.server.ServiceError;

import com.google.gson.annotations.Expose;

/**
 * Gson POJO for a RESTBase API error.
 */
public class RbServiceError implements ServiceError {
    @Expose private String type;
    @Expose private String title;
    @Expose private String detail;
    @Expose private String method;
    @Expose private String uri;

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
