package org.wikipedia.feed.continuereading;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.feed.FeedCoordinator;
import org.wikipedia.feed.dataclient.FeedClient;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.util.log.L;

import java.util.Collections;

public class ContinueReadingClient implements FeedClient {
    @Nullable private LastPageReadTask lastPageReadTask;

    @Override
    public void request(@NonNull Context context, @NonNull WikiSite wiki, int age,
                        @NonNull final FeedClient.Callback cb) {
        cancel();
        lastPageReadTask = new LastPageReadTask(age) {
            @Override
            public void onFinish(@Nullable HistoryEntry entry) {
                FeedCoordinator.postCardsToCallback(cb, entry == null ? Collections.emptyList()
                        : Collections.singletonList(new ContinueReadingCard(entry)));
            }

            @Override
            public void onCatch(Throwable caught) {
                L.w("Error fetching last-read page", caught);
                cb.error(caught);
            }
        };
        lastPageReadTask.execute();
    }

    @Override
    public void cancel() {
        if (lastPageReadTask != null) {
            lastPageReadTask.cancel();
            lastPageReadTask = null;
        }
    }
}
