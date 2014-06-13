package org.wikipedia.migration;

import android.content.Context;
import org.json.JSONObject;
import org.wikipedia.PageTitle;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;
import org.wikipedia.savedpages.SavedPage;
import org.wikipedia.savedpages.SavedPagePersister;

import java.util.List;

public class ArticleImporter {
    private final WikipediaApp app;

    public ArticleImporter(Context context) {
        app = (WikipediaApp) context.getApplicationContext();
    }

    public void importArticles(List<JSONObject> articles) {
        //
        SavedPagePersister persister = (SavedPagePersister) app.getPersister(SavedPage.class);

        for (JSONObject item : articles) {
            PageTitle title = titleForItem(item);
            SavedPage savedPage = new SavedPage(title);
            persister.upsert(savedPage);
        }
    }

    private PageTitle titleForItem(JSONObject item) {
        Site site = new Site(item.optString("lang") + ".wikipedia.org");
        return new PageTitle(null, item.optString("title"), site);
    }
}
