package org.wikipedia.page.fetch;

import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.page.Section;

import java.util.List;

/**
 * Retrieve the first section, general page metadata, and whatever we need to show below the fold.
 */
public interface LeadSectionFetcher {
    RequestBuilder buildRequest(Api api, int leadImageWidth);

    List<Section> processResult(ApiResult result) throws Throwable;

    /** @return the JSONObject key for a child JSONObject which contains the metadata for
     * PageProperties */
    String getPagePropsResponseName();
}
