package org.wikipedia.page.fetch;

import org.json.JSONArray;
import org.json.JSONException;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiException;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.page.PageTitle;
import org.wikipedia.page.Section;

import java.util.ArrayList;
import java.util.List;

/** TODO: Using nodejs/RESTBase endpoints */
public class SectionsFetcherRB implements Fetcher<List<Section>> {
    private final PageTitle title;
    private final String sectionsRequested;
    private final boolean downloadImages;

    public SectionsFetcherRB(PageTitle title, String sectionsRequested, boolean downloadImages) {
        this.downloadImages = downloadImages;
        this.title = title;
        this.sectionsRequested = sectionsRequested;
    }

    @Override
    public RequestBuilder buildRequest(Api api) {
        RequestBuilder builder = api.action("mobileview")
                .param("page", title.getPrefixedText())
                .param("prop", "text|sections|languagecount")
                .param("onlyrequestedsections", "1") // Stupid mediawiki & stupid backwardscompat
                .param("sections", sectionsRequested)
                .param("sectionprop", "toclevel|line|anchor")
                .param("noheadings", "true");
        if (!downloadImages) {
            builder.param("noimages", "true");
        }
        return builder;
    }

    @Override
    public List<Section> processResult(ApiResult result) throws Throwable {
        JSONArray sectionsJSON = result.asObject().optJSONObject("mobileview").optJSONArray("sections");
        ArrayList<Section> sections = new ArrayList<>();

        if (sectionsJSON == null) {
            throw new ApiException(new JSONException("FIXME: server returned 0 sections with no error."));
        }
        for (int i = 0; i < sectionsJSON.length(); i++) {
            Section newSection = new Section(sectionsJSON.getJSONObject(i));
            sections.add(newSection);
        }

        return sections;
    }

    public String getPagePropsResponseName() {
        return "mobileview";
    }
}
