package org.wikipedia.beta.migration;

import android.content.Context;
import org.json.JSONObject;
import org.wikipedia.beta.PageTitle;
import org.wikipedia.beta.Site;
import org.wikipedia.beta.WikipediaApp;
import org.wikipedia.beta.analytics.SavedPagesFunnel;
import org.wikipedia.beta.savedpages.SavedPage;
import org.wikipedia.beta.savedpages.SavedPagePersister;

import java.util.List;

public class ArticleImporter {
    private final WikipediaApp app;

    public ArticleImporter(Context context) {
        app = (WikipediaApp) context.getApplicationContext();
    }

    public void importArticles(List<JSONObject> articles) {
        SavedPagePersister persister = (SavedPagePersister) app.getPersister(SavedPage.class);

        for (JSONObject item : articles) {
            PageTitle title = titleForItem(item);
            SavedPage savedPage = new SavedPage(title);
            SavedPagesFunnel funnel = app.getFunnelManager().getSavedPagesFunnel(title.getSite());
            funnel.logImport();
            persister.upsert(savedPage);
        }
    }

    private PageTitle titleForItem(JSONObject item) {
        Site site = new Site(item.optString("lang") + ".wikipedia.org");
        return new PageTitle(null, item.optString("title"), site);
    }
}
