package org.wikipedia.feed.mainpage;

import android.support.annotation.NonNull;

import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.feed.dataclient.DummyClient;
import org.wikipedia.feed.model.Card;
import org.wikipedia.page.PageTitle;
import org.wikipedia.settings.SiteInfoClient;

public class MainPageClient extends DummyClient {
    @Override public Card getNewCard(WikiSite wiki) {
        return new MainPageCard();
    }

    @NonNull
    public static PageTitle getMainPageTitle() {
        WikipediaApp app = WikipediaApp.getInstance();
        return new PageTitle(SiteInfoClient.getMainPageForLang(app.getAppOrSystemLanguageCode()), app.getWikiSite());
    }
}
