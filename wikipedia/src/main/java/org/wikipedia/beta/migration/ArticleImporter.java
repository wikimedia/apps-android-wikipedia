package org.wikipedia.beta.migration;

import android.content.Context;
import org.json.JSONObject;
import org.wikipedia.beta.PageTitle;
import org.wikipedia.beta.Site;
import org.wikipedia.beta.WikipediaApp;
import org.wikipedia.beta.bookmarks.Bookmark;
import org.wikipedia.beta.bookmarks.BookmarkPersister;

import java.util.List;

public class ArticleImporter {
    private final WikipediaApp app;

    public ArticleImporter(Context context) {
        app = (WikipediaApp) context.getApplicationContext();
    }

    public void importArticles(List<JSONObject> articles) {
        //
        BookmarkPersister persister = (BookmarkPersister) app.getPersister(Bookmark.class);

        for (JSONObject item : articles) {
            PageTitle title = titleForItem(item);
            Bookmark bookmark = new Bookmark(title);
            persister.upsert(bookmark);
        }
    }

    private PageTitle titleForItem(JSONObject item) {
        Site site = new Site(item.optString("lang") + ".wikipedia.org");
        return new PageTitle(null, item.optString("title"), site);
    }
}
