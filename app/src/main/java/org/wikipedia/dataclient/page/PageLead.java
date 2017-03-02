package org.wikipedia.dataclient.page;

import android.location.Location;
import android.support.annotation.Nullable;

import org.wikipedia.dataclient.ServiceError;
import org.wikipedia.page.Page;
import org.wikipedia.page.PageTitle;

/**
 * Gson POJI for loading the first stage of page content.
 */
public interface PageLead {
    boolean hasError();

    ServiceError getError();

    void logError(String message);

    /** Note: before using this check that #hasError is false */
    Page toPage(PageTitle title);

    String getLeadSectionContent();

    @Nullable String getTitlePronunciationUrl();

    @Nullable Location getGeo();
}
