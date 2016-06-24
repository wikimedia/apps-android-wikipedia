package org.wikipedia.feed;

import android.content.Context;
import android.support.annotation.NonNull;

import org.wikipedia.Site;
import org.wikipedia.feed.model.Card;

import java.util.Collections;

/* A dummy client for providing static cards (main page, random) to the FeedCoordinator. */
public abstract class DummyClient<T extends Card> implements FeedClient {

    @Override
    public void request(@NonNull Context context, @NonNull Site site, int age,
                        @NonNull final FeedClient.Callback cb) {
        try {
            cb.success(Collections.singletonList(getNewCard(site)));
        } catch (Throwable t) {
            cb.error(t);
        }
    }

    @Override
    public void cancel() {
    }

    public abstract T getNewCard(Site site);
}
