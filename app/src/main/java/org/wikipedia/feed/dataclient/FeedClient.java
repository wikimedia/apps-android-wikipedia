package org.wikipedia.feed.dataclient;

import android.content.Context;
import android.support.annotation.NonNull;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.feed.model.Card;

import java.util.List;

public interface FeedClient {
    void request(@NonNull Context context, @NonNull WikiSite wiki, int age, @NonNull final Callback cb);
    void cancel();

    interface Callback {
        void success(@NonNull List<? extends Card> cards);
        void error(@NonNull Throwable caught);
    }
}
