package org.wikipedia.page;

import android.content.*;
import org.json.*;
import org.mediawiki.api.json.*;
import org.wikipedia.*;

import java.util.*;

public class SectionsFetchTask extends ApiTask<List<Section>> {
    private final PageTitle title;
    private final String sections;
    private final WikipediaApp app;

    public SectionsFetchTask(Context context, PageTitle title, String sections) {
        super(
                1,
                ((WikipediaApp)context.getApplicationContext()).getAPIForSite(title.getSite())
        );
        this.title = title;
        this.sections = sections;
        this.app = (WikipediaApp)context.getApplicationContext();
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
