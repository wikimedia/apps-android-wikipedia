package org.wikipedia.feed;

import android.content.Context;
import android.support.annotation.NonNull;

import org.wikipedia.Site;
import org.wikipedia.feed.model.Card;

import java.util.List;

public interface FeedClient {
    void request(@NonNull Context context, @NonNull Site site, int age, @NonNull final Callback cb);
    void cancel();

    interface Callback {
        void success(@NonNull List<? extends Card> cards);
        void error(@NonNull Throwable caught);
    }
}
