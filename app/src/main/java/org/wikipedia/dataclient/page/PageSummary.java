package org.wikipedia.dataclient.page;

import android.support.annotation.Nullable;

import org.wikipedia.dataclient.ServiceError;

/**
 * Represents a summary of a page, useful for page previews.
 */
public interface PageSummary {
    boolean hasError();

    ServiceError getError();

    void logError(String message);

    @Nullable String getTitle();

    @Nullable String getExtract();

    @Nullable String getThumbnailUrl();

    /** So we can have polymorphic Retrofit Callbacks */
    interface Callback {
        void success(PageSummary pageSummary);

        void failure(Throwable throwable);
    }
}
