package org.wikipedia.feed.onboarding;

import android.content.Context;
import android.support.annotation.NonNull;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.feed.dataclient.FeedClient;
import org.wikipedia.feed.model.Card;

import java.util.ArrayList;
import java.util.List;

public class OnboardingClient implements FeedClient {
    @Override public void request(@NonNull Context context, @NonNull WikiSite wiki, int age,
                                  @NonNull FeedClient.Callback cb) {
        List<Card> cards = new ArrayList<>();
        // TODO: add onboarding cards conditionally
        cb.success(cards);
    }

    @Override public void cancel() { }
}
