package org.wikipedia.dataclient.page;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wikipedia.page.Namespace;

/**
 * Represents a summary of a page, useful for page previews.
 */
public interface PageSummary {
    String TYPE_STANDARD = "standard";
    String TYPE_DISAMBIGUATION = "disambiguation";
    String TYPE_MAIN_PAGE = "mainpage";
    String TYPE_NO_EXTRACT = "no-extract";

    @NonNull String getType();
    @Nullable String getTitle();
    @Nullable String getDisplayTitle();
    @Nullable String getConvertedTitle();
    @Nullable String getExtract();
    @Nullable String getExtractHtml();
    @Nullable String getThumbnailUrl();
    @Nullable String getRevision();
    @NonNull Namespace getNamespace();
    int getPageId();
}
