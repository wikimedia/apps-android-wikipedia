package org.wikipedia.feed.onboarding;

import android.content.Context;
import android.support.annotation.NonNull;

import org.wikipedia.R;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.feed.announcement.Announcement;
import org.wikipedia.feed.dataclient.FeedClient;
import org.wikipedia.feed.model.Card;
import org.wikipedia.util.UriUtil;

import java.util.ArrayList;
import java.util.List;

public class OnboardingClient implements FeedClient {
    @Override public void request(@NonNull Context context, @NonNull WikiSite wiki, int age,
                                  @NonNull FeedClient.Callback cb) {
        List<Card> cards = new ArrayList<>();

        OnboardingCard card = new OfflineOnboardingCard(new Announcement(
                "offlineOnboardingCard1",
                context.getString(R.string.offline_library_onboarding_text),
                "https://upload.wikimedia.org/wikipedia/commons/5/5b/Illustration-OfflineLibraryPromo2_3x.png",
                new Announcement.Action(context.getString(R.string.offline_library_onboarding_action),
                        UriUtil.LOCAL_URL_OFFLINE_LIBRARY)));
        if (card.shouldShow()) {
            cards.add(card);
        }

        cb.success(cards);
    }

    @Override public void cancel() { }
}
