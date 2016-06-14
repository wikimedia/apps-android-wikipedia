package org.wikipedia.feed.demo;

import android.content.Context;
import android.support.annotation.NonNull;

import org.wikipedia.Site;
import org.wikipedia.feed.FeedClient;
import org.wikipedia.feed.model.Card;

import java.util.Collections;

public class IntegerListClient implements FeedClient {
    @Override
    public void request(@NonNull Context context, @NonNull Site site, int age,
                        @NonNull final FeedClient.Callback cb) {
        cb.success(Collections.singletonList((Card) new IntegerListCard()));
    }

    @Override
    public void cancel() {
    }
}