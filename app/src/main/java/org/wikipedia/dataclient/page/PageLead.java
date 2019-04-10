package org.wikipedia.dataclient.page;

import android.location.Location;

import org.wikipedia.page.Page;
import org.wikipedia.page.PageTitle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Gson POJI for loading the first stage of page content.
 */
public interface PageLead {
    /** Note: before using this check that #hasError is false */
    Page toPage(PageTitle title);

    @NonNull String getLeadSectionContent();

    @Nullable String getTitlePronunciationUrl();
    @Nullable String getLeadImageUrl(int leadImageWidth);
    @Nullable String getThumbUrl();
    @Nullable String getDescription();

    @Nullable Location getGeo();
}
