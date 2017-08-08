package org.wikipedia.feed.mainpage;

import android.support.annotation.NonNull;

import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.feed.dataclient.DummyClient;
import org.wikipedia.feed.model.Card;
import org.wikipedia.offline.OfflineManager;
import org.wikipedia.page.PageTitle;
import org.wikipedia.staticdata.MainPageNameData;
import org.wikipedia.util.DeviceUtil;
import org.wikipedia.util.log.L;

import java.io.IOException;

public class MainPageClient extends DummyClient {
    @Override public Card getNewCard(WikiSite wiki) {
        return new MainPageCard();
    }

    @NonNull
    public static PageTitle getMainPageTitle() {
        WikipediaApp app = WikipediaApp.getInstance();
        PageTitle title = new PageTitle(MainPageNameData
                .valueFor(app.getAppOrSystemLanguageCode()), app.getWikiSite());
        if (OfflineManager.hasCompilation() && !DeviceUtil.isOnline()) {
            try {
                title = new PageTitle(OfflineManager.instance().getMainPageTitle(), app.getWikiSite());
            } catch (IOException e) {
                L.e(e);
            }
        }
        return title;
    }
}
