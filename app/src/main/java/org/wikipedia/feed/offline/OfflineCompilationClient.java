package org.wikipedia.feed.offline;

import android.content.Context;
import android.support.annotation.NonNull;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.feed.dataclient.FeedClient;
import org.wikipedia.feed.model.Card;

import java.util.Collections;

public class OfflineCompilationClient implements FeedClient {
    @Override
    public void request(@NonNull Context context, @NonNull WikiSite wiki, int age,
                        @NonNull final FeedClient.Callback cb) {
        cb.success(Collections.singletonList((Card) new OfflineCompilationCard()));
    }

    @Override
    public void cancel() {
    }
}
