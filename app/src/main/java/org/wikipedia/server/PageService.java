package org.wikipedia.server;

import java.io.IOException;

/**
 * Generic interface for Page content service.
 * Usually we would use direct Retrofit Callbacks here but since we have two ways of
 * getting to the data (MW API and RESTBase) we add this layer of indirection -- until we drop one.
 */
public interface PageService {
    /**
     * Gets a page summary for a given title -- for link previews
     *
     * @param title the page title to be used including prefix
     * @param cb a Retrofit callback which provides the populated PageSummary object in #success
     */
    void pageSummary(String title, PageSummary.Callback cb);

    /**
     * Gets the lead section and initial metadata of a given title.
     *
     * @param title the page title with prefix if necessary
     * @param leadImageThumbWidth one of the bucket widths for the lead image
     * @param noImages add the noimages flag to the request if true
     * @param cb a Retrofit callback which provides the populated PageLead object in #success
     */
    void pageLead(String title, int leadImageThumbWidth, boolean noImages, PageLead.Callback cb);

    /**
     * Gets the remaining sections of a given title.
     *
     * @param title the page title to be used including prefix
     * @param noImages add the noimages flag to the request if true
     * @param cb a Retrofit callback which provides the populated PageRemaining object in #success
     */
    void pageRemaining(String title, boolean noImages, PageRemaining.Callback cb);

    /**
     * Gets all page content of a given title.  Used in the saved page sync background service.
     * Synchronous call.
     *
     * @param title the page title to be used including prefix
     * @param noImages add the noimages flag to the request if true
     * @throws IOException when the request did not succeed
     */
    PageCombo pageCombo(String title, boolean noImages) throws IOException;
}
