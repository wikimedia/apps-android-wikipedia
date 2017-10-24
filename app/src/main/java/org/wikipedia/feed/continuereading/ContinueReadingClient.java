package org.wikipedia.feed.continuereading;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.feed.dataclient.FeedClient;
import org.wikipedia.feed.model.Card;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.util.log.L;

import java.io.IOException;
import java.util.Collections;

public class ContinueReadingClient implements FeedClient {
    private static final int MIN_DAYS_OLD = 1;
    private static final int MAX_DAYS_OLD = 30;

    @Nullable private LastPageReadTask lastPageReadTask;

    @Override
    public void request(@NonNull Context context, @NonNull WikiSite wiki, int age,
                        @NonNull final FeedClient.Callback cb) {
        cancel();
        lastPageReadTask = new LastPageReadTask(context, age, MIN_DAYS_OLD, MAX_DAYS_OLD) {
            @Override
            public void onFinish(@Nullable HistoryEntry entry) {
                if (entry == null) {
                    cb.error(new IOException("Error fetching last-read page"));
                    return;
                }
                cb.success(Collections.singletonList((Card) new ContinueReadingCard(entry)));
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
