package org.wikipedia.feed.random;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import org.wikipedia.R;
import org.wikipedia.feed.view.FeedViewCallback;
import org.wikipedia.feed.view.StaticCardView;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.PageTitle;
import org.wikipedia.random.RandomSummaryService;
import org.wikipedia.util.log.L;

public class RandomCardView extends StaticCardView<RandomCard> {
    public RandomCardView(@NonNull Context context) {
        super(context);
    }

    @Override public void setCard(@NonNull RandomCard card) {
        super.setCard(card);
        setTitle(getString(R.string.view_random_card_title));
        setSubtitle(getString(R.string.view_random_card_subtitle));
        setIcon(R.drawable.icon_feed_random);
    }

    @Override public void setCallback(@Nullable FeedViewCallback callback) {
        super.setCallback(callback);
        setOnClickListener(new CallbackAdapter(callback));
    }

    private class CallbackAdapter implements OnClickListener {
        @Nullable private final FeedViewCallback callback;

        CallbackAdapter(@Nullable FeedViewCallback callback) {
            this.callback = callback;
        }

        @Override
        public void onClick(View view) {
            if (callback != null && getCard() != null) {
                new RandomSummaryService(getCard().site(), serviceCallback).get();
            }
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