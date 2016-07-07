package org.wikipedia.feed.random;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import org.wikipedia.R;
import org.wikipedia.feed.FeedViewCallback;
import org.wikipedia.feed.view.StaticCardView;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.PageTitle;
import org.wikipedia.random.RandomSummaryService;
import org.wikipedia.util.log.L;

public class RandomCardView extends StaticCardView {
    @Nullable private FeedViewCallback callback;

    public RandomCardView(@NonNull Context context) {
        super(context);
    }

    public void set(@NonNull final RandomCard card) {
        setTitle(getString(R.string.view_random_card_title));
        setSubtitle(getString(R.string.view_random_card_subtitle));
        setIcon(R.drawable.icon_feed_random);
        setOnClickListener(new CallbackAdapter(card, callback));
    }

    @NonNull public RandomCardView setCallback(@Nullable FeedViewCallback callback) {
        this.callback = callback;
        return this;
    }

    private static class CallbackAdapter implements OnClickListener {
        @NonNull private final RandomCard card;
        @Nullable private final FeedViewCallback callback;

        CallbackAdapter(@NonNull final RandomCard card, @Nullable FeedViewCallback callback) {
            this.card = card;
            this.callback = callback;
        }

        @Override
        public void onClick(View view) {
            new RandomSummaryService(card.site(), serviceCallback).get();
        }

        private RandomSummaryService.RandomSummaryCallback serviceCallback
                = new RandomSummaryService.RandomSummaryCallback() {
            @Override
            public void onSuccess(PageTitle title) {
                if (callback != null) {
                    callback.onSelectPage(new HistoryEntry(title, HistoryEntry.SOURCE_FEED_RANDOM));
                }
            }

            @Override
            public void onError(Throwable t) {
                L.w("Failed to get random card", t);
            }
        };
    }
}
