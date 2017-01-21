package org.wikipedia.dataclient.restbase.page;

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.page.PageCombo;
import org.wikipedia.dataclient.restbase.RbServiceError;
import org.wikipedia.page.Page;
import org.wikipedia.page.PageProperties;
import org.wikipedia.page.PageTitle;
import org.wikipedia.util.log.L;


/**
 * Combines RbPageLead and RbPageRemaining Gson POJOs for RESTBase Nodejs API.
 * When using the Mobile Content Service API this class composes the two parts, lead and
 * remaining.
 */
public class RbPageCombo implements PageCombo {
    @SuppressWarnings("unused") @Nullable private RbServiceError error;
    @SuppressWarnings("unused") @Nullable private RbPageLead lead;
    @SuppressWarnings("unused") @Nullable private RbPageRemaining remaining;

    @Override
    public boolean hasError() {
        return error != null;
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

    /**
     * Note: before using this check that #hasError is false
     */
    @Override
    public Page toPage(PageTitle title) {
        if (lead == null) {
            throw new RuntimeException("lead is null. Check for errors before use!");
        }
        Page page = new Page(lead.adjustPageTitle(title), lead.getSections(),
                toPageProperties(title.getWikiSite()));
        if (remaining != null) {
            page.augmentRemainingSections(remaining.getSections());
        }
        return page;
    }

    @Override
    public String getLeadSectionContent() {
        return lead != null ? lead.getLeadSectionContent() : "";
    }

    @Override
    @Nullable
    public String getTitlePronunciationUrl() {
        return lead == null ? null : lead.getTitlePronunciationUrl();
    }

    @Nullable
    @Override
    public Location getGeo() {
        return lead == null ? null : lead.getGeo();
    }

    /** Converter */
    public PageProperties toPageProperties(@NonNull WikiSite wiki) {
        return new PageProperties(wiki, lead);
    }
}
