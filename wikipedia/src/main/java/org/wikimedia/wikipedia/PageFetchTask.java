package org.wikimedia.wikipedia;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiResult;
import org.wikimedia.wikipedia.concurrency.ExecutorService;
import org.wikimedia.wikipedia.concurrency.SaneAsyncTask;

import java.util.ArrayList;

public class PageFetchTask extends SaneAsyncTask<Page> {
    PageTitle title;
    Api api;

    public PageFetchTask(Api api, PageTitle title) {
        super(ExecutorService.getSingleton().getExecutor(PageFetchTask.class));
        this.title = title;
        this.api = api;
    }

    @Override
    public Page performTask() throws Throwable {
        ApiResult result = api.action("mobileview")
                .param("page", title.getTitle()) //TODO: Support non main NS!
                .param("prop", "text|sections")
                .param("sections", "0")
                .param("sectionprop", "toclevel|line|anchor")
                .param("noheadings", "true")
                .get();
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

        return new Page(title, sections);
    }
}
