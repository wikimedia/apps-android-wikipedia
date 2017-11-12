package org.wikipedia.dataclient.page;

import android.support.annotation.Nullable;

import org.wikipedia.dataclient.ServiceError;

/**
 * Represents a summary of a page, useful for page previews.
 */
public interface PageSummary {
    boolean hasError();
    @Nullable ServiceError getError();
    @Nullable String getTitle();
    @Nullable String getDisplayTitle();
    @Nullable String getExtract();
    @Nullable String getThumbnailUrl();
}
