package org.wikipedia.wikidata;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.PageQueryTask;
import org.wikipedia.PageTitle;
import org.wikipedia.Site;
import org.wikipedia.Utils;

import java.util.List;

/**
 * Retrieves Wikidata descriptions via a Wikipedia site.
 */
public class GetDescriptionsTask extends PageQueryTask<String> {

    public GetDescriptionsTask(Api api, Site site, List<PageTitle> titles) {
        super(LOW_CONCURRENCY, api, site, titles);
    }

    @Override
    public void buildQueryParams(RequestBuilder builder) {
        builder.param("prop", "pageterms")
                .param("wbptterm", "description");
    }

    @Override
    public String processPage(int pageId, PageTitle pageTitle, JSONObject pageData) throws Throwable {
        JSONObject terms = pageData.optJSONObject("terms");
        if (terms != null) {
            final JSONArray array = terms.optJSONArray("description");
            if (array != null && array.length() > 0) {
                return Utils.capitalizeFirstChar(array.getString(0));
            }
        }
        return null;
    }
}
