package org.wikipedia.server.restbase;

import android.support.annotation.Nullable;

import org.wikipedia.server.PageSummary;
import org.wikipedia.util.log.L;

/**
 * Useful for link previews coming from RESTBase.
 */
public class RbPageSummary implements PageSummary {
    @SuppressWarnings("unused") private RbServiceError error;
    @SuppressWarnings("unused") @Nullable private String title;
    @SuppressWarnings("unused") @Nullable private String extract;
    @SuppressWarnings("unused") @Nullable private String description;
    @SuppressWarnings("unused") @Nullable private Thumbnail thumbnail;

    @Override
    public boolean hasError() {
        // if there is no title or no extract set something went terribly wrong
        return error != null || title == null || extract == null;
    }

    @Override
    @Nullable
    public RbServiceError getError() {
        return error;
    }

    @Override
    public void logError(String message) {
        if (error != null) {
            message += ": " + error.toString();
        }
        L.e(message);
    }

    @Override @Nullable
    public String getTitle() {
        return title;
    }

    @Override @Nullable
    public String getExtract() {
        return extract;
    }

    @Override @Nullable
    public String getThumbnailUrl() {
        return thumbnail == null ? null : thumbnail.getUrl();
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    /**
     * For the thumbnail URL of the page
     */
    public static class Thumbnail {
        @SuppressWarnings("unused") private String source;

        public String getUrl() {
            return source;
        }
    }
}
