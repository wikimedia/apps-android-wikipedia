package org.wikipedia.feed.searchbar;

import android.content.Context;
import android.support.annotation.NonNull;

import org.wikipedia.Site;
import org.wikipedia.feed.FeedClient;
import org.wikipedia.feed.model.Card;

import java.util.Collections;

public class SearchClient implements FeedClient {
    @Override
    public void request(@NonNull Context context, @NonNull Site site, int age,
                        @NonNull final FeedClient.Callback cb) {
        cb.success(Collections.singletonList((Card) new SearchCard()));
    }

    @Override
    public void cancel() {
    }
}