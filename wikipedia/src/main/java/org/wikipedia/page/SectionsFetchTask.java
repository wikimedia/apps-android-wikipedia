package org.wikipedia.page;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiException;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.ApiTask;
import org.wikipedia.PageTitle;
import org.wikipedia.Utils;
import org.wikipedia.WikipediaApp;

import java.util.ArrayList;
import java.util.List;

public class SectionsFetchTask extends ApiTask<List<Section>> {
    private final PageTitle title;
    private final String sectionsRequested;
    private final WikipediaApp app;

    public SectionsFetchTask(Context context, PageTitle title, String sectionsRequested) {
        super(
                SINGLE_THREAD,
                ((WikipediaApp)context.getApplicationContext()).getAPIForSite(title.getSite())
        );
        this.title = title;
        this.sectionsRequested = sectionsRequested;
        this.app = (WikipediaApp)context.getApplicationContext();
    }

    @Override
    public RequestBuilder buildRequest(Api api) {
        return api.action("mobileview")
                .param("page", title.getPrefixedText())
                .param("prop", "text|sections")
                .param("onlyrequestedsections", "1") // Stupid mediawiki & stupid backwardscompat
                .param("sections", sectionsRequested)
                .param("sectionprop", "toclevel|line|anchor")
                .param("noheadings", "true");
    }

    @Override
    public List<Section> processResult(ApiResult result) throws Throwable {
        if (result.asObject().has("error")) {
            JSONObject errorJSON = result.asObject().optJSONObject("error");
            throw new SectionsFetchException(errorJSON.optString("code"), errorJSON.optString("info"));
        }

        JSONArray sectionsJSON = result.asObject().optJSONObject("mobileview").optJSONArray("sections");
        ArrayList<Section> sections = new ArrayList<Section>();

        if (sectionsJSON == null) {
            throw new ApiException(new JSONException("FIXME: server returned 0 sections with no error."));
        }
        for (int i = 0; i < sectionsJSON.length(); i++) {
            Section newSection = new Section(sectionsJSON.getJSONObject(i));
            sections.add(newSection);
        }
        if (WikipediaApp.isWikipediaZeroDevmodeOn()) {
            Utils.processHeadersForZero(app, result);
        }

        return sections;
    }
}
