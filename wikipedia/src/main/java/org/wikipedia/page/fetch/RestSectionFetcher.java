package org.wikipedia.page.fetch;

import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.page.Section;

import java.util.List;

/**
 * Retrieve the remaining page content and metadata not already covered by the
 * {@link LeadSectionFetcher}.
 */
public interface RestSectionFetcher {
    RequestBuilder buildRequest(Api api);
    List<Section> processResult(ApiResult result) throws Throwable;
}
