package org.wikimedia.wikipedia;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiResult;
import org.wikimedia.wikipedia.concurrency.ExecutorService;

import java.util.ArrayList;
import java.util.List;

public class SectionsFetchTask extends ApiTask<List<Section>> {
    private final PageTitle title;
    private final String sections;

    public SectionsFetchTask(Api api, PageTitle title, String sections) {
        super(ExecutorService.getSingleton().getExecutor(SectionsFetchTask.class, 1), api);
        this.title = title;
        this.sections = sections;
    }

    @Override
    public ApiResult buildRequest(Api api) {
        return api.action("mobileview")
                .param("page", title.getPrefixedText())
                .param("prop", "text|sections")
                .param("onlyrequestedsections", "1") // Stupid mediawiki & stupid backwardscompat
                .param("sections", sections)
                .param("sectionprop", "toclevel|line|anchor")
                .param("noheadings", "true")
                .get();
    }

    @Override
    public List<Section> processResult(ApiResult result) throws Throwable {
        JSONArray sectionsJSON = result.asObject().optJSONObject("mobileview").optJSONArray("sections");
        ArrayList<Section> sections = new ArrayList<Section>();
        Section curSection = null;

        for (int i=0; i < sectionsJSON.length(); i++) {
            JSONObject sectionJSON = sectionsJSON.getJSONObject(i);
            Section newSection = new Section(sectionJSON.optInt("id"),
                    sectionJSON.optInt("toclevel", 1), // Assume all sections are 1st level if they don't have that info
                    sectionJSON.optString("line", null),
                    sectionJSON.optString("anchor", null),
                    sectionJSON.optString("text")
            );
            if (curSection == null || newSection.getLevel() <= curSection.getLevel())  {
                curSection = newSection;
                sections.add(newSection);
            } else {
                curSection.insertSection(newSection);
            }
        }
        return sections;
    }
}
