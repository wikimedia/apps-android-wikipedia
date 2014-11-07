package org.wikipedia.wikidata;

import org.wikipedia.PageQueryTask;
import org.wikipedia.PageTitle;
import org.wikipedia.Site;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.RequestBuilder;
import org.json.JSONObject;
import java.util.List;

/**
 * Retrieves Wikidata IDs from Wikipedia: PageTitles -> Wikidata IDs.
 */
public class WikidataIdsTask extends PageQueryTask<String> {

    public WikidataIdsTask(Api api, Site site, List<PageTitle> titles) {
        super(LOW_CONCURRENCY, api, site, titles);
    }

    @Override
    public void buildQueryParams(RequestBuilder builder) {
        builder.param("prop", "pageprops")
               .param("ppprop", "wikibase_item");
    }

    @Override
    public String processPage(int pageId, PageTitle pageTitle, JSONObject pageData) throws Throwable {
        JSONObject pageProps = pageData.optJSONObject("pageprops");
        if (pageProps == null) {
            return null;
        } else {
            return pageProps.optString("wikibase_item");
        }
    }
}
