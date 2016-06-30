package org.wikipedia.server.mwapi;

import org.wikipedia.server.PageSummary;
import org.wikipedia.util.log.L;

import android.support.annotation.Nullable;

/**
 * Useful for link previews coming from MW API.
 */
public class MwPageSummary implements PageSummary {
    @SuppressWarnings("unused") private MwServiceError error;
    @SuppressWarnings("unused") @Nullable private MwQuery query;

    @Override
    public boolean hasError() {
        // if there is no page set something went terribly wrong
        return error != null || getFirstPage() == null;
    }

    @Override
    @Nullable
    public MwServiceError getError() {
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
        return getFirstPage() == null ? null : getFirstPage().title;
    }

    @Override @Nullable
    public String getExtract() {
        return getFirstPage() == null ? null : getFirstPage().extract;
    }

    @Override @Nullable
    public String getThumbnailUrl() {
        return getFirstPage() == null ? null : getFirstPage().getThumbnailUrl();
    }

    private MwPage getFirstPage() {
        return (query != null && query.pages != null) ? query.pages[0] : null;
    }

    private static class MwQuery {
        @SuppressWarnings("unused,MismatchedReadAndWriteOfArray")
        @Nullable private MwPage[] pages;
    }

    private static class MwPage {
        @SuppressWarnings("unused") @Nullable private String title;
        @SuppressWarnings("unused") @Nullable private String extract;
        @SuppressWarnings("unused") @Nullable private Thumb thumbnail;

        @Nullable
        public String getThumbnailUrl() {
            return thumbnail == null ? null : thumbnail.getUrl();
        }
    }

    /**
     * For the thumbnail URL of the page
     */
    public static class Thumb {
        @SuppressWarnings("unused") private String source;

        public String getUrl() {
            return source;
        }
    }
}
