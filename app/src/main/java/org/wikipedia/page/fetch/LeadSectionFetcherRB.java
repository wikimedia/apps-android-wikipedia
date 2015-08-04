package org.wikipedia.page.fetch;

import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.page.Page;
import org.wikipedia.page.PageTitle;

/**
 * @see SectionsFetcherRB
 */
public class LeadSectionFetcherRB extends SectionsFetcherRB implements LeadSectionFetcher {
    public LeadSectionFetcherRB(PageTitle title, String sectionsRequested, boolean downloadImages) {
        super(title, sectionsRequested, downloadImages);
    }

    @Override
    public RequestBuilder buildRequest(Api api, int leadImageWidth) {
        RequestBuilder builder = super.buildRequest(api);
        builder.param("prop", builder.getParams().get("prop")
                + "|thumb|image|id|revision|description|"
                + Page.API_REQUEST_PROPS);
        builder.param("thumbsize", Integer.toString(leadImageWidth));
        return builder;
    }
}
