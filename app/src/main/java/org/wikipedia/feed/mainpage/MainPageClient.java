package org.wikipedia.feed.mainpage;

import android.content.Context;

import androidx.annotation.NonNull;

import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.feed.FeedContentType;
import org.wikipedia.feed.FeedCoordinator;
import org.wikipedia.feed.dataclient.FeedClient;
import org.wikipedia.feed.model.Card;

import java.util.ArrayList;
import java.util.List;

public class MainPageClient implements FeedClient {
    @Override public void request(@NonNull Context context, @NonNull WikiSite wiki, int age,
                                  @NonNull FeedClient.Callback cb) {
        List<Card> cards = new ArrayList<>();
        for (String appLangCode : WikipediaApp.getInstance().language().getAppLanguageCodes()) {
            if (!FeedContentType.MAIN_PAGE.getLangCodesDisabled().contains(appLangCode)) {
                cards.add(new MainPageCard(WikiSite.forLanguageCode(appLangCode)));
            }
        }
        FeedCoordinator.postCardsToCallback(cb, cards);
    }

    @Override public void cancel() { }
}
