package org.wikipedia.wikidata;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.page.PageQueryTask;
import org.wikipedia.page.PageTitle;
import org.wikipedia.Site;

import java.util.List;

/**
 * Populates a list of PageTitles with Wikidata descriptions for each item.
 * This task doesn't "return" anything; it simply modifies the PageTitle objects in place.
 */
public class GetDescriptionsTask extends PageQueryTask<Void> {
    private List<PageTitle> titles;

    public GetDescriptionsTask(Api api, Site site, List<PageTitle> titles) {
        super(api, site, titles);
        this.titles = titles;
    }

    @Override
    public void buildQueryParams(RequestBuilder builder) {
        builder.param("prop", "pageterms")
                .param("wbptterm", "description");
    }

    @Override
    public Void processPage(int pageId, PageTitle pageTitle, JSONObject pageData) throws Throwable {
        JSONObject terms = pageData.optJSONObject("terms");
        if (terms != null) {
            final JSONArray array = terms.optJSONArray("description");
            if (array != null && array.length() > 0) {
                for (PageTitle title : titles) {
                    if (title.getPrefixedText().equals(pageTitle.getPrefixedText())
                            || title.getDisplayText().equals(pageTitle.getDisplayText())) {
                        title.setDescription(array.getString(0));
                        break;
                    }
                }
            }
        }
        return null;
    }
}
