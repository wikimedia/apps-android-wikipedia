package org.wikipedia.server.restbase;

import org.wikipedia.page.Page;
import org.wikipedia.page.PageProperties;
import org.wikipedia.page.PageTitle;
import org.wikipedia.server.PageCombo;
import org.wikipedia.util.log.L;

import com.google.gson.annotations.Expose;

import android.support.annotation.Nullable;


/**
 * Combines RbPageLead and RbPageRemaining Gson POJOs for RESTBase Nodejs API.
 * When using the Mobile Content Service API this class composes the two parts, lead and
 * remaining.
 */
public class RbPageCombo implements PageCombo {
    @Expose @Nullable private RbServiceError error;
    @Expose @Nullable private RbPageLead lead;
    @Expose @Nullable private RbPageRemaining remaining;


    @Override
    public boolean hasError() {
        return error != null;
    }

    @Nullable
    public RbServiceError getError() {
        return error;
    }

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
        Page page = new Page(lead.adjustPageTitle(title), lead.getSections(), toPageProperties());
        if (remaining != null) {
            page.augmentRemainingSections(remaining.getSections());
        }
        return page;
    }

    @Override
    public String getLeadSectionContent() {
        return lead != null ? lead.getLeadSectionContent() : "";
    }

    /** Converter */
    public PageProperties toPageProperties() {
        return new PageProperties(lead);
    }
}
