package org.wikipedia.page;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.ApiTask;
import org.wikipedia.PageTitle;
import org.wikipedia.WikipediaApp;
import org.wikipedia.concurrency.ExecutorService;

import java.util.ArrayList;
import java.util.List;

public class SectionsFetchTask extends ApiTask<List<Section>> {
    private final PageTitle title;
    private final String sections;

    public SectionsFetchTask(Context context, PageTitle title, String sections) {
        super(
                ExecutorService.getSingleton().getExecutor(SectionsFetchTask.class, 1),
                ((WikipediaApp)context.getApplicationContext()).getAPIForSite(title.getSite())
        );
        this.title = title;
        this.sections = sections;
    }

    @Override
    public RequestBuilder buildRequest(Api api) {
        return api.action("mobileview")
                .param("page", title.getPrefixedText())
                .param("prop", "text|sections")
                .param("onlyrequestedsections", "1") // Stupid mediawiki & stupid backwardscompat
                .param("sections", sections)
                .param("sectionprop", "toclevel|line|anchor")
                .param("noheadings", "true");
    }

    @Override
    public List<Section> processResult(ApiResult result) throws Throwable {
        JSONArray sectionsJSON = result.asObject().optJSONObject("mobileview").optJSONArray("sections");
        ArrayList<Section> sections = new ArrayList<Section>();

        for (int i=0; i < sectionsJSON.length(); i++) {
            Section newSection = new Section(sectionsJSON.getJSONObject(i));
            sections.add(newSection);
        }
        return sections;
    }
}
